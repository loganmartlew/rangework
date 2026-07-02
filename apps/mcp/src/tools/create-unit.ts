import type { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import type { UserContext } from '../auth/userContext.js';
import { z } from 'zod';
import type { CallToolResult } from '@modelcontextprotocol/sdk/types.js';
import { toolError, ErrorCodes } from '../validation/tool-errors.js';
import {
  fetchAllClubCodes,
  validateClubCode,
} from '../validation/club-codes.js';
import { resolveTagCodes } from '../validation/tags.js';

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
        "Creates a new practice unit (a single drill) in the user's account. A unit has a title, one to ten step-by-step instructions (each with optional ball count), optional coaching focus, and an optional default club. Returns the new unit's id — save this to use in `create_session`. Club references must use the `code` field from `get_user_clubs`, not the display name.",
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
                  'Number of balls for this step. A nonnegative integer: a positive count is N balls, and 0 is a deliberate no-ball step (e.g. practice swings for feel). Omit the field entirely to leave the count uncounted (unknown) — omitting is not the same as 0.',
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
      // Validate instructions array length
      if (!args.instructions || args.instructions.length === 0) {
        return toolError(
          ErrorCodes.VALIDATION_ERROR,
          'at least one instruction is required',
          {
            field: 'instructions',
          },
        );
      }
      if (args.instructions.length > 10) {
        return toolError(
          ErrorCodes.VALIDATION_ERROR,
          'a unit may have at most 10 instructions',
          {
            field: 'instructions',
          },
        );
      }

      // Trim and validate title
      const title = args.title.trim();
      if (!title) {
        return toolError(
          ErrorCodes.VALIDATION_ERROR,
          'title must not be empty',
          {
            field: 'title',
          },
        );
      }

      // Trim and validate instruction texts; check positive integers
      const instructions = [];
      for (let idx = 0; idx < args.instructions.length; idx++) {
        const inst = args.instructions[idx]!;
        const text = inst.text.trim();
        if (!text) {
          return toolError(
            ErrorCodes.VALIDATION_ERROR,
            'instruction text must not be empty',
            { field: `instructions[${idx}].text` },
          );
        }
        if (!Number.isInteger(inst.order) || inst.order < 1) {
          return toolError(
            ErrorCodes.VALIDATION_ERROR,
            'instruction order must be a positive integer',
            { field: `instructions[${idx}].order` },
          );
        }
        if (
          inst.ball_count !== undefined &&
          (!Number.isInteger(inst.ball_count) || inst.ball_count < 0)
        ) {
          return toolError(
            ErrorCodes.VALIDATION_ERROR,
            'ball_count must not be negative',
            { field: `instructions[${idx}].ball_count` },
          );
        }
        // Treat a blank per-instruction club_code as "use default" (absent).
        const clubCode = inst.club_code?.trim() ? inst.club_code.trim() : undefined;
        instructions.push({ ...inst, text, club_code: clubCode });
      }

      // Check for duplicate order values
      const orderValues = instructions.map(i => i.order);
      const uniqueOrders = new Set(orderValues);
      if (uniqueOrders.size !== orderValues.length) {
        return toolError(
          ErrorCodes.VALIDATION_ERROR,
          'instruction order values must be unique',
          { field: 'instructions' },
        );
      }

      // Validate every club code provided — the unit default plus any
      // per-instruction club — against the catalog in a single fetch. This is
      // the primary guard; the post-RPC FK branch below is a safety net.
      const clubCodesToValidate: Array<{ code: string; field: string }> = [];
      if (args.default_club_code) {
        clubCodesToValidate.push({
          code: args.default_club_code,
          field: 'default_club_code',
        });
      }
      instructions.forEach((inst, idx) => {
        if (inst.club_code) {
          clubCodesToValidate.push({
            code: inst.club_code,
            field: `instructions[${idx}].club_code`,
          });
        }
      });

      if (clubCodesToValidate.length > 0) {
        let allCodes: string[];
        try {
          allCodes = await fetchAllClubCodes(ctx.supabaseClient);
        } catch {
          return toolError(
            ErrorCodes.DATABASE_ERROR,
            'Failed to validate club code. Please try again.',
          );
        }
        for (const { code, field } of clubCodesToValidate) {
          const clubError = validateClubCode(code, allCodes, field);
          if (clubError) return clubError;
        }
      }

      // Resolve tag codes to ids (rejects unknown codes; never creates tags)
      let tagIds: string[] = [];
      if (args.tag_codes && args.tag_codes.length > 0) {
        const resolved = await resolveTagCodes(
          ctx.supabaseClient,
          args.tag_codes,
          'tag_codes',
        );
        if ('error' in resolved) return resolved.error;
        tagIds = resolved.ids;
      }

      // Generate unit ID
      const unitId = crypto.randomUUID();

      // Build instructions JSONB (omit ball_count / club_code keys if not provided)
      const instructionsJsonb = instructions.map(inst => {
        const obj: Record<string, unknown> = {
          order: inst.order,
          text: inst.text,
        };
        if (inst.ball_count !== undefined) {
          obj.ball_count = inst.ball_count;
        }
        if (inst.club_code !== undefined) {
          obj.club_code = inst.club_code;
        }
        return obj;
      });

      // Call the RPC
      const { error } = await ctx.supabaseClient.rpc('save_practice_unit', {
        p_unit_id: unitId,
        p_title: title,
        p_notes: args.notes ?? null,
        p_focus: args.focus ?? null,
        p_default_club_code: args.default_club_code ?? null,
        p_instructions: instructionsJsonb,
        p_tag_ids: tagIds,
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
