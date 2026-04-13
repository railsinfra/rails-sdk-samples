# C# sample

## What this is

An ASP.NET Core app that calls the Rails API with the official C# SDK. You only configure **base URL** and **API key**.

## Prerequisites

- **.NET SDK** 8+
- This repository checked out with **`mvp/rails-sdks/sdks/rails-csharp`** (project reference). See the [root SDK samples README](../README.md).

## Install dependencies

```bash
cd sdk-samples/csharp
dotnet restore RailsSdkSample.sln
```

## Credentials

```bash
cp .env.example .env
```

Edit `.env` with **`RAILS_BASE_URL`** and **`RAILS_API_KEY`**. Optional: **`PORT`** (default `8081`), **`RAILS_X_ENVIRONMENT`**.

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
