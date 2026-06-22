import { StreamableHTTPServerTransport } from '@modelcontextprotocol/sdk/server/streamableHttp.js';
import { createServer } from './server.js';

/**
 * Cloudflare Workers fetch handler.
 *
 * Mounts the MCP Streamable HTTP transport at `/mcp`. All POST requests to
 * that path are forwarded to the MCP SDK transport; other methods/paths
 * receive appropriate HTTP status codes.
 */
export default {
  async fetch(request: Request): Promise<Response> {
    const url = new URL(request.url);

    // Health-check endpoint (non-MCP)
    if (url.pathname === '/health' && request.method === 'GET') {
      return new Response(JSON.stringify({ status: 'ok' }), {
        headers: { 'content-type': 'application/json' },
      });
    }

    // MCP Streamable HTTP endpoint
    if (url.pathname === '/mcp') {
      if (request.method !== 'POST') {
        return new Response('Method not allowed', { status: 405 });
      }

      const server = createServer();
      const transport = new StreamableHTTPServerTransport({
        sessionIdGenerator: undefined,
      });

      await server.connect(transport);

      const body = await request.text();
      const parsedBody = body ? JSON.parse(body) : undefined;

      // Use the web-standard transport's handleRequest method
      const webStandardTransport = (
        transport as {
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

    // Root path — redirect to /mcp or return info
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
};
