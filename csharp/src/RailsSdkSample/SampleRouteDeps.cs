using Rails;

namespace RailsSdkSample;

public sealed record SampleRouteDeps(
    string BaseUrl,
    string ApiKey,
    RailsClient Client,
    HttpClient ProxyHttp,
    string RailsXEnvironment
);
