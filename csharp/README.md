# C# sample

## What this is

An ASP.NET Core app that calls the Rails API with the official C# SDK. You only configure **base URL** and **API key**.

## Prerequisites

- **.NET SDK** 8+
- **Local SDK from source:** the `.csproj` references **`src/rails-sdks/sdks/rails-csharp`** relative to the Rails monorepo layout (this repo at `src/rails-sdks/samples`). A standalone `rails-sdk-samples` clone has no monorepo `src/rails-sdks/` tree — repoint the project reference or use a published package; see the [root SDK samples README](../README.md).

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

Edit `.env`: keep **`RAILS_BASE_URL`** as in `.env.example` unless onboarding gave a different host; set **`RAILS_API_KEY`** from https://railsinfra.com (sign in, create a server API key). Optional: **`PORT`** (default `8081`), **`RAILS_X_ENVIRONMENT`**.

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

Forwarded routes use `HttpClient`. For dev TLS issues, set **`RAILS_INSECURE_SSL=true`**. You should see `Proxy HttpClient trust-all TLS: ON`. SDK routes stay strict unless you pass a custom `HttpClient` into `RailsClient`.
