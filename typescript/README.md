# TypeScript sample

## What this is

A small Express server that calls the Rails API with the official TypeScript SDK (`rails` package). You only configure **base URL** and **API key**.

## Prerequisites

- **Node.js** 18.17+ (20+ recommended)
- This repository checked out with **`mvp/rails-sdks/`** present (the sample depends on `file:../../mvp/rails-sdks/sdks/rails-typescript`). See the [root SDK samples README](../README.md).

## Install dependencies

```bash
cd sdk-samples/typescript
npm install
```

If the local SDK has no build output yet, from the repo root:

```bash
cd mvp/rails-sdks/sdks/rails-typescript && npm install && npm run build
```

## Credentials

```bash
cp .env.example .env
```

Edit `.env` and set:

- **`RAILS_BASE_URL`** — API host from Rails or your dashboard
- **`RAILS_API_KEY`** — your API key  
- Optional: **`PORT`** (default `8081`), **`RAILS_X_ENVIRONMENT`** (`sandbox` | `production`)

## Run

```bash
npm run dev
```

## Verify

- Swagger UI: **http://localhost:8081/** (or your `PORT`)
- OpenAPI JSON: **http://localhost:8081/openapi.json**

## Troubleshooting

Forwarded routes use `fetch`. For dev TLS issues against a private CA, set **`RAILS_INSECURE_SSL=true`** (this sample only). You should see `Proxy fetch trust-all TLS: ON` in the log. SDK routes are unchanged unless you pass a custom `fetch` into the client.
