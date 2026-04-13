import { describe, expect, it } from 'vitest';
import { HttpError, isTlsOrCertError, toErrorBody } from './errors';

describe('isTlsOrCertError', () => {
  it('detects OpenSSL-style codes', () => {
    const err = Object.assign(new Error('x'), { code: 'UNABLE_TO_VERIFY_LEAF_SIGNATURE' });
    expect(isTlsOrCertError(err)).toBe(true);
  });

  it('detects PKIX / handshake wording', () => {
    expect(isTlsOrCertError(new Error('PKIX path building failed'))).toBe(true);
    expect(isTlsOrCertError(new Error('SSL handshake failure'))).toBe(true);
  });

  it('returns false for unrelated errors', () => {
    expect(isTlsOrCertError(new Error('ENOTFOUND'))).toBe(false);
  });
});

describe('toErrorBody', () => {
  it('maps HttpError', () => {
    expect(toErrorBody(new HttpError(400, 'bad'), '/x')).toEqual({
      status: 400,
      message: 'bad',
      exception: 'HttpError',
      path: '/x',
    });
  });

  it('maps TLS errors to 502 with guidance', () => {
    const b = toErrorBody(new Error('certificate has expired'), '/proxy');
    expect(b.status).toBe(502);
    expect(b.message).toContain('RAILS_INSECURE_SSL');
  });
});
