# TypeScript sample

## What this is

A small Express server that calls the Rails API with the official TypeScript SDK (`rails` package). You only configure the **API key**.

## Prerequisites

- **Node.js** 18.17+ (20+ recommended)

## Install dependencies

From the **`rails-sdk-samples` repository root**:

```bash
cd typescript
npm install
```

## Credentials

```bash
cp .env.example .env
```

Edit `.env` and set:

- **`RAILS_BASE_URL`** — use the value from `.env.example` (do not change unless onboarding gave a different host)
- **`RAILS_API_KEY`** — sign in at https://railsinfra.com and create a server API key, then paste it here
- Optional: **`PORT`** (default `8081`)

## Run

```bash
npm run dev
```

## Verify

- Swagger UI: **http://localhost:8081/** (or your `PORT`)
- OpenAPI JSON: **http://localhost:8081/openapi.json**

## Troubleshooting

Forwarded routes use `fetch`. For dev TLS issues against a private CA, set **`RAILS_INSECURE_SSL=true`** (this sample only). You should see `Proxy fetch trust-all TLS: ON` in the log. SDK routes are unchanged unless you pass a custom `fetch` into the client.
