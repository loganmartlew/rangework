import { createRemoteJWKSet, jwtVerify, errors as JoseErrors } from 'jose';

export interface ValidatedPayload {
  sub: string;
  iss: string;
  exp: number;
}

export type AuthResult =
  | { ok: true; payload: ValidatedPayload; rawToken: string }
  | { ok: false; error: 'missing_token' | 'invalid_token' | 'expired_token'; message: string };

// Module-level JWKS cache — persists across requests in the same Worker isolate.
// Keyed by URI so a config change gets a fresh instance.
let jwksCache: ReturnType<typeof createRemoteJWKSet> | null = null;
let cachedJwksUri: string | null = null;

function getJWKS(jwksUri: string): ReturnType<typeof createRemoteJWKSet> {
  if (!jwksCache || cachedJwksUri !== jwksUri) {
    jwksCache = createRemoteJWKSet(new URL(jwksUri));
    cachedJwksUri = jwksUri;
  }
  return jwksCache;
}

export async function validateToken(
  request: Request,
  jwksUri: string,
  issuer: string,
): Promise<AuthResult> {
  const authHeader = request.headers.get('Authorization');

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return {
      ok: false,
      error: 'missing_token',
      message: 'Missing or malformed Authorization header',
    };
  }

  const token = authHeader.slice(7).trim();
  if (!token) {
    return {
      ok: false,
      error: 'missing_token',
      message: 'Empty token in Authorization header',
    };
  }

  const jwks = getJWKS(jwksUri);

  try {
    const { payload } = await jwtVerify(token, jwks, {
      issuer,
      algorithms: ['ES256'],
    });

    if (typeof payload.sub !== 'string' || !payload.sub) {
      return { ok: false, error: 'invalid_token', message: 'Token missing sub claim' };
    }

    return {
      ok: true,
      payload: {
        sub: payload.sub,
        iss: payload.iss ?? '',
        exp: typeof payload.exp === 'number' ? payload.exp : 0,
      },
      rawToken: token,
    };
  } catch (e) {
    if (e instanceof JoseErrors.JWTExpired) {
      return { ok: false, error: 'expired_token', message: 'Token has expired' };
    }
    return { ok: false, error: 'invalid_token', message: 'Token validation failed' };
  }
}
