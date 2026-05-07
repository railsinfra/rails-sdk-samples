namespace RailsSdkSample.Tests;

public class SampleConfigTests
{
    [Fact]
    public void ResolveRailsXEnvironment_AlwaysSandbox()
    {
        Assert.Equal("sandbox", SampleConfig.ResolveRailsXEnvironment("production", "sandbox"));
        Assert.Equal("sandbox", SampleConfig.ResolveRailsXEnvironment("Sandbox", "production"));
        Assert.Equal("sandbox", SampleConfig.ResolveRailsXEnvironment(null, "production"));
        Assert.Equal("sandbox", SampleConfig.ResolveRailsXEnvironment("staging", "sandbox"));
    }

    [Fact]
    public void DefaultRailsXEnvironmentFromEnv_AlwaysSandbox()
    {
        Assert.Equal("sandbox", SampleConfig.DefaultRailsXEnvironmentFromEnv());
    }
}
