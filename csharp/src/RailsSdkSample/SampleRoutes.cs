using System.Collections.Frozen;
using System.Text;
using System.Text.Json;
using Rails.Core;
using Rails.Models.Accounts;
using Rails.Models.Transactions;

namespace RailsSdkSample;

public static class SampleRoutes
{
    static string TrimBase(string u) => u.TrimEnd('/');

    static FrozenDictionary<string, JsonElement> EmptyQuery => FrozenDictionary<string, JsonElement>.Empty;

    static FrozenDictionary<string, JsonElement> EnvHeaders(string railsXEnvironment) =>
        FrozenDictionary.ToFrozenDictionary(
            new Dictionary<string, JsonElement>
            {
                ["X-Environment"] = JsonSerializer.SerializeToElement(railsXEnvironment),
            }
        );

    static string GenIdempotencyKey(string prefix) =>
        $"{prefix}-{DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()}-{Guid.NewGuid():N}";

    static async Task ForwardJson(HttpContext ctx, HttpClient http, Uri url, HttpMethod method, string? jsonBody, Dictionary<string, string> headers)
    {
        using var msg = new HttpRequestMessage(method, url);
        foreach (var (k, v) in headers)
            msg.Headers.TryAddWithoutValidation(k, v);
        if (jsonBody != null)
            msg.Content = new StringContent(jsonBody, Encoding.UTF8, "application/json");
        using var resp = await http.SendAsync(msg, HttpCompletionOption.ResponseHeadersRead, ctx.RequestAborted);
        var text = await resp.Content.ReadAsStringAsync(ctx.RequestAborted);
        ctx.Response.StatusCode = (int)resp.StatusCode;
        ctx.Response.ContentType = "application/json";
        await ctx.Response.WriteAsync(text, ctx.RequestAborted);
    }

    public static void Map(WebApplication app, SampleRouteDeps d)
    {
        var root = TrimBase(d.BaseUrl);

        async Task PostCreate(HttpContext ctx)
        {
            var xEnv = ctx.Request.Headers["X-Environment"].FirstOrDefault() ?? "sandbox";
            var body = await new StreamReader(ctx.Request.Body).ReadToEndAsync(ctx.RequestAborted);
            await ForwardJson(
                ctx,
                d.ProxyHttp,
                new Uri($"{root}/api/v1/accounts"),
                HttpMethod.Post,
                string.IsNullOrEmpty(body) ? "{}" : body,
                new Dictionary<string, string>
                {
                    ["Content-Type"] = "application/json",
                    ["X-API-Key"] = d.ApiKey,
                    ["X-Environment"] = xEnv,
                }
            );
        }

        async Task GetListAccounts(HttpContext ctx)
        {
            var xEnv = ctx.Request.Headers["X-Environment"].FirstOrDefault() ?? "sandbox";
            var qs = ctx.Request.QueryString.HasValue ? ctx.Request.QueryString.Value : "";
            await ForwardJson(
                ctx,
                d.ProxyHttp,
                new Uri($"{root}/api/v1/accounts{qs}"),
                HttpMethod.Get,
                null,
                new Dictionary<string, string> { ["X-API-Key"] = d.ApiKey, ["X-Environment"] = xEnv }
            );
        }

        async Task PostDeposit(HttpContext ctx, string id)
        {
            if (string.IsNullOrEmpty(id))
                throw new HttpError(400, "missing id");
            var body = await new StreamReader(ctx.Request.Body).ReadToEndAsync(ctx.RequestAborted);
            var t = body.Trim();
            if (t is "{}" or "null" or "")
                throw new HttpError(400, "missing body");
            var idem = ctx.Request.Headers["Idempotency-Key"].FirstOrDefault() ?? GenIdempotencyKey("dep");
            var xEnv = ctx.Request.Headers["X-Environment"].FirstOrDefault();
            var headers = new Dictionary<string, string>
            {
                ["Content-Type"] = "application/json",
                ["X-API-Key"] = d.ApiKey,
                ["Idempotency-Key"] = idem,
            };
            if (xEnv is "sandbox" or "production")
                headers["X-Environment"] = xEnv;
            await ForwardJson(
                ctx,
                d.ProxyHttp,
                new Uri($"{root}/api/v1/accounts/{Uri.EscapeDataString(id)}/deposit"),
                HttpMethod.Post,
                body,
                headers
            );
        }

        async Task PostTransfer(HttpContext ctx, string id)
        {
            if (string.IsNullOrEmpty(id))
                throw new HttpError(400, "missing id");
            var body = await new StreamReader(ctx.Request.Body).ReadToEndAsync(ctx.RequestAborted);
            var t = body.Trim();
            if (t is "{}" or "null" or "")
                throw new HttpError(400, "missing body");
            var idem = ctx.Request.Headers["Idempotency-Key"].FirstOrDefault() ?? GenIdempotencyKey("xfr");
            var xEnv = ctx.Request.Headers["X-Environment"].FirstOrDefault();
            var headers = new Dictionary<string, string>
            {
                ["Content-Type"] = "application/json",
                ["X-API-Key"] = d.ApiKey,
                ["Idempotency-Key"] = idem,
            };
            if (xEnv is "sandbox" or "production")
                headers["X-Environment"] = xEnv;
            await ForwardJson(
                ctx,
                d.ProxyHttp,
                new Uri($"{root}/api/v1/accounts/{Uri.EscapeDataString(id)}/transfer"),
                HttpMethod.Post,
                body,
                headers
            );
        }

        async Task PostWithdraw(HttpContext ctx, string id)
        {
            if (string.IsNullOrEmpty(id))
                throw new HttpError(400, "missing id");
            var body = await new StreamReader(ctx.Request.Body).ReadToEndAsync(ctx.RequestAborted);
            var t = body.Trim();
            if (t is "{}" or "null" or "")
                throw new HttpError(400, "missing body");
            var idem = ctx.Request.Headers["Idempotency-Key"].FirstOrDefault() ?? GenIdempotencyKey("wdr");
            var xEnv = ctx.Request.Headers["X-Environment"].FirstOrDefault();
            var headers = new Dictionary<string, string>
            {
                ["Content-Type"] = "application/json",
                ["X-API-Key"] = d.ApiKey,
                ["Idempotency-Key"] = idem,
            };
            if (xEnv is "sandbox" or "production")
                headers["X-Environment"] = xEnv;
            await ForwardJson(
                ctx,
                d.ProxyHttp,
                new Uri($"{root}/api/v1/accounts/{Uri.EscapeDataString(id)}/withdraw"),
                HttpMethod.Post,
                body,
                headers
            );
        }

        async Task GetAccount(HttpContext ctx, string id)
        {
            if (string.IsNullOrEmpty(id))
                throw new HttpError(400, "missing id");
            var xEnv = SampleConfig.ResolveRailsXEnvironment(
                ctx.Request.Headers["X-Environment"].FirstOrDefault(),
                d.RailsXEnvironment
            );
            var data = await d.Client.Accounts.Retrieve(
                AccountRetrieveParams.FromRawUnchecked(EnvHeaders(xEnv), EmptyQuery, id),
                ctx.RequestAborted
            );
            await ctx.Response.WriteAsJsonAsync(data, ctx.RequestAborted);
        }

        async Task DeleteAccount(HttpContext ctx, string id)
        {
            if (string.IsNullOrEmpty(id))
                throw new HttpError(400, "missing id");
            var xEnv = SampleConfig.ResolveRailsXEnvironment(
                ctx.Request.Headers["X-Environment"].FirstOrDefault(),
                d.RailsXEnvironment
            );
            var data = await d.Client.Accounts.Close(
                AccountCloseParams.FromRawUnchecked(EnvHeaders(xEnv), EmptyQuery, id),
                ctx.RequestAborted
            );
            await ctx.Response.WriteAsJsonAsync(data, ctx.RequestAborted);
        }

        async Task PatchStatus(HttpContext ctx, string id)
        {
            if (string.IsNullOrEmpty(id))
                throw new HttpError(400, "missing id");
            var body = await ctx.Request.ReadFromJsonAsync<PatchStatusBody>(ctx.RequestAborted);
            if (string.IsNullOrWhiteSpace(body?.Status))
                throw new HttpError(400, "status required");
            if (body.Status is not ("active" or "suspended" or "closed"))
                throw new HttpError(400, "invalid status");
            var xEnv = SampleConfig.ResolveRailsXEnvironment(
                ctx.Request.Headers["X-Environment"].FirstOrDefault(),
                d.RailsXEnvironment
            );
            var frozenBody = FrozenDictionary.ToFrozenDictionary(
                new Dictionary<string, JsonElement> { ["status"] = JsonSerializer.SerializeToElement(body.Status) }
            );
            var data = await d.Client.Accounts.UpdateStatus(
                AccountUpdateStatusParams.FromRawUnchecked(EnvHeaders(xEnv), EmptyQuery, frozenBody, id),
                ctx.RequestAborted
            );
            await ctx.Response.WriteAsJsonAsync(data, ctx.RequestAborted);
        }

        async Task RawGet(HttpContext ctx)
        {
            var pathParam = ctx.Request.Query["path"].FirstOrDefault() ?? "api/v1/accounts";
            pathParam = pathParam.TrimStart('/');
            await ForwardJson(
                ctx,
                d.ProxyHttp,
                new Uri($"{root}/{pathParam}"),
                HttpMethod.Get,
                null,
                new Dictionary<string, string> { ["X-API-Key"] = d.ApiKey }
            );
        }

        async Task RawPost(HttpContext ctx)
        {
            var pathParam = ctx.Request.Query["path"].FirstOrDefault() ?? "api/v1/accounts";
            pathParam = pathParam.TrimStart('/');
            var body = await new StreamReader(ctx.Request.Body).ReadToEndAsync(ctx.RequestAborted);
            await ForwardJson(
                ctx,
                d.ProxyHttp,
                new Uri($"{root}/{pathParam}"),
                HttpMethod.Post,
                string.IsNullOrEmpty(body) ? "{}" : body,
                new Dictionary<string, string> { ["Content-Type"] = "application/json", ["X-API-Key"] = d.ApiKey }
            );
        }

        async Task GetTx(HttpContext ctx, string id)
        {
            if (string.IsNullOrEmpty(id))
                throw new HttpError(400, "missing id");
            var xEnv = SampleConfig.ResolveRailsXEnvironment(
                ctx.Request.Headers["X-Environment"].FirstOrDefault(),
                d.RailsXEnvironment
            );
            var data = await d.Client.Transactions.Retrieve(
                TransactionRetrieveParams.FromRawUnchecked(EnvHeaders(xEnv), EmptyQuery, id),
                ctx.RequestAborted
            );
            await ctx.Response.WriteAsJsonAsync(data, ctx.RequestAborted);
        }

        async Task ListByAccount(HttpContext ctx, string accountId)
        {
            if (string.IsNullOrEmpty(accountId))
                throw new HttpError(400, "missing accountId");
            var xEnv = SampleConfig.ResolveRailsXEnvironment(
                ctx.Request.Headers["X-Environment"].FirstOrDefault(),
                d.RailsXEnvironment
            );
            long? limit = null;
            var limitRaw = ctx.Request.Query["limit"].FirstOrDefault();
            if (limitRaw != null && long.TryParse(limitRaw, out var lim))
                limit = lim;
            var q = new Dictionary<string, JsonElement>();
            if (limit is { } l)
                q["limit"] = JsonSerializer.SerializeToElement(l);
            var frozenQ = q.Count > 0 ? FrozenDictionary.ToFrozenDictionary(q) : EmptyQuery;
            var data = await d.Client.Transactions.ListByAccount(
                TransactionListByAccountParams.FromRawUnchecked(EnvHeaders(xEnv), frozenQ, accountId),
                ctx.RequestAborted
            );
            await ctx.Response.WriteAsJsonAsync(data, ctx.RequestAborted);
        }

        app.MapPost("/api/accounts", PostCreate);
        app.MapPost("/api/v1/accounts", PostCreate);
        app.MapGet("/api/accounts", GetListAccounts);
        app.MapGet("/api/v1/accounts", GetListAccounts);

        app.MapPost("/api/accounts/{id}/deposit", PostDeposit);
        app.MapPost("/api/v1/accounts/{id}/deposit", PostDeposit);
        app.MapPost("/api/accounts/{id}/transfer", PostTransfer);
        app.MapPost("/api/v1/accounts/{id}/transfer", PostTransfer);
        app.MapPost("/api/accounts/{id}/withdraw", PostWithdraw);
        app.MapPost("/api/v1/accounts/{id}/withdraw", PostWithdraw);

        app.MapGet("/api/accounts/{id}", GetAccount);
        app.MapGet("/api/v1/accounts/{id}", GetAccount);
        app.MapDelete("/api/accounts/{id}", DeleteAccount);
        app.MapDelete("/api/v1/accounts/{id}", DeleteAccount);

        app.MapPatch("/api/accounts/{id}/status", PatchStatus);
        app.MapPatch("/api/v1/accounts/{id}/status", PatchStatus);
        app.MapPatch("/api/accounts/{id}", PatchStatus);
        app.MapPatch("/api/v1/accounts/{id}", PatchStatus);

        app.MapGet("/api/raw/get", RawGet);
        app.MapGet("/api/v1/raw/get", RawGet);
        app.MapPost("/api/raw/post", RawPost);
        app.MapPost("/api/v1/raw/post", RawPost);

        app.MapGet("/api/transactions/{id}", GetTx);
        app.MapGet("/api/v1/transactions/{id}", GetTx);
        app.MapGet("/api/accounts/{accountId}/transactions", ListByAccount);
        app.MapGet("/api/v1/accounts/{accountId}/transactions", ListByAccount);
    }

    sealed record PatchStatusBody(string? Status);
}
