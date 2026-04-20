# Kotlin sample

## What this is

A small JVM server that calls the Rails API with the official Kotlin SDK. You only configure **base URL** and **API key**.

## Prerequisites

- **JDK** 17+
- **Local SDK from source:** Gradle `includeBuild` expects **`src/rails-sdks/sdks/rails-kotlin`** in the **Rails monorepo** (this repo is usually embedded at `src/rails-sdks/samples`). Standalone `rails-sdk-samples` clones do not include the monorepo `src/rails-sdks/` tree — see the [root SDK samples README](../README.md).

## Install dependencies

From the **`rails-sdk-samples` repository root**:

```bash
cd kotlin
./gradlew --version   # downloads wrapper if needed; first run may fetch dependencies
```

*Monorepo path from the Rails repo root: `src/rails-sdks/samples/kotlin`.*

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

If forwarded `java.net.http` calls fail with **PKIX / SSLHandshakeException**, for **this sample only**:

- `export RAILS_INSECURE_SSL=true` before `./gradlew run`, or  
- `./gradlew run -PrailsInsecureSsl=true`  

Check stderr for `Proxy HttpClient trust-all TLS: ON`. This does **not** change OkHttp (SDK) routes. IDE runs: set the env var or pass `-Drails.insecure.ssl=true` on the JVM.
