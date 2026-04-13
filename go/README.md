# Go sample

## What this is

An HTTP server (and optional CLI) that calls the Rails API with the official Go SDK. You only configure **base URL** and **API key**.

## Prerequisites

- **Go** 1.22+
- **Local SDK from source:** `go.mod` uses a `replace` that points at the generated Go SDK inside the **Rails monorepo** (`mvp/rails-sdks/sdks/rails-go`). That path only exists when this repo is checked out as part of that monorepo (for example at `mvp/rails-sdks/samples`). For a **standalone** clone of `rails-sdk-samples` only, either adjust `replace` to your SDK checkout or use a published module if available — see the [root SDK samples README](../README.md).

## Install dependencies

From the **`rails-sdk-samples` repository root** (standalone clone or submodule — there is no `mvp/` directory inside this repo):

```bash
cd go
go mod download
```

*If you use the full Rails monorepo, this folder is at `mvp/rails-sdks/samples/go` from the monorepo root.*

## Credentials

```bash
cp .env.example .env
```

Edit `.env`: **`RAILS_API_KEY`**, **`RAILS_BASE_URL`**, optional **`PORT`** (default **8083** for the HTTP sample).

Load env vars into your shell (example for bash):

```bash
set -a && source .env && set +a
```

## Run (HTTP server + Swagger)

```bash
go run ./cmd/sample-api
```

## Verify

- Swagger UI: **http://localhost:8083/** (or your `PORT`)
- OpenAPI JSON: **http://localhost:8083/openapi.json**

## Optional CLI smoke test

When **`RAILS_SAMPLE_ACCOUNT_ID`** is set:

```bash
go run .
```

## Troubleshooting

For dev TLS handshake / PKIX errors against a private CA, set **`RAILS_INSECURE_SSL=true`** in `.env` (see `.env.example`). You should see a log line that trust-all TLS is ON. Dev/staging only.
