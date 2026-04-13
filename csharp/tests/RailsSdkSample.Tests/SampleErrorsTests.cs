using System.Net.Http;

namespace RailsSdkSample.Tests;

public class SampleErrorsTests
{
    [Fact]
    public void IsTlsOrCertError_DetectsHandshakeWording()
    {
        Assert.True(SampleErrors.IsTlsOrCertError(new HttpRequestException("The SSL connection could not be established.")));
        Assert.True(SampleErrors.IsTlsOrCertError(new Exception("PKIX path building failed")));
    }

    [Fact]
    public void IsTlsOrCertError_ReturnsFalseForUnrelatedErrors()
    {
        Assert.False(SampleErrors.IsTlsOrCertError(new Exception("ENOTFOUND")));
    }

    [Fact]
    public void ToErrorBody_MapsHttpError()
    {
        var b = SampleErrors.ToErrorBody(new HttpError(400, "bad"), "/x");
        Assert.Equal(400, b.Status);
        Assert.Equal("bad", b.Message);
        Assert.Equal(nameof(HttpError), b.Exception);
        Assert.Equal("/x", b.Path);
    }

    [Fact]
    public void ToErrorBody_MapsTlsErrorsTo502WithGuidance()
    {
        var b = SampleErrors.ToErrorBody(new Exception("certificate has expired"), "/proxy");
        Assert.Equal(502, b.Status);
        Assert.Contains("RAILS_INSECURE_SSL", b.Message, StringComparison.Ordinal);
    }
}
