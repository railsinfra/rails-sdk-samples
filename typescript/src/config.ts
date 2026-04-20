import 'dotenv/config';

function normalizeBaseUrl(raw: string): string {
  const t = raw.trim().replace(/\/$/, '');
  if (!t) return 'https://api.railsinfra.com';
  if (/^https?:\/\//i.test(t)) return t;
  return `https://${t}`;
}

/** Sandboxed Rails API calls in this sample (always `sandbox`). */
export type RailsXEnvironment = 'sandbox';

/**
 * This sample always uses the sandbox environment for account API calls.
 * Forwarded/proxy routes set `X-Environment: sandbox`; SDK routes use the same.
 */
export function resolveRailsXEnvironment(
  _req: { headers: Record<string, string | string[] | undefined> },
  _fallback: RailsXEnvironment,
): RailsXEnvironment {
  return 'sandbox';
}

export function loadConfig() {
  const baseURL = normalizeBaseUrl(process.env.RAILS_BASE_URL ?? 'https://api.railsinfra.com');
  const apiKey = process.env.RAILS_API_KEY ?? '';
  const port = Number.parseInt(process.env.PORT ?? '8081', 10);
  const insecureProxyTls =
    process.env.RAILS_INSECURE_SSL?.toLowerCase() === 'true' ||
    process.env.RAILS_INSECURE_SSL === '1';

  return {
    baseURL,
    apiKey,
    port: Number.isFinite(port) && port > 0 ? port : 8081,
    insecureProxyTls,
  };
}
