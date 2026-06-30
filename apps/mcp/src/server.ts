import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { registerPingTool } from './tools/ping.js';
import { registerGetUserClubsTool } from './tools/get-user-clubs.js';
import { registerListUnitsTool } from './tools/list-units.js';
import { registerListSessionsTool } from './tools/list-sessions.js';
import { registerCreateUnitTool } from './tools/create-unit.js';
import { registerCreateSessionTool } from './tools/create-session.js';
import { registerListTagsTool } from './tools/list-tags.js';
import { registerCreateTagTool } from './tools/create-tag.js';
import { registerGetCoachingGuideTool } from './tools/get-coaching-guide.js';
import { registerBuildPracticePlanPrompt } from './prompts/build-practice-plan.js';
import type { UserContext } from './auth/userContext.js';

/**
 * Build and configure the MCP server instance.
 *
 * Exported separately from the Workers entrypoint so unit tests can import
 * the server and call `tools/list` and `tools/call` without spinning up the
 * Workers runtime.
 *
 * @param userContext - Authenticated user context (available from RWK-30 onwards).
 *   All production requests reach here with a validated context (auth is enforced
 *   in the fetch handler).
 * @param bucket - R2 bucket containing the coaching methodology.
 */
export function createServer(
  userContext: UserContext,
  bucket: R2Bucket,
): McpServer {
  const server = new McpServer(
    {
      name: 'rangework-mcp',
      version: '0.0.1',
    },
    {
      capabilities: {
        tools: {},
        prompts: {},
      },
    },
  );

  registerPingTool(server);
  registerGetUserClubsTool(server, userContext);
  registerListUnitsTool(server, userContext);
  registerListSessionsTool(server, userContext);
  registerCreateUnitTool(server, userContext);
  registerCreateSessionTool(server, userContext);
  registerListTagsTool(server, userContext);
  registerCreateTagTool(server, userContext);
  registerGetCoachingGuideTool(server, bucket);
  registerBuildPracticePlanPrompt(server, bucket);

  return server;
}
