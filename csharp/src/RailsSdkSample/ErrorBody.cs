namespace RailsSdkSample;

/// <summary>Uniform JSON error body for handlers + middleware (aligned with Kotlin <c>ErrorResponse</c>).</summary>
public sealed class ErrorBody
{
    public int Status { get; init; }
    public string Message { get; init; } = "";
    public string? Exception { get; init; }
    public string? Path { get; init; }
}

public sealed class HttpError : Exception
{
    public int Status { get; }
    public string? Code { get; }

    public HttpError(int status, string message, string? code = null)
        : base(message)
    {
        Status = status;
        Code = code;
    }
}
