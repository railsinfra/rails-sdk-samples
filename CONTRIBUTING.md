# Contributing to rails-sdk-samples

Thanks for helping improve the reference apps. This document is intentionally short: get oriented, run checks locally, open a PR.

## Run locally

1. Clone the repository and `cd rails-sdk-samples`.
2. Choose a language folder (`typescript/`, `go/`, `java/`, `kotlin/`, or `csharp/`).
3. Set credentials as in the root [README.md](README.md): **`RAILS_BASE_URL`** from `.env.example` (unless onboarding says otherwise), **`RAILS_API_KEY`** from https://railsinfra.com after sign-in, plus optional **`PORT`** / **`RAILS_X_ENVIRONMENT`** per that sample’s README.
4. Install dependencies and start the sample using the commands in the language README.

See the root [README.md](README.md) for a numbered quick start, default ports, and links to each sample.

## What belongs in each sample

- Keep samples **small**: one HTTP app, Swagger + OpenAPI, enough routes to demonstrate the official SDK.
- **Per-language details** (prerequisites, local SDK path overrides such as npm `file:`, Go `replace`, Gradle project references, TLS dev flags) stay in that folder’s **README.md** unless the change applies to every language.

## Run tests

From the **sample folder** (after dependencies are installed):

| Sample     | Command |
|------------|---------|
| TypeScript | `npm test` |
| Go         | `go test ./...` |
| Java       | `./gradlew test` |
| C#         | `dotnet test RailsSdkSample.sln` |
| Kotlin     | No test task in-tree yet |

## Pull requests

- **Base branch:** open PRs against **`develop`** unless a maintainer asks otherwise.
- Keep the change focused on one concern when possible.
- Describe **what** changed and **why** in the PR body (plain language is enough).
- Ensure CI is green before requesting review when CI is enabled.

## Commit messages

Prefer **[Conventional Commits](https://www.conventionalcommits.org/)** (`feat`, `fix`, `docs`, `chore`, and so on) so history stays readable.

If something is unclear, open a draft PR or issue and iterate in the thread.
