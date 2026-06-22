import type { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import type { UserContext } from '../auth/userContext.js';

/**
 * Tool: `get_user_clubs`
 *
 * Returns the clubs currently enabled in the user's bag.
 * Call this at the start of a planning session to learn which clubs are
 * available. Use the `code` field (not `display_name`) in all subsequent
 * tool calls that accept a club reference.
 */
export function registerGetUserClubsTool(
  server: McpServer,
  ctx: UserContext,
): void {
  server.registerTool(
    'get_user_clubs',
    {
      description:
        "Returns the clubs currently enabled in the user's bag. Call this at the start of a planning session to learn which clubs are available. Use the `code` field (not `display_name`) in all subsequent tool calls that accept a club reference.",
      inputSchema: {},
    },
    async () => {
      const { data, error } = await ctx.supabaseClient
        .from('user_enabled_clubs')
        .select('club_code, clubs(code, display_name, category, sort_order)')
        .order('clubs(sort_order)', { ascending: true });

      if (error) {
        return {
          content: [
            {
              type: 'text' as const,
              text: JSON.stringify({
                code: 'DATABASE_ERROR',
                message: 'Failed to fetch user clubs.',
              }),
            },
          ],
          isError: true,
        };
      }

      const clubs = (data ?? []).map((row: Record<string, unknown>) => {
        const club = row.clubs as {
          code: string;
          display_name: string;
          category: string;
        };
        return {
          code: club.code,
          display_name: club.display_name,
          category: club.category,
        };
      });

      return {
        content: [
          {
            type: 'text' as const,
            text: JSON.stringify({ clubs }),
          },
        ],
      };
    },
  );
}
