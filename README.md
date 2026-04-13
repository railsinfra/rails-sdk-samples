# Rails SDK samples

Official **reference apps** for banks and integrators. Each sample is a small HTTP server with **Swagger UI** so you can try routes against the real Rails API using only:

1. **`RAILS_BASE_URL`** — API host we give you (for example `https://www.api.dev.railsinfra.com`).
2. **`RAILS_API_KEY`** — key provisioned for your institution.

You do not get access to our databases or internal services.

## Where to run these samples

These projects **link to the generated SDKs** in the parent Rails monorepo (`mvp/rails-sdks/` via Gradle `includeBuild`, Go `replace`, npm `file:`, or .NET `ProjectReference`). **Clone the main Rails repository** and work from the repo root so both `sdk-samples/` and `mvp/rails-sdks/` exist. If `sdk-samples` is a **git submodule**, initialize it after clone:

```bash
git submodule update --init --recursive
```

To publish `sdk-samples` as its own remote and attach it back as a submodule, see the root [README](../README.md) section **SDK sample apps** and [mvp/scripts/publish-sdk-samples-submodule.sh](../mvp/scripts/publish-sdk-samples-submodule.sh).

## Quick index

| Language   | Folder        | Default port | README |
|------------|---------------|--------------|--------|
| TypeScript | `typescript/` | 8081         | [typescript/README.md](typescript/README.md) |
| Kotlin     | `kotlin/`     | 8081         | [kotlin/README.md](kotlin/README.md) |
| Java       | `java/`       | 8081         | [java/README.md](java/README.md) |
| C#         | `csharp/`     | 8081         | [csharp/README.md](csharp/README.md) |
| Go         | `go/`         | 8083         | [go/README.md](go/README.md) |

Use a different port by setting **`PORT`** in `.env` (where supported) or your shell.

## Clone only one sample (sparse checkout)

You can clone this repository but **check out a single language folder** in your working tree (Git still fetches repo metadata, but you only materialize the paths you need):

```bash
git clone --filter=blob:none --sparse https://github.com/sibabale/rails-sdk-samples.git
cd rails-sdk-samples
git sparse-checkout set typescript
```

Replace `typescript` with `go`, `java`, `kotlin`, or `csharp` as needed. To add another folder later:

```bash
git sparse-checkout add go
```

**Note:** Samples are still wired to build against the generated SDKs inside the **Rails monorepo** (`mvp/rails-sdks/`). If you use a sparse clone **without** the parent repo, follow that language’s README for using **published** packages or adjust local paths.

## Credentials (all samples)

| Variable            | Required | Description |
|---------------------|----------|-------------|
| `RAILS_BASE_URL`    | Yes      | Rails API base URL. |
| `RAILS_API_KEY`     | Yes      | API key from the Rails team or your dashboard. |
| `RAILS_X_ENVIRONMENT` | No     | `sandbox` or `production` (default `sandbox`). |
| `PORT`              | No       | Local server port (defaults differ by language; see table). |

Copy `.env.example` to `.env` when the sample provides one, or export the variables in your shell.

## Verify locally

After you start a sample:

- Open **`http://localhost:<port>/`** for **Swagger UI** (unless the sample README says otherwise).
- Open **`http://localhost:<port>/openapi.json`** for the OpenAPI document.

## Troubleshooting (dev TLS)

If forwarded/proxy calls fail with **TLS / certificate / PKIX** errors against a dev host with a private CA, samples support a **dev-only** insecure flag (see each folder README). This does not relax TLS for all SDK paths unless noted.

---

## Rails maintainers — full API on your laptop

Bank integrators should **not** follow this. For Postgres, users service, accounts service, etc., use [mvp/docker-compose.sdk.yml](../mvp/docker-compose.sdk.yml), [mvp/scripts/prepare-local-db.sh](../mvp/scripts/prepare-local-db.sh), and READMEs under `mvp/api/`.
