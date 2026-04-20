# Java sample

## What this is

A small JVM server that calls the Rails API with the official Java SDK (OkHttp). You only configure **base URL** and **API key**.

## Prerequisites

- **JDK** 17+
- **Local SDK from source:** Gradle `includeBuild` expects the generated Java SDK under **`src/rails-sdks/sdks/rails-java`** in the **Rails monorepo**, which exists when this repo lives inside that tree (for example at `src/rails-sdks/samples`). A standalone clone of `rails-sdk-samples` alone does not contain the monorepo `src/rails-sdks/` layout — use published packages or point the composite build at your SDK checkout; see the [root SDK samples README](../README.md).

## Install dependencies

From the **`rails-sdk-samples` repository root**:

```bash
cd java
./gradlew --version
```

*Monorepo path from the Rails repo root: `src/rails-sdks/samples/java`.*

## Credentials

```bash
export RAILS_API_KEY='<your-api-key>'
export RAILS_BASE_URL='https://api.railsinfra.com'
```

Sign in at https://railsinfra.com and create a server API key for **`RAILS_API_KEY`**. Use the same **`RAILS_BASE_URL`** rules as the [root README](../README.md#credentials-reference) (match other samples’ `.env.example` unless onboarding gave you a different host).

Optional: **`PORT`** (default `8081`).

## Run

```bash
./gradlew run
```

## Verify

- Swagger UI: **http://localhost:8081/** (or your `PORT`)
- OpenAPI JSON: **http://localhost:8081/openapi.json**

## Troubleshooting

If you see **PKIX / SSLHandshakeException**, this sample can disable TLS verification for **both** forwarded `java.net.http` and the **OkHttp** SDK client (dev only):

- `export RAILS_INSECURE_SSL=true`, or  
- `./gradlew run -PrailsInsecureSsl=true`  

Check for `Proxy HttpClient trust-all TLS: ON`. IDE: env var or `-Drails.insecure.ssl=true`.
