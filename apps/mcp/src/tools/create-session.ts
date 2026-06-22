import type { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import type { UserContext } from '../auth/userContext.js';
import { z } from 'zod';
import type { CallToolResult } from '@modelcontextprotocol/sdk/types.js';
import { toolError, ErrorCodes } from '../validation/tool-errors.js';
import {
  fetchAllClubCodes,
  validateClubCode,
} from '../validation/club-codes.js';

/**
 * Tool: `create_session`
 *
 * Creates a new practice session in the user's account. A session is an ordered
 * list of practice units with optional per-item club overrides, coaching cues,
 * and repeat counts. Call `list_units` or `create_unit` first to get unit ids.
 * Returns the new session's id. Each item's `practice_unit_id` must be a unit
 * that belongs to this user.
 */
export function registerCreateSessionTool(
  server: McpServer,
  ctx: UserContext,
): void {
  server.registerTool(
    'create_session',
    {
      description:
        "Creates a new practice session in the user's account. A session is an ordered list of practice units with optional per-item club overrides, coaching cues, and repeat counts. Call `list_units` or `create_unit` first to get unit ids. Returns the new session's id. Each item's `practice_unit_id` must be a unit that belongs to this user.",
      inputSchema: {
        name: z
          .string()
          .describe(
            'Short name for the session (e.g. "Pre-round warm-up", "Wedge Wednesday").',
          ),
        items: z
          .array(
            z.object({
              practice_unit_id: z
                .string()
                .describe(
                  'The `id` of a practice unit returned by `list_units` or `create_unit`.',
                ),
              order: z
                .number()
                .describe(
                  'Item number, starting at 1. Must be a positive integer.',
                ),
              repeat_count: z
                .number()
                .describe(
                  'How many times to run this unit in the session (e.g. 2 = two rounds of this drill). Must be a positive integer.',
                ),
              club_reference: z
                .string()
                .optional()
                .describe(
                  "Optional club override for this item. Overrides the unit's default club. Use a `code` from `get_user_clubs`.",
                ),
              focus_cue: z
                .string()
                .optional()
                .describe(
                  'Optional per-item coaching cue (e.g. "Hinge earlier").',
                ),
              notes: z
                .string()
                .optional()
                .describe(
                  'Optional per-item reminder (e.g. "Use the 50y stake").',
                ),
            }),
          )
          .describe(
            'Ordered list of practice units. Each item needs `practice_unit_id`, `order` (starting at 1), `repeat_count`, and optionally a `club_reference`, `notes`, and `focus_cue`. Must have at least 1 item.',
          ),
        notes: z
          .string()
          .optional()
          .describe(
            'Optional session-level notes (e.g. "Tournament prep — focus on short game").',
          ),
      },
    },
    async (args): Promise<CallToolResult> => {
      // Validate items array length
      if (!args.items || args.items.length === 0) {
        return toolError(
          ErrorCodes.VALIDATION_ERROR,
          'at least one item is required',
          {
            field: 'items',
          },
        );
      }

      // Trim and validate name
      const name = args.name.trim();
      if (!name) {
        return toolError(
          ErrorCodes.VALIDATION_ERROR,
          'name must not be empty',
          {
            field: 'name',
          },
        );
      }

      // Check for duplicate order values
      const orderValues = args.items.map(i => i.order);
      const uniqueOrders = new Set(orderValues);
      if (uniqueOrders.size !== orderValues.length) {
        return toolError(
          ErrorCodes.VALIDATION_ERROR,
          'item order values must be unique',
          { field: 'items' },
        );
      }

      // Pre-fetch the user's unit ids
      const { data: ownedUnits, error: unitsError } = await ctx.supabaseClient
        .from('practice_units')
        .select('id');

      if (unitsError) {
        return toolError(
          ErrorCodes.DATABASE_ERROR,
          'Failed to fetch user units.',
        );
      }

      const ownedUnitIds = new Set((ownedUnits ?? []).map(u => u.id));

      // Validate all practice_unit_ids
      const invalidUnitIds = args.items
        .map(i => i.practice_unit_id)
        .filter(id => !ownedUnitIds.has(id));

      if (invalidUnitIds.length > 0) {
        return toolError(
          ErrorCodes.UNIT_NOT_FOUND,
          `unit ${invalidUnitIds[0]} not found or does not belong to you`,
          { invalid_unit_ids: invalidUnitIds },
        );
      }

      // Validate club codes if any are provided
      const clubReferences = args.items
        .map(i => i.club_reference)
        .filter((ref): ref is string => ref !== undefined);

      if (clubReferences.length > 0) {
        const allCodes = await fetchAllClubCodes(ctx.supabaseClient);

        for (let idx = 0; idx < args.items.length; idx++) {
          const item = args.items[idx]!;
          if (item.club_reference) {
            const clubError = validateClubCode(
              item.club_reference,
              allCodes,
              `items[${idx}].club_reference`,
            );
            if (clubError) return clubError;
          }
        }
      }

      // Generate session ID
      const sessionId = crypto.randomUUID();

      // Build items JSONB (omit optional keys if not provided)
      const itemsJsonb = args.items.map(item => {
        const obj: Record<string, unknown> = {
          practice_unit_id: item.practice_unit_id,
          order: item.order,
          repeat_count: item.repeat_count,
        };
        if (item.club_reference !== undefined) {
          obj.club_reference = item.club_reference;
        }
        if (item.notes !== undefined) {
          obj.notes = item.notes;
        }
        if (item.focus_cue !== undefined) {
          obj.focus_cue = item.focus_cue;
        }
        return obj;
      });

      // Call the RPC
      const { error } = await ctx.supabaseClient.rpc('save_practice_session', {
        p_session_id: sessionId,
        p_name: name,
        p_notes: args.notes ?? null,
        p_items: itemsJsonb,
      });

      if (error) {
        return toolError(
          ErrorCodes.DATABASE_ERROR,
          'Failed to create session. Please try again.',
        );
      }

      return {
        content: [
          {
            type: 'text' as const,
            text: JSON.stringify({ session_id: sessionId }),
          },
        ],
      };
    },
  );
}
