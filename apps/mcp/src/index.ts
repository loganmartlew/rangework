import { StreamableHTTPServerTransport } from '@modelcontextprotocol/sdk/server/streamableHttp.js';
import { createServer } from './server.js';
import { validateToken } from './auth/validateToken.js';
import { createUserContext } from './auth/userContext.js';

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
export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    const resourceBaseUrl = `${url.protocol}//${url.host}`;
    const metadataUrl = `${resourceBaseUrl}/.well-known/oauth-protected-resource`;

    // Health-check endpoint (unauthenticated)
    if (url.pathname === '/health' && request.method === 'GET') {
      return new Response(JSON.stringify({ status: 'ok' }), {
        headers: { 'content-type': 'application/json' },
      });
    }

    // OAuth 2.0 Protected Resource Metadata (RFC 9728)
    // MCP clients use this to discover the authorization server.
    if (
      url.pathname === '/.well-known/oauth-protected-resource' &&
      request.method === 'GET'
    ) {
      const metadata = {
        resource: resourceBaseUrl,
        authorization_servers: [`${env.SUPABASE_URL}/auth/v1`],
        bearer_methods_supported: ['header'],
        resource_signing_alg_values_supported: ['ES256'],
      };
      return new Response(JSON.stringify(metadata), {
        headers: { 'content-type': 'application/json' },
      });
    }

    // MCP Streamable HTTP endpoint (authenticated)
    if (url.pathname === '/mcp') {
      if (request.method !== 'POST') {
        return new Response('Method not allowed', { status: 405 });
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
          headers: { 'content-type': 'application/json' },
        });
      }

      // Use the web-standard transport's handleRequest method
      const webStandardTransport = (
        transport as unknown as {
          _webStandardTransport: {
            handleRequest: (
              req: Request,
              opts: { parsedBody?: unknown },
            ) => Promise<Response>;
          };
        }
      )._webStandardTransport;
      const response = await webStandardTransport.handleRequest(request, {
        parsedBody,
      });

      return response as Response;
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
          headers: { 'content-type': 'application/json' },
        },
      );
    }

    return new Response('Not found', { status: 404 });
  },
} satisfies ExportedHandler<Env>;
