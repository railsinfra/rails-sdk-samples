/** Uniform JSON error body for handlers + global middleware (aligned with Kotlin `ErrorResponse`). */
export interface ErrorBody {
  status: number;
  message: string;
  exception?: string;
  path?: string;
}

export class HttpError extends Error {
  readonly status: number;
  readonly code?: string;

  constructor(status: number, message: string, code?: string) {
    super(message);
    this.name = 'HttpError';
    this.status = status;
    this.code = code;
  }
}

export function isTlsOrCertError(err: unknown): boolean {
  const e = err as NodeJS.ErrnoException | undefined;
  const code = e?.code;
  if (
    code === 'UNABLE_TO_VERIFY_LEAF_SIGNATURE' ||
    code === 'CERT_HAS_EXPIRED' ||
    code === 'DEPTH_ZERO_SELF_SIGNED_CERT' ||
    code === 'SELF_SIGNED_CERT_IN_CHAIN' ||
    code === 'ERR_TLS_CERT_ALTNAME_INVALID'
  ) {
    return true;
  }
  const msg = err instanceof Error ? err.message : String(err);
  return /UNABLE_TO_VERIFY_LEAF_SIGNATURE|certificate|SSL|TLS|handshake|PKIX|cert\.verify/i.test(msg);
}

/** Stainless `APIError` (and subclasses) carry an HTTP status on the error instance. */
function isSdkApiError(err: unknown): err is Error & { status: number } {
  return err instanceof Error && typeof (err as { status?: unknown }).status === 'number';
}

export function toErrorBody(err: unknown, path: string): ErrorBody {
  if (err instanceof HttpError) {
    return {
      status: err.status,
      message: err.message,
      exception: err.code ?? err.name,
      path,
    };
  }

  if (isSdkApiError(err)) {
    return {
      status: err.status,
      message: err.message,
      exception: err.constructor?.name ?? 'APIError',
      path,
    };
  }

  if (isTlsOrCertError(err)) {
    return {
      status: 502,
      message:
        'TLS handshake or certificate verification failed when calling the upstream API. For local dev against a private CA, set RAILS_INSECURE_SSL=true (proxy calls only; SDK routes still use strict TLS).',
      exception: err instanceof Error ? err.name : 'TlsError',
      path,
    };
  }

  return {
    status: 500,
    message: err instanceof Error ? err.message : String(err),
    exception: err instanceof Error ? err.name : 'Error',
    path,
  };
}
