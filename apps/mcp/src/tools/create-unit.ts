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
 * Tool: `create_unit`
 *
 * Creates a new practice unit (a single drill) in the user's account.
 * A unit has a title, one to ten step-by-step instructions (each with optional
 * ball count), optional coaching focus, and an optional default club.
 * Returns the new unit's id — save this to use in `create_session`.
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
                  'Optional number of balls for this step. Must be a positive integer if provided.',
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
        default_club_reference: z
          .string()
          .optional()
          .describe(
            'Optional default club for this drill. Use the `code` from `get_user_clubs`.',
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

      // Trim and validate instruction texts
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
        instructions.push({ ...inst, text });
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

      // Validate club code if provided
      if (args.default_club_reference) {
        const allCodes = await fetchAllClubCodes(ctx.supabaseClient);
        const clubError = validateClubCode(
          args.default_club_reference,
          allCodes,
          'default_club_reference',
        );
        if (clubError) return clubError;
      }

      // Generate unit ID
      const unitId = crypto.randomUUID();

      // Build instructions JSONB (omit ball_count key if not provided)
      const instructionsJsonb = instructions.map(inst => {
        const obj: Record<string, unknown> = {
          order: inst.order,
          text: inst.text,
        };
        if (inst.ball_count !== undefined) {
          obj.ball_count = inst.ball_count;
        }
        return obj;
      });

      // Call the RPC
      const { error } = await ctx.supabaseClient.rpc('save_practice_unit', {
        p_unit_id: unitId,
        p_title: title,
        p_notes: args.notes ?? null,
        p_focus: args.focus ?? null,
        p_default_club_reference: args.default_club_reference ?? null,
        p_instructions: instructionsJsonb,
      });

      if (error) {
        // Map FK violation to UNKNOWN_CLUB_CODE
        if (error.message.includes('foreign key') || error.code === '23503') {
          const allCodes = await fetchAllClubCodes(ctx.supabaseClient);
          return toolError(
            ErrorCodes.UNKNOWN_CLUB_CODE,
            `Unknown club code: ${args.default_club_reference}`,
            {
              field: 'default_club_reference',
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
