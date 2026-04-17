# TypeScript sample

## What this is

A small Express server that calls the Rails API with the official TypeScript SDK (`rails` package). You only configure **base URL** and **API key**.

## Prerequisites

- **Node.js** 18.17+ (20+ recommended)
- **Local SDK from source:** `package.json` uses `file:../../sdks/rails-typescript`, which matches this folder at **`src/rails-sdks/samples/typescript`** inside the Rails monorepo (sibling of `src/rails-sdks/sdks/`). Checked out alone as `rails-sdk-samples`, you do not have the monorepo `src/rails-sdks/` tree — use the published `rails` package from npm or change the `file:` path; see the [root SDK samples README](../README.md).

## Install dependencies

From the **`rails-sdk-samples` repository root**:

```bash
cd typescript
npm install
```

If you are in the **Rails monorepo** and the SDK package has never been built:

```bash
cd src/rails-sdks/sdks/rails-typescript && npm install && npm run build
```

*(Run that from the Rails repository root, not from `rails-sdk-samples`.)*

*Monorepo path to this app from the Rails repo root: `src/rails-sdks/samples/typescript`.*

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
