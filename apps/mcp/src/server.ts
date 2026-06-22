import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { registerPingTool } from './tools/ping.js';

/**
 * Build and configure the MCP server instance.
 *
 * Exported separately from the Workers entrypoint so unit tests can import
 * the server and call `tools/list` and `tools/call` without spinning up the
 * Workers runtime.
 */
export function createServer(): McpServer {
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
