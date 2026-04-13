using System.Net.Http;
using System.Net.Security;
using System.Security.Authentication;
using Rails.Exceptions;

namespace RailsSdkSample;

public static class SampleErrors
{
    public static void LogTlsMode(bool insecure)
    {
        Console.Error.WriteLine(
            "[rails-sdk-sample] Proxy HttpClient trust-all TLS: "
                + (insecure ? "ON" : "OFF")
                + " (set RAILS_INSECURE_SSL=true if PKIX / handshake errors persist on forwarded routes only)"
        );
    }

    public static bool IsTlsOrCertError(Exception? err)
    {
        for (var e = err; e != null; e = e.InnerException)
        {
            if (e is AuthenticationException)
                return true;
            if (e is HttpRequestException hre && IsTlsMessage(hre.Message))
                return true;
            if (IsTlsMessage(e.Message))
                return true;
        }

        return false;
    }

    static bool IsTlsMessage(string? msg)
    {
        if (string.IsNullOrEmpty(msg))
            return false;
        return msg.Contains("SSL", StringComparison.OrdinalIgnoreCase)
            || msg.Contains("TLS", StringComparison.OrdinalIgnoreCase)
            || msg.Contains("handshake", StringComparison.OrdinalIgnoreCase)
            || msg.Contains("certificate", StringComparison.OrdinalIgnoreCase)
            || msg.Contains("PKIX", StringComparison.OrdinalIgnoreCase)
            || msg.Contains("remote certificate", StringComparison.OrdinalIgnoreCase);
    }

    public static ErrorBody ToErrorBody(Exception err, string path)
    {
        if (err is HttpError httpErr)
        {
            return new ErrorBody
            {
                Status = httpErr.Status,
                Message = httpErr.Message,
                Exception = httpErr.Code ?? nameof(HttpError),
                Path = path,
            };
        }

        if (err is RailsApiException apiEx)
        {
            return new ErrorBody
            {
                Status = (int)apiEx.StatusCode,
                Message = apiEx.Message,
                Exception = apiEx.GetType().Name,
                Path = path,
            };
        }

        if (IsTlsOrCertError(err))
        {
            return new ErrorBody
            {
                Status = 502,
                Message =
                    "TLS handshake or certificate verification failed when calling the upstream API. For local dev against a private CA, set RAILS_INSECURE_SSL=true (proxy calls only; SDK routes still use strict TLS).",
                Exception = err.GetType().Name,
                Path = path,
            };
        }

        return new ErrorBody
        {
            Status = 500,
            Message = err.Message,
            Exception = err.GetType().Name,
            Path = path,
        };
    }
}
