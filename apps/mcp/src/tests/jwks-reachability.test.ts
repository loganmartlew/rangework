import { describe, expect, it } from 'vitest';

/**
 * Integration test: confirm the Worker runtime can reach the Supabase JWKS URL.
 *
 * This is NOT token validation (RWK-30 scope) — it only confirms the Worker's
 * runtime can fetch the JWKS endpoint and that RWK-28's JWKS configuration is live.
 *
 * The Supabase project URL is provided via the SUPABASE_URL environment variable.
 * If not set, the test is skipped.
 */
describe('JWKS reachability', () => {
  it('can fetch the Supabase JWKS endpoint', async () => {
    const supabaseUrl = process.env.SUPABASE_URL;
    if (!supabaseUrl) {
      console.warn('SUPABASE_URL not set — skipping JWKS reachability test');
      return;
    }

    // Construct the JWKS URI from the Supabase project URL
    // Format: https://<project-ref>.supabase.co/.well-known/jwks.json
    const jwksUrl = new URL('/.well-known/jwks.json', supabaseUrl);

    const response = await fetch(jwksUrl, {
      method: 'GET',
      headers: { accept: 'application/json' },
    });

    expect(response.ok).toBe(true);
    expect(response.headers.get('content-type')).toContain('application/json');

    const jwks = (await response.json()) as { keys: Array<{ alg: string }> };
    expect(jwks).toHaveProperty('keys');
    expect(Array.isArray(jwks.keys)).toBe(true);
    expect(jwks.keys.length).toBeGreaterThan(0);

    // Verify at least one key is ES256
    const es256Key = jwks.keys.find(key => key.alg === 'ES256');
    expect(es256Key).toBeDefined();
  });
});
