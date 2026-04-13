import 'dotenv/config';

function normalizeBaseUrl(raw: string): string {
  const t = raw.trim().replace(/\/$/, '');
  if (!t) return 'https://api.railsinfra.com';
  if (/^https?:\/\//i.test(t)) return t;
  return `https://${t}`;
}

/** Value for `X-Environment` on Rails account API calls (required by the backend). */
export type RailsXEnvironment = 'sandbox' | 'production';

export function defaultRailsXEnvironmentFromEnv(): RailsXEnvironment {
  const raw = (process.env.RAILS_X_ENVIRONMENT ?? 'sandbox').trim().toLowerCase();
  return raw === 'production' ? 'production' : 'sandbox';
}

/**
 * Prefer the incoming request header (Swagger UI), else the configured default.
 * Forwarded/proxy routes already set `X-Environment`; SDK routes must add it explicitly.
 */
export function resolveRailsXEnvironment(
  req: { headers: Record<string, string | string[] | undefined> },
  fallback: RailsXEnvironment,
): RailsXEnvironment {
  const raw = req.headers['x-environment'];
  const first = Array.isArray(raw) ? raw[0] : raw;
  const v = typeof first === 'string' ? first.trim().toLowerCase() : '';
  if (v === 'production') return 'production';
  if (v === 'sandbox') return 'sandbox';
  return fallback;
}

export function loadConfig() {
  const baseURL = normalizeBaseUrl(process.env.RAILS_BASE_URL ?? 'https://api.railsinfra.com');
  const apiKey = process.env.RAILS_API_KEY ?? '';
  const port = Number.parseInt(process.env.PORT ?? '8081', 10);
  const insecureProxyTls =
    process.env.RAILS_INSECURE_SSL?.toLowerCase() === 'true' ||
    process.env.RAILS_INSECURE_SSL === '1';
  const railsXEnvironment = defaultRailsXEnvironmentFromEnv();

  return {
    baseURL,
    apiKey,
    port: Number.isFinite(port) && port > 0 ? port : 8081,
    insecureProxyTls,
    railsXEnvironment,
  };
}
