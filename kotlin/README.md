# Kotlin sample

## What this is

A small JVM server that calls the Rails API with the official Kotlin SDK. You only configure **base URL** and **API key**.

## Prerequisites

- **JDK** 17+
- This repository checked out with **`mvp/rails-sdks/sdks/rails-kotlin`** (Gradle composite build). See the [root SDK samples README](../README.md).

## Install dependencies

```bash
cd mvp/sdk-samples/kotlin
./gradlew --version   # downloads wrapper if needed; first run may fetch dependencies
```

## Credentials

```bash
export RAILS_API_KEY='<your-api-key>'
export RAILS_BASE_URL='https://your-api-host.example'
```

Optional: **`RAILS_X_ENVIRONMENT`** (`sandbox` | `production`, default `sandbox`), **`PORT`** (default `8081`).

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
