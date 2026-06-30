import type { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import type { UserContext } from '../auth/userContext.js';
import { z } from 'zod';
import type { CallToolResult } from '@modelcontextprotocol/sdk/types.js';
import { toolError, ErrorCodes } from '../validation/tool-errors.js';
import { slugifyTag } from '../validation/tags.js';

/**
 * Tool: `create_tag`
 *
 * Mints a new Custom Tag (or returns the matching existing tag) by name. This
 * is the *only* way to create a tag — `create_unit` / `create_session` reject
 * unknown codes rather than creating tags as a side effect.
 *
 * Strongly prefer attaching an existing tag from `list_tags`. Only call this
 * when nothing in the shared vocabulary reasonably fits; a typed name that
 * matches an existing tag (ignoring case and spacing) resolves to that tag
 * instead of duplicating it.
 */
export function registerCreateTagTool(
  server: McpServer,
  ctx: UserContext,
): void {
  server.registerTool(
    'create_tag',
    {
      description:
        "Mints a new Custom Tag (or returns the matching existing tag) by name. The only way to create a tag — create_unit/create_session reject unknown codes. Strongly prefer an existing tag from list_tags; only create one when nothing reasonably fits. A name that matches an existing tag (ignoring case and spacing) resolves to that tag instead of duplicating it.",
      inputSchema: {
        name: z
          .string()
          .describe(
            'Display name for the tag (e.g. "Lag putting"). Slugged to a stable code; a name matching an existing tag reuses it.',
          ),
      },
    },
    async (args): Promise<CallToolResult> => {
      const name = args.name.trim();
      const code = slugifyTag(name);
      if (!code) {
        return toolError(
          ErrorCodes.VALIDATION_ERROR,
          'tag name must contain at least one letter or number',
          { field: 'name' },
        );
      }

      const { data: tagId, error } = await ctx.supabaseClient.rpc(
        'create_or_get_tag',
        { p_code: code, p_name: name },
      );

      if (error || !tagId) {
        return toolError(
          ErrorCodes.DATABASE_ERROR,
          'Failed to create tag. Please try again.',
        );
      }

      // Re-read the resolved tag so we return its canonical code + display name.
      const { data: tag, error: fetchError } = await ctx.supabaseClient
        .from('tags')
        .select('code, display_name, owner_id')
        .eq('id', tagId)
        .single();

      if (fetchError || !tag) {
        return toolError(
          ErrorCodes.DATABASE_ERROR,
          'Tag was created but could not be loaded.',
        );
      }

      return {
        content: [
          {
            type: 'text' as const,
            text: JSON.stringify({
              tag: {
                code: tag.code,
                display_name: tag.display_name,
                is_default: tag.owner_id === null,
              },
            }),
          },
        ],
      };
    },
  );
}
