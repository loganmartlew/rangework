import { describe, expect, it, vi, beforeEach } from 'vitest';

vi.mock('jose', () => {
  class JWTExpired extends Error {
    constructor(message: string) {
      super(message);
      this.name = 'JWTExpired';
    }
  }
  return {
    createRemoteJWKSet: vi.fn(() => vi.fn()),
    jwtVerify: vi.fn(),
    errors: { JWTExpired },
  };
});

import { jwtVerify, errors as JoseErrors } from 'jose';
import { validateToken } from '../auth/validateToken.js';

const MOCK_JWKS_URI = 'https://test.supabase.co/auth/v1/.well-known/jwks.json';
const MOCK_ISSUER = 'https://test.supabase.co/auth/v1';
const MOCK_TOKEN = 'mock.jwt.token';

function makeRequest(authHeader?: string): Request {
  const headers = new Headers();
  if (authHeader !== undefined) {
    headers.set('Authorization', authHeader);
  }
  return new Request('https://mcp.rangework.app/mcp', { method: 'POST', headers });
}

describe('validateToken', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('returns missing_token when Authorization header is absent', async () => {
    const result = await validateToken(makeRequest(), MOCK_JWKS_URI, MOCK_ISSUER);
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.error).toBe('missing_token');
  });

  it('returns missing_token when Authorization does not start with Bearer', async () => {
    const result = await validateToken(makeRequest('Basic abc123'), MOCK_JWKS_URI, MOCK_ISSUER);
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.error).toBe('missing_token');
  });

  it('returns missing_token when Bearer token is empty', async () => {
    const result = await validateToken(makeRequest('Bearer '), MOCK_JWKS_URI, MOCK_ISSUER);
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.error).toBe('missing_token');
  });

  it('returns valid payload for a good token', async () => {
    vi.mocked(jwtVerify).mockResolvedValueOnce({
      payload: { sub: 'user-123', iss: MOCK_ISSUER, exp: 9_999_999_999 },
      protectedHeader: { alg: 'ES256' },
    } as Awaited<ReturnType<typeof jwtVerify>>);

    const result = await validateToken(makeRequest(`Bearer ${MOCK_TOKEN}`), MOCK_JWKS_URI, MOCK_ISSUER);
    expect(result.ok).toBe(true);
    if (result.ok) {
      expect(result.payload.sub).toBe('user-123');
      expect(result.payload.iss).toBe(MOCK_ISSUER);
      expect(result.rawToken).toBe(MOCK_TOKEN);
    }
  });

  it('returns expired_token for an expired JWT', async () => {
    // Cast through unknown: TypeScript sees the real jose signature (3 args) but the
    // mock class only needs one. The cast is safe here — we control the mock.
    const expiredErr = new (JoseErrors.JWTExpired as unknown as new (msg: string) => Error)(
      'JWT expired',
    );
    vi.mocked(jwtVerify).mockRejectedValueOnce(expiredErr);

    const result = await validateToken(makeRequest(`Bearer ${MOCK_TOKEN}`), MOCK_JWKS_URI, MOCK_ISSUER);
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.error).toBe('expired_token');
  });

  it('returns invalid_token when signature verification fails', async () => {
    vi.mocked(jwtVerify).mockRejectedValueOnce(new Error('signature verification failed'));

    const result = await validateToken(makeRequest(`Bearer ${MOCK_TOKEN}`), MOCK_JWKS_URI, MOCK_ISSUER);
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.error).toBe('invalid_token');
  });

  it('returns invalid_token when sub claim is missing', async () => {
    vi.mocked(jwtVerify).mockResolvedValueOnce({
      payload: { iss: MOCK_ISSUER, exp: 9_999_999_999 },
      protectedHeader: { alg: 'ES256' },
    } as Awaited<ReturnType<typeof jwtVerify>>);

    const result = await validateToken(makeRequest(`Bearer ${MOCK_TOKEN}`), MOCK_JWKS_URI, MOCK_ISSUER);
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.error).toBe('invalid_token');
  });
});
