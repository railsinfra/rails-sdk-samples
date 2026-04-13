# Go sample

## What this is

An HTTP server (and optional CLI) that calls the Rails API with the official Go SDK. You only configure **base URL** and **API key**.

## Prerequisites

- **Go** 1.22+
- This repository checked out with **`mvp/rails-sdks/sdks/rails-go`** (`replace` in `go.mod`). See the [root SDK samples README](../README.md).

## Install dependencies

```bash
cd mvp/sdk-samples/go
go mod download
```

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
