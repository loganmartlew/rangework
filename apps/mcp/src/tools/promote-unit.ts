import type { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import type { UserContext } from '../auth/userContext.js';
import { z } from 'zod';
import type { CallToolResult } from '@modelcontextprotocol/sdk/types.js';
import { toolError, ErrorCodes } from '../validation/tool-errors.js';

/**
 * Tool: `promote_unit`
 *
 * Detaches an Inline Unit from its owning session (`scoped_to_session_id →
 * null`), turning it into an ordinary library unit. Mirrors Stage 3's
 * `archive_session` mechanism: direct PostgREST update, owner-scoped by RLS,
 * re-read via `select` so the model can confirm the outcome. Content and
 * identity are unchanged — only the ownership pointer moves — and the
 * session that contained the unit keeps referencing the same id.
 */
export function registerPromoteUnitTool(
  server: McpServer,
  ctx: UserContext,
): void {
  server.registerTool(
    'promote_unit',
    {
      description:
        "Promote an inline unit to a reusable library unit: detach it from its owning session so it appears in `list_units` and can be reused in other sessions. The session that contained it keeps using it, unchanged. One-way — there is no demote. Only call this when the player asks to keep or reuse a specific drill (e.g. 'save that Tuesday drill', 'I want to reuse the gate drill'). Find the unit's `id` in the `items` of `list_sessions` where `inline` is true. Returns UNIT_NOT_FOUND if the unit does not exist or does not belong to you. Calling this on a unit that is already in the library is a harmless no-op.",
      inputSchema: {
        unit_id: z
          .string()
          .describe(
            "The `id` of an inline unit, found via a session's items in `list_sessions` (items where `inline` is true).",
          ),
      },
    },
    async (args): Promise<CallToolResult> => {
      const unitId = args.unit_id?.trim();
      if (!unitId) {
        return toolError(
          ErrorCodes.VALIDATION_ERROR,
          'unit_id must not be empty',
          { field: 'unit_id' },
        );
      }

      const { data: unit, error } = await ctx.supabaseClient
        .from('practice_units')
        .update({ scoped_to_session_id: null })
        .eq('id', unitId)
        .select('id, title, scoped_to_session_id')
        .maybeSingle();

      if (error) {
        // A malformed id (not a valid uuid) makes Postgres raise 22P02 rather
        // than matching no row. Treat it the same as a well-formed id that
        // matches nothing — a bad id never leaks a scary transport error.
        if (error.code === '22P02') {
          return toolError(
            ErrorCodes.UNIT_NOT_FOUND,
            'unit not found or does not belong to you',
          );
        }
        return toolError(
          ErrorCodes.DATABASE_ERROR,
          'Failed to promote unit.',
        );
      }

      if (!unit) {
        return toolError(
          ErrorCodes.UNIT_NOT_FOUND,
          'unit not found or does not belong to you',
        );
      }

      return {
        content: [
          {
            type: 'text' as const,
            text: JSON.stringify({
              unit: {
                id: unit.id,
                title: unit.title,
                inline: unit.scoped_to_session_id != null,
              },
            }),
          },
        ],
      };
    },
  );
}
