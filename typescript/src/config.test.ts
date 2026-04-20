import { describe, expect, it } from 'vitest';
import { resolveRailsXEnvironment } from './config';

describe('resolveRailsXEnvironment', () => {
  it('always resolves to sandbox', () => {
    expect(resolveRailsXEnvironment({ headers: { 'x-environment': 'production' } }, 'sandbox')).toBe(
      'sandbox',
    );
    expect(resolveRailsXEnvironment({ headers: { 'x-environment': 'Sandbox' } }, 'sandbox')).toBe(
      'sandbox',
    );
    expect(resolveRailsXEnvironment({ headers: {} }, 'sandbox')).toBe('sandbox');
    expect(resolveRailsXEnvironment({ headers: { 'x-environment': 'staging' } }, 'sandbox')).toBe(
      'sandbox',
    );
  });
});
