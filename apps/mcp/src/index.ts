import { StreamableHTTPServerTransport } from '@modelcontextprotocol/sdk/server/streamableHttp.js';
import { createServer } from './server.js';
import { validateToken } from './auth/validateToken.js';
import { createUserContext } from './auth/userContext.js';
import { toIncomingMessage, createResponseShim } from './transport-shim.js';

export interface Env {
  SUPABASE_URL: string;
  SUPABASE_ANON_KEY: string;
  METHODOLOGY_BUCKET: R2Bucket;
}

/**
 * Cloudflare Workers fetch handler.
 *
 * Mounts the MCP Streamable HTTP transport at `/mcp`. All POST requests to
 * that path require a valid Supabase-issued Bearer JWT (RWK-30). Other
 * methods/paths receive appropriate HTTP status codes.
 */
function corsHeaders(request: Request): Record<string, string> {
  return {
    'Access-Control-Allow-Origin': request.headers.get('Origin') ?? '*',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers':
      'Content-Type, Authorization, MCP-Protocol-Version, mcp-protocol-version',
    'Access-Control-Expose-Headers': 'mcp-session-id, WWW-Authenticate',
    'Access-Control-Max-Age': '86400',
  };
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    const resourceBaseUrl = `${url.protocol}//${url.host}`;
    const metadataUrl = `${resourceBaseUrl}/.well-known/oauth-protected-resource`;

    // CORS preflight
    if (request.method === 'OPTIONS') {
      return new Response(null, { status: 204, headers: corsHeaders(request) });
    }

    // Health-check endpoint (unauthenticated)
    if (url.pathname === '/health' && request.method === 'GET') {
      return new Response(JSON.stringify({ status: 'ok' }), {
        headers: { 'content-type': 'application/json', ...corsHeaders(request) },
      });
    }

    // OAuth 2.0 Protected Resource Metadata (RFC 9728)
    // MCP clients use this to discover the authorization server.
    if (
      url.pathname === '/.well-known/oauth-protected-resource' &&
      request.method === 'GET'
    ) {
      const metadata = {
        resource: `${resourceBaseUrl}/mcp`,
        authorization_servers: [`${env.SUPABASE_URL}/auth/v1`],
        bearer_methods_supported: ['header'],
        resource_signing_alg_values_supported: ['ES256', 'RS256'],
      };
      return new Response(JSON.stringify(metadata), {
        headers: { 'content-type': 'application/json', ...corsHeaders(request) },
      });
    }

    // MCP Streamable HTTP endpoint (authenticated)
    if (url.pathname === '/mcp') {
      if (request.method !== 'POST') {
        return new Response('Method not allowed', {
          status: 405,
          headers: corsHeaders(request),
        });
      }

      const jwksUri = `${env.SUPABASE_URL}/auth/v1/.well-known/jwks.json`;
      const issuer = `${env.SUPABASE_URL}/auth/v1`;

      const authResult = await validateToken(request, jwksUri, issuer);

      if (!authResult.ok) {
        const isTokenError =
          authResult.error === 'invalid_token' ||
          authResult.error === 'expired_token';

        let wwwAuthenticate = `Bearer realm="rangework-mcp", resource_metadata="${metadataUrl}"`;
        if (isTokenError) {
          wwwAuthenticate += `, error="invalid_token", error_description="${authResult.message}"`;
        }

        return new Response(JSON.stringify({ error: authResult.message }), {
          status: 401,
          headers: {
            'content-type': 'application/json',
            'WWW-Authenticate': wwwAuthenticate,
            ...corsHeaders(request),
          },
        });
      }

      const userContext = createUserContext(
        authResult.payload.sub,
        authResult.rawToken,
        env.SUPABASE_URL,
        env.SUPABASE_ANON_KEY,
      );

      const server = createServer(userContext, env.METHODOLOGY_BUCKET);
      const transport = new StreamableHTTPServerTransport({
        sessionIdGenerator: undefined,
      });

      await server.connect(transport);

      let parsedBody: unknown;
      try {
        const body = await request.text();
        parsedBody = body ? JSON.parse(body) : undefined;
      } catch {
        return new Response(JSON.stringify({ error: 'Invalid JSON body' }), {
          status: 400,
          headers: { 'content-type': 'application/json', ...corsHeaders(request) },
        });
      }

      const req = toIncomingMessage(request);
      const { shim: res, response: responsePromise } = createResponseShim();

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      await transport.handleRequest(req as any, res, parsedBody);

      const response = await responsePromise;
      const mcpResponse = new Response(response.body, response);
      for (const [key, value] of Object.entries(corsHeaders(request))) {
        mcpResponse.headers.set(key, value);
      }
      return mcpResponse;
    }

    // Root path — info
    if (url.pathname === '/') {
      return new Response(
        JSON.stringify({
          name: 'rangework-mcp',
          version: '0.0.1',
          mcp_endpoint: '/mcp',
        }),
        {
          headers: { 'content-type': 'application/json', ...corsHeaders(request) },
        },
      );
    }

    return new Response('Not found', { status: 404, headers: corsHeaders(request) });
  },
} satisfies ExportedHandler<Env>;
