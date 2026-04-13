using System.Net.Http;

namespace RailsSdkSample;

/// <summary>
/// Forwarded HTTP only — same idea as Kotlin's <c>java.net.http</c> client with optional trust-all TLS.
/// The Rails SDK client keeps default certificate verification unless you pass a custom <see cref="HttpClient"/> there too.
/// </summary>
public static class ProxyHttp
{
    public static HttpClient Create(bool insecureProxyTls)
    {
        if (!insecureProxyTls)
            return new HttpClient();

        var handler = new HttpClientHandler { ServerCertificateCustomValidationCallback = HttpClientHandler.DangerousAcceptAnyServerCertificateValidator };
        return new HttpClient(handler);
    }
}
