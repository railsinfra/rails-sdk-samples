# Kotlin sample

## What this is

A small JVM server that calls the Rails API with the official Kotlin SDK. You only configure **base URL** and **API key**.

## Prerequisites

- **JDK** 17+
- **Local SDK from source:** Gradle `includeBuild` expects **`../../rails-sdks/rails-kotlin`** relative to this folder. In the default workspace layout, `rails-sdk-samples/` and `rails-sdks/` are sibling directories.

## Install dependencies

From the **`rails-sdk-samples` repository root**:

```bash
cd kotlin
./gradlew --version   # downloads wrapper if needed; first run may fetch dependencies
```

*Default workspace path: `rails-sdk-samples/kotlin` next to `rails-sdks/rails-kotlin`.*

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

### SDK path issues

If you encounter build errors related to the local SDK, ensure the `settings.gradle.kts` file has the correct includeBuild path:

```kotlin
includeBuild("../../rails-sdks/rails-kotlin") {
    dependencySubstitution {
        substitute(module("com.railsinfra:rails-kotlin")).using(project(":rails-kotlin"))
    }
}
```

The local SDK should be at `../../rails-sdks/rails-kotlin` relative to this sample directory (adjust the path if your SDK layout differs).

### TLS issues

If forwarded `java.net.http` calls fail with **PKIX / SSLHandshakeException**, for **this sample only**:

- `export RAILS_INSECURE_SSL=true` before `./gradlew run`, or  
- `./gradlew run -PrailsInsecureSsl=true`  

Check stderr for `Proxy HttpClient trust-all TLS: ON`. This does **not** change OkHttp (SDK) routes. IDE runs: set the env var or pass `-Drails.insecure.ssl=true` on the JVM.
