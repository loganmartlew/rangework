import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { registerPingTool } from './tools/ping.js';
import type { UserContext } from './auth/userContext.js';

/**
 * Build and configure the MCP server instance.
 *
 * Exported separately from the Workers entrypoint so unit tests can import
 * the server and call `tools/list` and `tools/call` without spinning up the
 * Workers runtime.
 *
 * @param userContext - Authenticated user context (available from RWK-30 onwards).
 *   Undefined only in unauthenticated test scenarios. All production requests
 *   reach here with a validated context (auth is enforced in the fetch handler).
 */
export function createServer(_userContext?: UserContext): McpServer {
  const server = new McpServer(
    {
      name: 'rangework-mcp',
      version: '0.0.1',
    },
    {
      capabilities: {
        tools: {},
      },
    },
  );

  registerPingTool(server);

  return server;
}
