import { describe, expect, it } from 'vitest';
import { resolveRailsXEnvironment } from './config';

describe('resolveRailsXEnvironment', () => {
  it('uses sandbox or production from the request when valid', () => {
    expect(
      resolveRailsXEnvironment({ headers: { 'x-environment': 'production' } }, 'sandbox'),
    ).toBe('production');
    expect(
      resolveRailsXEnvironment({ headers: { 'x-environment': 'Sandbox' } }, 'production'),
    ).toBe('sandbox');
  });

  it('falls back when header missing or invalid', () => {
    expect(resolveRailsXEnvironment({ headers: {} }, 'production')).toBe('production');
    expect(resolveRailsXEnvironment({ headers: { 'x-environment': 'staging' } }, 'sandbox')).toBe(
      'sandbox',
    );
  });
});
