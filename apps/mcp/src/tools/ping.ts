import type { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';

/**
 * The `ping` tool — a minimal health-check tool that returns `{ status: "ok" }`.
 *
 * Stage 1: unauthenticated. Auth enforcement is deferred to RWK-30.
 */
export function registerPingTool(server: McpServer): void {
  server.registerTool(
    'ping',
    {
      description:
        'Health-check tool. Returns { status: "ok" } when the server is reachable.',
      inputSchema: {},
    },
    async () => {
      return {
        content: [
          {
            type: 'text',
            text: JSON.stringify({ status: 'ok' }),
          },
        ],
      };
    },
  );
}
