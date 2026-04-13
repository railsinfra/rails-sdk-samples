import { Agent, fetch as undiciFetch } from 'undici';

/**
 * Forwarded HTTP only — same idea as Kotlin's `java.net.http` client with optional trust-all TLS.
 * The Rails SDK client keeps default certificate verification unless you pass a custom `fetch` there too.
 */
export function createInsecureAwareProxyFetch(insecure: boolean): typeof fetch {
  if (!insecure) {
    return globalThis.fetch.bind(globalThis);
  }

  const agent = new Agent({ connect: { rejectUnauthorized: false } });
  const insecureFetch: typeof fetch = (input, init) =>
    undiciFetch(input as Parameters<typeof undiciFetch>[0], {
      ...(init as object),
      dispatcher: agent,
    } as Parameters<typeof undiciFetch>[1]) as unknown as Promise<Response>;
  return insecureFetch;
}

export function logTlsMode(insecure: boolean): void {
  // eslint-disable-next-line no-console
  console.error(
    `[rails-sdk-sample] Proxy fetch trust-all TLS: ${insecure ? 'ON' : 'OFF'} ` +
      '(set RAILS_INSECURE_SSL=true if PKIX / handshake errors persist on forwarded routes only)',
  );
}
