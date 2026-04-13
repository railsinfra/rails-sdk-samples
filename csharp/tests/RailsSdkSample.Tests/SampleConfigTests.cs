namespace RailsSdkSample.Tests;

public class SampleConfigTests
{
    [Fact]
    public void ResolveRailsXEnvironment_UsesSandboxOrProductionFromRequestWhenValid()
    {
        Assert.Equal("production", SampleConfig.ResolveRailsXEnvironment("production", "sandbox"));
        Assert.Equal("sandbox", SampleConfig.ResolveRailsXEnvironment("Sandbox", "production"));
    }

    [Fact]
    public void ResolveRailsXEnvironment_FallsBackWhenHeaderMissingOrInvalid()
    {
        Assert.Equal("production", SampleConfig.ResolveRailsXEnvironment(null, "production"));
        Assert.Equal("sandbox", SampleConfig.ResolveRailsXEnvironment("staging", "sandbox"));
    }
}
