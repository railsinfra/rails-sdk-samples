# Rails SDK samples

Official **reference apps** for banks and integrators. Each sample is a small HTTP server with **Swagger UI** so you can try routes against the real Rails API using only **`RAILS_BASE_URL`** and **`RAILS_API_KEY`**.

## What this project is / is not

**This project is**

- Small reference servers (TypeScript, Go, JVM, .NET) that call the public Rails HTTP API
- **Swagger UI** and **`/openapi.json`** for exploring routes against a host you already have access to
- **Local-dev friendly**: clone, configure env, run one process per language (no Docker Compose in this repo)

**This project is not**

- A way to run Postgres, users, accounts, ledger, or other Rails internals on your machine
- A hosted product or a substitute for credentials from Rails or your institution
- The [rails-core](https://github.com/railsinfra/rails-core) monorepo (see [Need the full Rails API locally?](#need-the-full-rails-api-locally) if you work on the full stack)

---

## Quick start

### 1. Clone

```bash
git clone https://github.com/railsinfra/rails-sdk-samples.git
cd rails-sdk-samples
```

Use your fork’s URL if you cloned from a fork.

### 2. Pick a language and open its folder

| Folder        | Stack                    | Detailed setup                          |
|---------------|--------------------------|-----------------------------------------|
| `typescript/` | Node / Express + OpenAPI | [typescript/README.md](typescript/README.md) |
| `kotlin/`     | JVM / Gradle             | [kotlin/README.md](kotlin/README.md)   |
| `java/`       | JVM / Gradle             | [java/README.md](java/README.md)         |
| `csharp/`     | .NET                     | [csharp/README.md](csharp/README.md)   |
| `go/`         | Go                       | [go/README.md](go/README.md)           |

```bash
cd typescript   # or go, java, kotlin, csharp
```

### 3. Install dependencies

First-time installs can take a while (`npm install`, Gradle wrapper downloads, `dotnet restore`, and so on).

| Language   | From the sample folder | Command (see language README for edge cases) |
|------------|------------------------|-----------------------------------------------|
| TypeScript | `typescript/`          | `npm install`                                 |
| Go         | `go/`                  | `go mod download`                             |
| Kotlin     | `kotlin/`              | `./gradlew --version`                         |
| Java       | `java/`                | `./gradlew --version`                         |
| C#         | `csharp/`              | `dotnet restore RailsSdkSample.sln`         |

Samples may use **published** SDK packages or **local path / composite**. Each language README explains prerequisites and how to repoint paths for a standalone clone of this repo only.

### 4. Run

| Language   | From the sample folder | Command                                      |
|------------|------------------------|----------------------------------------------|
| TypeScript | `typescript/`          | `npm run dev`                                |
| Go         | `go/`                  | `go run ./cmd/sample-api`                    |
| Kotlin     | `kotlin/`              | `./gradlew run`                              |
| Java       | `java/`                | `./gradlew run`                              |
| C#         | `csharp/`              | `dotnet run --project src/RailsSdkSample/RailsSdkSample.csproj` |

### 5. Open the UI

With the server running, use the URLs in [When the sample is running](#when-the-sample-is-running) (Swagger at `/`, OpenAPI at `/openapi.json` unless that sample’s README says otherwise).

---

## When the sample is running

| Language   | Folder        | Default `PORT` | Swagger UI                         | OpenAPI JSON                              |
|------------|---------------|----------------|-------------------------------------|-------------------------------------------|
| TypeScript | `typescript/` | 8081           | [http://localhost:8081/](http://localhost:8081/) | [http://localhost:8081/openapi.json](http://localhost:8081/openapi.json) |
| Kotlin     | `kotlin/`     | 8081           | [http://localhost:8081/](http://localhost:8081/) | [http://localhost:8081/openapi.json](http://localhost:8081/openapi.json) |
| Java       | `java/`       | 8081           | [http://localhost:8081/](http://localhost:8081/) | [http://localhost:8081/openapi.json](http://localhost:8081/openapi.json) |
| C#         | `csharp/`     | 8081           | [http://localhost:8081/](http://localhost:8081/) | [http://localhost:8081/openapi.json](http://localhost:8081/openapi.json) |
| Go         | `go/`         | 8083           | [http://localhost:8083/](http://localhost:8083/) | [http://localhost:8083/openapi.json](http://localhost:8083/openapi.json) |

Use a different port with **`PORT`** in `.env` (where supported) or your shell, as described in each sample README.

---

## Stop or restart the sample

| Situation | What to do |
|-----------|------------|
| Foreground server | **Ctrl+C** in the terminal where it is running. |
| Port already in use | Pick another **`PORT`** or stop the other process bound to that port. |
| Change env or code | Stop the process, edit `.env` or source, start the run command again (TypeScript `npm run dev` reloads on save). |

---

## Optional checks

Run these **from the sample folder** after dependencies are installed.

| Language   | Command | Notes |
|------------|---------|--------|
| TypeScript | `npm test` | Vitest |
| Go         | `go test ./...` | Includes package tests under `go/` |
| Java       | `./gradlew test` | JUnit |
| C#         | `dotnet test RailsSdkSample.sln` | xUnit |
| Kotlin     | — | No automated tests in this sample tree yet |

---

## Repository layout

```
rails-sdk-samples/
│
├── typescript/
├── go/
├── java/
├── kotlin/
├── csharp/
│
├── README.md
├── CONTRIBUTING.md
└── .gitignore
```

Each language folder contains its own **`README.md`**, dependency manifests, and (where used) **`.env.example`**.

---

## SDK dependencies

Samples may use **published** packages (npm, Maven Central, NuGet, Go modules) or **local path / composite** wiring to a checked-out SDK. What applies depends on the language; each sample README explains prerequisites and how to override paths when you do not use the default layout (for example a standalone clone of this repo without the `src/rails-sdks/` tree from rails-core).

---

## Optional: clone only one sample (sparse checkout)

You can clone this repository but **materialize a single language folder** in your working tree (Git still fetches repo metadata):

```bash
git clone --filter=blob:none --sparse https://github.com/railsinfra/rails-sdk-samples.git
cd rails-sdk-samples
git sparse-checkout set typescript
```

Replace `typescript` with `go`, `java`, `kotlin`, or `csharp` as needed. To add another folder later:

```bash
git sparse-checkout add go
```

Sparse checkouts follow the same SDK rules as a full clone; use the language README if you need only one folder but a different SDK source (published vs local).

---

## Credentials reference

| Variable              | Required | Description |
|-----------------------|----------|-------------|
| `RAILS_BASE_URL`      | Yes      | Use the value from **`.env.example`** for that sample; do not change unless Rails gave you a different base URL. |
| `RAILS_API_KEY`       | Yes      | Create at [railsinfra.com](https://railsinfra.com) after sign-in; set in `.env` (never commit real values). |
| `RAILS_X_ENVIRONMENT` | No       | `sandbox` or `production` (default `sandbox`). |
| `PORT`                | No       | Local server port (defaults in [When the sample is running](#when-the-sample-is-running)). |

---

## Troubleshooting (dev TLS)

If forwarded or proxy calls fail with **TLS / certificate / PKIX** errors against a dev host with a private CA, samples support a **dev-only** insecure flag (see each folder README). That does not relax TLS for all SDK paths unless noted.

---

## Need the full Rails API locally?

**Bank integrators should not follow this path.** For a complete local stack (gateway, Postgres-backed services, users, accounts, ledger, and related pieces), use your **organization’s internal platform documentation** or run the open-source stack in **[rails-core](https://github.com/railsinfra/rails-core)** (`README.md` there covers `make dev`, env, and URLs). That setup is outside this samples repository.

---

## Docs and contributing

- [typescript/README.md](typescript/README.md) — Node prerequisites, `file:` SDK override, run and TLS flags  
- [go/README.md](go/README.md) — Go `replace` directive, optional CLI smoke test  
- [java/README.md](java/README.md) — JDK, Gradle, TLS flags  
- [kotlin/README.md](kotlin/README.md) — JDK, Gradle, TLS flags  
- [csharp/README.md](csharp/README.md) — .NET SDK, tests, TLS flags  
- [CONTRIBUTING.md](CONTRIBUTING.md) — how to change samples and open a PR  
