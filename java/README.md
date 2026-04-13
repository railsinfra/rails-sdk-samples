# Java sample

## What this is

A small JVM server that calls the Rails API with the official Java SDK (OkHttp). You only configure **base URL** and **API key**.

## Prerequisites

- **JDK** 17+
- This repository checked out with **`mvp/rails-sdks/sdks/rails-java`** (Gradle composite build). See the [root SDK samples README](../README.md).

## Install dependencies

```bash
cd sdk-samples/java
./gradlew --version
```

## Credentials

```bash
export RAILS_API_KEY='<your-api-key>'
export RAILS_BASE_URL='https://your-api-host.example'
```

Optional: **`RAILS_X_ENVIRONMENT`**, **`PORT`** (default `8081`).

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
