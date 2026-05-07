# C# sample

## What this is

An ASP.NET Core app that calls the Rails API with the official C# SDK. You only configure **base URL** and **API key**.

## Prerequisites

- **.NET SDK** 8+
- **Local SDK from source:** the `.csproj` references **`src/rails-sdks/rails-csharp`** relative to the Rails monorepo layout (this repo at `src/rails-sdks/samples`). A standalone `rails-sdk-samples` clone has no monorepo `src/rails-sdks/` tree — repoint the project reference or use a published package; see the [root SDK samples README](../README.md).

## Install dependencies

From the **`rails-sdk-samples` repository root**:

```bash
cd csharp
dotnet restore RailsSdkSample.sln
```

*Monorepo path from the Rails repo root: `src/rails-sdks/samples/csharp`.*

## Credentials

```bash
cp .env.example .env
```

Edit `.env`: keep **`RAILS_BASE_URL`** as in `.env.example` unless onboarding gave a different host; set **`RAILS_API_KEY`** from https://railsinfra.com (sign in, create a server API key). Optional: **`PORT`** (default `8081`).

Dotnet loads `.env` via DotNetEnv on startup (see sample code).

## Run

```bash
dotnet run --project src/RailsSdkSample/RailsSdkSample.csproj
```

## Verify

- Swagger UI: **http://localhost:8081/** (or your `PORT`)
- OpenAPI JSON: **http://localhost:8081/openapi.json**

## Tests

```bash
dotnet test RailsSdkSample.sln
```

## Troubleshooting

### SDK path issues

If you encounter build errors related to the local SDK, ensure the `.csproj` file has the correct ProjectReference path:

```xml
<ProjectReference Include="../../../../rails-sdks/rails-csharp/src/Rails/Rails.csproj" />
```

The local SDK should be at `../../../../rails-sdks/rails-csharp/src/Rails/Rails.csproj` relative to the sample project file (adjust the path if your SDK layout differs).

### TLS issues

Forwarded routes use `HttpClient`. For dev TLS issues, set **`RAILS_INSECURE_SSL=true`**. You should see `Proxy HttpClient trust-all TLS: ON`. SDK routes stay strict unless you pass a custom `HttpClient` into `RailsClient`.

### Restore fails with MSB4019 (`Microsoft.NET.Sdk.Common.targets` not found)

That usually means **`MSBuildSDKsPath`** is set to `{dotnet root}/Sdks`. Current SDKs live under **`{dotnet root}/sdk/<version>/Sdks`**, so the import path is wrong (common with old blog posts or mixed Intel/arm64 installs). **Remove the variable** from your shell profile or IDE environment, then open a new terminal. Quick one-off: `unset MSBuildSDKsPath` (Unix) or `env -u MSBuildSDKsPath dotnet restore RailsSdkSample.sln`. This workspace’s `.vscode/settings.json` clears it for **new integrated terminals** in VS Code/Cursor only; other tools still need the variable removed globally.
