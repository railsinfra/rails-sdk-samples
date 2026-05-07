namespace RailsSdkSample;

/// <summary>Value for <c>X-Environment</c> on Rails account API calls (required by the backend).</summary>
public static class SampleConfig
{
    public static SampleSettings Load()
    {
        var baseUrl = NormalizeBaseUrl(Environment.GetEnvironmentVariable("RAILS_BASE_URL") ?? "https://api.railsinfra.com");
        var apiKey = Environment.GetEnvironmentVariable("RAILS_API_KEY") ?? "";
        var port = int.TryParse(Environment.GetEnvironmentVariable("PORT"), out var p) && p > 0 ? p : 8081;
        var insecure =
            string.Equals(Environment.GetEnvironmentVariable("RAILS_INSECURE_SSL"), "true", StringComparison.OrdinalIgnoreCase)
            || Environment.GetEnvironmentVariable("RAILS_INSECURE_SSL") == "1";
        var railsXEnvironment = DefaultRailsXEnvironmentFromEnv();
        return new SampleSettings(baseUrl, apiKey, port, insecure, railsXEnvironment);
    }

    public static string DefaultRailsXEnvironmentFromEnv() => "sandbox";

    /// <summary>This sample always uses the sandbox environment for Rails account API calls.</summary>
    public static string ResolveRailsXEnvironment(string? _, string __) => "sandbox";

    static string NormalizeBaseUrl(string raw)
    {
        var t = raw.Trim().TrimEnd('/');
        if (t.Length == 0)
            return "https://api.railsinfra.com";
        if (t.StartsWith("http://", StringComparison.OrdinalIgnoreCase)
            || t.StartsWith("https://", StringComparison.OrdinalIgnoreCase))
            return t;
        return "https://" + t;
    }
}

public sealed record SampleSettings(
    string BaseUrl,
    string ApiKey,
    int Port,
    bool InsecureProxyTls,
    string RailsXEnvironment
);
