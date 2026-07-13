import type { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import type { UserContext } from '../auth/userContext.js';
import { z } from 'zod';
import type { CallToolResult } from '@modelcontextprotocol/sdk/types.js';
import { toolError, ErrorCodes } from '../validation/tool-errors.js';
import { fetchAllClubCodes } from '../validation/club-codes.js';
import {
  validateInlineUnit,
  buildInlineUnitJsonb,
} from '../validation/inline-units.js';

/**
 * Tool: `create_unit`
 *
 * Creates a new practice unit (a single drill) in the user's account.
 * A unit has a title, one to ten step-by-step instructions (each with optional
 * ball count and optional per-instruction club), optional coaching focus, and an
 * optional default club. A step with no club of its own falls back to the unit's
 * default club. Returns the new unit's id — save this to use in `create_session`.
 * Club references must use the `code` field from `get_user_clubs`, not the display name.
 */
export function registerCreateUnitTool(
  server: McpServer,
  ctx: UserContext,
): void {
  server.registerTool(
    'create_unit',
    {
      description:
        "Creates a new practice unit (a single drill) in the user's account. A unit has a title, one to ten step-by-step instructions (each with optional ball count), optional coaching focus, an optional default club, and an optional `success_criterion` (a player-judged success rubric that lets a session enable the `success` observation on this drill). Returns the new unit's id — save this to use in `create_session`. Club references must use the `code` field from `get_user_clubs`, not the display name.",
      inputSchema: {
        title: z
          .string()
          .describe(
            'Short name for the drill (e.g. "Gate drill", "Draw shot tracer").',
          ),
        instructions: z
          .array(
            z.object({
              order: z
                .number()
                .describe(
                  'Step number, starting at 1. Must be a positive integer.',
                ),
              text: z.string().describe('Instruction text.'),
              ball_count: z
                .number()
                .optional()
                .describe(
                  "Number of balls for this step. A nonnegative integer: a positive count is N balls, and 0 is a deliberate no-ball step (e.g. practice swings for feel). Omit the field entirely to leave the count uncounted (unknown) — omitting is not the same as 0. For a single-instruction drill, put the drill's full volume here (e.g. 15 balls → `ball_count: 15`) instead of relying on the session's `repeat_count`.",
                ),
              club_code: z
                .string()
                .optional()
                .describe(
                  'Optional club for this specific step. Use the `code` from `get_user_clubs` (same vocabulary as `default_club_code`). When set, this step uses this club; when omitted, the step falls back to the unit `default_club_code`. Use this to vary club across steps (e.g. a wedge ladder: GW, then SW, then LW).',
                ),
            }),
          )
          .describe(
            'Ordered list of steps. Each step needs `order` (starting at 1), `text`, and an optional `ball_count`. Must have 1-10 items.',
          ),
        focus: z
          .string()
          .optional()
          .describe(
            'Optional single-sentence coaching cue or swing thought (e.g. "Keep the club face square through impact").',
          ),
        notes: z
          .string()
          .optional()
          .describe(
            'Optional context or reminders for the user (e.g. "Use an alignment stick").',
          ),
        success_criterion: z
          .string()
          .optional()
          .describe(
            'Optional short, player-judged success rubric for this drill (e.g. "inside 5m of the 60m flag"). Never parsed by code — the player judges each ball against it. Set one whenever the drill has a checkable target: it is what makes an X-of-Y success count meaningful, and it is required before the `success` observation can be enabled on this unit in a session. Omit for drills with no checkable target.',
          ),
        default_club_code: z
          .string()
          .optional()
          .describe(
            'Optional default club for this drill. Use the `code` from `get_user_clubs`.',
          ),
        tag_codes: z
          .array(z.string())
          .optional()
          .describe(
            'Optional existing tag codes to classify this drill (e.g. ["putting", "short_game"]). Use codes from `list_tags`. Unknown codes are rejected — mint a tag with `create_tag` first. At most 8.',
          ),
      },
    },
    async (args): Promise<CallToolResult> => {
      const validated = await validateInlineUnit(ctx, args, '');
      if ('error' in validated) return validated.error;
      const unit = validated.unit;

      // Generate unit ID
      const unitId = crypto.randomUUID();

      const instructionsJsonb = buildInlineUnitJsonb(unit.instructions);

      // Call the RPC
      const { error } = await ctx.supabaseClient.rpc('save_practice_unit', {
        p_unit_id: unitId,
        p_title: unit.title,
        p_notes: unit.notes ?? null,
        p_focus: unit.focus ?? null,
        p_default_club_code: unit.defaultClubCode ?? null,
        p_instructions: instructionsJsonb,
        p_tag_ids: unit.tagIds,
        p_success_criterion: unit.successCriterion ?? null,
      });

      if (error) {
        // A club-code FK violation here should be unreachable: every club code
        // (unit default + per-instruction) is pre-validated above. Since any of
        // N instructions may now carry a club, this branch can no longer
        // attribute the violation to a specific field, so it returns a
        // non-field-specific error with the valid-codes hint as a safety net.
        if (error.message.includes('foreign key') || error.code === '23503') {
          let allCodes: string[] = [];
          try {
            allCodes = await fetchAllClubCodes(ctx.supabaseClient);
          } catch {
            // Best-effort — return the error without valid_codes hint
          }
          return toolError(
            ErrorCodes.UNKNOWN_CLUB_CODE,
            'One of the provided club codes is not in the catalog. Use a `code` from `get_user_clubs`.',
            {
              valid_codes: allCodes,
            },
          );
        }

        return toolError(
          ErrorCodes.DATABASE_ERROR,
          'Failed to create unit. Please try again.',
        );
      }

      return {
        content: [
          {
            type: 'text' as const,
            text: JSON.stringify({ unit_id: unitId }),
          },
        ],
      };
    },
  );
}
