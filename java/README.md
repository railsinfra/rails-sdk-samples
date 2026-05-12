# Java sample

## What this is

A small JVM server that calls the Rails API with the official Java SDK (OkHttp). You only configure **base URL** and **API key**.

## Prerequisites

- **JDK** 17+
- **Local SDK from source:** Gradle `includeBuild` expects the generated Java SDK at **`../../rails-sdks/java`** relative to this folder. In the default workspace layout, `rails-sdk-samples/` and `rails-sdks/` are sibling directories.

## Install dependencies

From the **`rails-sdk-samples` repository root**:

```bash
cd java
./gradlew --version
```

*Default workspace path: `rails-sdk-samples/java` next to `rails-sdks/java`.*

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
