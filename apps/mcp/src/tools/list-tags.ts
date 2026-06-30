import type { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import type { UserContext } from '../auth/userContext.js';
import { fetchVisibleTags } from '../validation/tags.js';

/**
 * Tool: `list_tags`
 *
 * Returns the tags available to the user: the shared Default Tags plus the
 * user's own Custom Tags. Use the `code` field (not `display_name`) when
 * attaching tags in `create_unit` / `create_session` or filtering in
 * `list_units` / `list_sessions`. Parallel to `get_user_clubs`.
 */
export function registerListTagsTool(
  server: McpServer,
  ctx: UserContext,
): void {
  server.registerTool(
    'list_tags',
    {
      description:
        "Returns the tags available to the user: the shared Default Tags plus the user's own Custom Tags. Use the `code` field (not `display_name`) when attaching tags in `create_unit` / `create_session` or filtering in `list_units` / `list_sessions`.",
      inputSchema: {},
    },
    async () => {
      let tags;
      try {
        tags = await fetchVisibleTags(ctx.supabaseClient);
      } catch {
        return {
          content: [
            {
              type: 'text' as const,
              text: JSON.stringify({
                code: 'DATABASE_ERROR',
                message: 'Failed to fetch tags.',
              }),
            },
          ],
          isError: true,
        };
      }

      return {
        content: [
          {
            type: 'text' as const,
            text: JSON.stringify({
              tags: tags.map(t => ({
                code: t.code,
                display_name: t.display_name,
                is_default: t.owner_id === null,
              })),
            }),
          },
        ],
      };
    },
  );
}
