import type { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import type { R2Bucket } from '@cloudflare/workers-types';
import type { CallToolResult } from '@modelcontextprotocol/sdk/types.js';
import { loadMethodology } from '../methodology/loader.js';
import { toolError, ErrorCodes } from '../validation/tool-errors.js';

/**
 * Tool: `get_coaching_guide`
 *
 * Fallback tool for clients that don't support MCP prompts. Returns the full
 * coaching methodology text alongside the methodology version string.
 */
export function registerGetCoachingGuideTool(
  server: McpServer,
  bucket: R2Bucket,
): void {
  server.registerTool(
    'get_coaching_guide',
    {
      description:
        'Returns the Rangework coaching guide — a methodology for planning golf practice sessions. Call this to learn how to structure a practice plan, then use `get_user_clubs`, `create_unit`, and `create_session` to build one. The guide includes step-by-step instructions for the tool-call sequence.',
      inputSchema: {},
    },
    async (): Promise<CallToolResult> => {
      const methodology = await loadMethodology(bucket);

      if (!methodology) {
        return toolError(
          ErrorCodes.CONTENT_UNAVAILABLE,
          'Coaching guide is temporarily unavailable. Please try again.',
        );
      }

      // Extract methodology_version from the preamble
      const versionMatch = methodology.match(
        /methodology_version:\s*"?([^"\n]+)"?/,
      );
      const version = versionMatch?.[1]?.trim() ?? 'unknown';

      return {
        content: [
          {
            type: 'text',
            text: JSON.stringify({
              methodology_version: version,
              guide: methodology,
            }),
          },
        ],
      };
    },
  );
}
