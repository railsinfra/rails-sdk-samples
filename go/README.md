# Go sample

## What this is

A small HTTP server that calls the Rails API with the official Go SDK (`github.com/railsinfra/rails-go`). You only configure the **API key**.

## Prerequisites

- **Go** 1.22+
- **Local SDK:** `go.mod` uses `replace` → `../../rails-sdks/rails-go` (sibling `rails-sdks/rails-go` next to this repo). Repoint or use a published module if your layout differs — see the [root SDK samples README](../README.md).

## Install dependencies

From the **`rails-sdk-samples` repository root**:

```bash
cd go
go mod download
```

## Credentials

```bash
cp .env.example .env
```

Edit `.env` and set:

- **`RAILS_BASE_URL`** — use the value from `.env.example` (do not change unless onboarding gave a different host)
- **`RAILS_API_KEY`** — sign in at https://railsinfra.com and create a server API key, then paste it here
- Optional: **`PORT`** (default `8083`)

Export variables into your environment before you run; this sample does not load `.env` files (for example, in bash: `set -a && source .env && set +a`).

## Run

```bash
set -a && source .env && set +a && go run ./cmd/sample-api
```

With **`RAILS_SAMPLE_ACCOUNT_ID`** set in the environment, `go run .` runs an optional CLI smoke test.

## Verify

- Swagger UI: **http://localhost:8083/** (or your `PORT`)
- OpenAPI JSON: **http://localhost:8083/openapi.json**

## Troubleshooting

For dev TLS issues against a private CA, set **`RAILS_INSECURE_SSL=true`** (this sample only). You should see `[rails-go-sample] outbound HTTP client: trust-all TLS ON ...` in the log. Dev/staging only.

### Module path issues

If you encounter import errors, ensure the `go.mod` file has the correct module path and replace directive:

```go
module github.com/rails/sdk-samples/go

require github.com/railsinfra/rails-go v0.0.0

replace github.com/railsinfra/rails-go => ../../rails-sdks/rails-go
```

The local SDK should be at `../../rails-sdks/rails-go` relative to this sample directory (adjust the path if your SDK layout differs).
