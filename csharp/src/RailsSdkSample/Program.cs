using System.Text.Json.Nodes;
using DotNetEnv;
using Rails;
using Rails.Core;
using RailsSdkSample;

Env.TraversePath().Load();

var cfg = SampleConfig.Load();
SampleErrors.LogTlsMode(cfg.InsecureProxyTls);

using var proxyHttp = ProxyHttp.Create(cfg.InsecureProxyTls);
using var railsClient = new RailsClient(
    new ClientOptions
    {
        BaseUrl = cfg.BaseUrl,
        ApiKey = cfg.ApiKey,
    }
);

var openApiJson = LoadOpenApiSpecJson();

var builder = WebApplication.CreateBuilder(args);
builder.WebHost.UseUrls($"http://+:{cfg.Port}");
builder.Services.AddCors(o =>
    o.AddDefaultPolicy(p => p.AllowAnyOrigin().AllowAnyMethod().AllowAnyHeader())
);

var app = builder.Build();

app.Use(
    async (ctx, next) =>
    {
        try
        {
            await next();
        }
        catch (Exception ex)
        {
            if (ctx.Response.HasStarted)
                throw;
            Console.Error.WriteLine(ex);
            var path = ctx.Request.Path.Value ?? "";
            var body = SampleErrors.ToErrorBody(ex, path);
            var status = body.Status is >= 400 and < 600 ? body.Status : 500;
            ctx.Response.StatusCode = status;
            await ctx.Response.WriteAsJsonAsync(body, ctx.RequestAborted);
        }
    }
);

app.UseCors();

app.MapGet(
    "/health",
    () => Results.Json(new Dictionary<string, string> { ["status"] = "ok" })
);

app.MapGet("/openapi.json", () => Results.Text(openApiJson, "application/json"));

SampleRoutes.Map(
    app,
    new SampleRouteDeps(cfg.BaseUrl, cfg.ApiKey, railsClient, proxyHttp, cfg.RailsXEnvironment)
);

app.UseSwaggerUI(c =>
{
    c.RoutePrefix = string.Empty;
    c.SwaggerEndpoint("/openapi.json", "Rails C# SDK sample");
});

Console.Error.WriteLine(
    $"[rails-sdk-sample] listening on http://localhost:{cfg.Port} — Swagger UI at /, OpenAPI at /openapi.json"
);

app.Run();

static string LoadOpenApiSpecJson()
{
    var path = Path.Combine(AppContext.BaseDirectory, "openapi-source.json");
    if (!File.Exists(path))
        throw new InvalidOperationException(
            $"OpenAPI file not found at {path}. Ensure mvp/sdk-samples/kotlin is present and the project copies openapi-source.json."
        );
    var raw = File.ReadAllText(path);
    var node = JsonNode.Parse(raw)?.AsObject()
        ?? throw new InvalidOperationException("OpenAPI JSON could not be parsed.");
    var info = node["info"]?.AsObject() ?? new JsonObject();
    info["title"] = "Rails C# SDK sample";
    node["info"] = info;
    return node.ToJsonString(new System.Text.Json.JsonSerializerOptions { WriteIndented = true });
}
