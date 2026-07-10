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
import { validateObservationTypes } from '../validation/observation-types.js';

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
        "Creates a new practice session in the user's account. A session is an ordered list of practice units with optional per-item club overrides, coaching cues, repeat counts, and optional per-item `observation_types` (what to record per ball). Call `list_units` or `create_unit` first to get unit ids. Returns the new session's id. Each item's `practice_unit_id` must be a unit that belongs to this user.",
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
                  "How many times to cycle this unit's full instruction list (e.g. 2 = two passes of a multi-step progression). Must be a positive integer. Use values > 1 only for multi-instruction units; a single-instruction drill's volume belongs in that instruction's `ball_count` (use `repeat_count: 1`).",
                ),
              club_code: z
                .string()
                .optional()
                .describe(
                  "Optional club override for this item. Overrides the unit's default club. Use a `code` from `get_user_clubs`. Set it only when it differs from the unit's own club — a value equal to the unit's default is dropped.",
                ),
              focus_cue: z
                .string()
                .optional()
                .describe(
                  'Optional per-item coaching cue (e.g. "Hinge earlier"). An override, not a copy: set it only when it differs from the unit\'s own focus — a value equal to the unit\'s focus is dropped.',
                ),
              notes: z
                .string()
                .optional()
                .describe(
                  'Optional per-item reminder (e.g. "Use the 50y stake"). An override, not a copy: set it only when it differs from the unit\'s own notes — a value equal to the unit\'s notes is dropped.',
                ),
              observation_types: z
                .array(z.string())
                .optional()
                .describe(
                  'Optional per-ball observations to record for this item, drawn from: `success`, `strike_location`, `contact`, `shape`, `distance`, `direction`. `success` (whether a ball met the target) is valid only when the item\'s unit has a `success_criterion`. Omit for no per-ball capture (the default). Restraint: enable observations on at most 1–2 blocks per session — the ones tied to the player\'s stated focus — and tell the player what you enabled and why. Don\'t enable types on no-ball (action-only) drills.',
                ),
            }),
          )
          .describe(
            'Ordered list of practice units. Each item needs `practice_unit_id`, `order` (starting at 1), `repeat_count`, and optionally a `club_code`, `notes`, and `focus_cue`. Must have at least 1 item.',
          ),
        notes: z
          .string()
          .optional()
          .describe(
            'Optional session-level notes (e.g. "Tournament prep — focus on short game").',
          ),
        tag_codes: z
          .array(z.string())
          .optional()
          .describe(
            "Optional existing tag codes describing the session's own goal (e.g. [\"short_game\"]). Set independently of the units' tags. Use codes from `list_tags`; unknown codes are rejected. At most 8.",
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

      // Validate positive integers and check for duplicate order values
      for (let idx = 0; idx < args.items.length; idx++) {
        const item = args.items[idx]!;
        if (!Number.isInteger(item.order) || item.order < 1) {
          return toolError(
            ErrorCodes.VALIDATION_ERROR,
            'item order must be a positive integer',
            { field: `items[${idx}].order` },
          );
        }
        if (!Number.isInteger(item.repeat_count) || item.repeat_count < 1) {
          return toolError(
            ErrorCodes.VALIDATION_ERROR,
            'repeat_count must be a positive integer',
            { field: `items[${idx}].repeat_count` },
          );
        }
      }

      const orderValues = args.items.map(i => i.order);
      const uniqueOrders = new Set(orderValues);
      if (uniqueOrders.size !== orderValues.length) {
        return toolError(
          ErrorCodes.VALIDATION_ERROR,
          'item order values must be unique',
          { field: 'items' },
        );
      }

      // Pre-fetch the user's units with their base values, so per-item
      // overrides that merely copy the unit can be stripped below.
      const { data: ownedUnits, error: unitsError } = await ctx.supabaseClient
        .from('practice_units')
        .select('id, notes, focus, default_club_code, success_criterion');

      if (unitsError) {
        return toolError(
          ErrorCodes.DATABASE_ERROR,
          'Failed to fetch user units.',
        );
      }

      const unitsById = new Map((ownedUnits ?? []).map(u => [u.id, u]));
      const ownedUnitIds = new Set(unitsById.keys());

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

      // Validate per-item observation_types against the fixed vocabulary and
      // pre-enforce the success-requires-criterion rule (the friendly,
      // field-scoped half of the two-layer rule; the RPC is the backstop).
      const observationTypesByIndex: string[][] = [];
      for (let idx = 0; idx < args.items.length; idx++) {
        const item = args.items[idx]!;
        const validated = validateObservationTypes(
          item.observation_types,
          `items[${idx}].observation_types`,
        );
        if ('error' in validated) return validated.error;

        if (validated.types.includes('success')) {
          const unit = unitsById.get(item.practice_unit_id);
          const criterion = unit?.success_criterion;
          if (criterion == null || criterion.trim() === '') {
            return toolError(
              ErrorCodes.VALIDATION_ERROR,
              "the 'success' observation requires the unit to have a success_criterion; set one with create_unit first",
              { field: `items[${idx}].observation_types` },
            );
          }
        }

        observationTypesByIndex.push(validated.types);
      }

      // Validate club codes if any are provided
      const clubReferences = args.items
        .map(i => i.club_code)
        .filter((ref): ref is string => ref !== undefined);

      if (clubReferences.length > 0) {
        let allCodes: string[];
        try {
          allCodes = await fetchAllClubCodes(ctx.supabaseClient);
        } catch {
          return toolError(
            ErrorCodes.DATABASE_ERROR,
            'Failed to validate club codes. Please try again.',
          );
        }

        for (let idx = 0; idx < args.items.length; idx++) {
          const item = args.items[idx]!;
          if (item.club_code) {
            const clubError = validateClubCode(
              item.club_code,
              allCodes,
              `items[${idx}].club_code`,
            );
            if (clubError) return clubError;
          }
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

      // Generate session ID
      const sessionId = crypto.randomUUID();

      // Build items JSONB (omit optional keys if not provided). Overrides
      // that exactly equal the unit's own base value are dropped — loss-free,
      // and it guarantees override ≠ base in stored data (override hygiene).
      const sameAsBase = (
        override: string | undefined,
        base: string | null | undefined,
      ): boolean =>
        override !== undefined &&
        base != null &&
        override.trim() === base.trim();

      const itemsJsonb = args.items.map((item, idx) => {
        const unit = unitsById.get(item.practice_unit_id);
        const obj: Record<string, unknown> = {
          practice_unit_id: item.practice_unit_id,
          order: item.order,
          repeat_count: item.repeat_count,
        };
        // Omit the key entirely when no types are enabled — the RPC coalesces a
        // missing key to an empty array (same as "no per-ball capture").
        const observationTypes = observationTypesByIndex[idx] ?? [];
        if (observationTypes.length > 0) {
          obj.observation_types = observationTypes;
        }
        if (
          item.club_code !== undefined &&
          !sameAsBase(item.club_code, unit?.default_club_code)
        ) {
          obj.club_code = item.club_code;
        }
        if (item.notes !== undefined && !sameAsBase(item.notes, unit?.notes)) {
          obj.notes = item.notes;
        }
        if (
          item.focus_cue !== undefined &&
          !sameAsBase(item.focus_cue, unit?.focus)
        ) {
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
        p_tag_ids: tagIds,
      });

      if (error) {
        // Backstop for the success-requires-criterion rule: if the criterion
        // was removed between our pre-fetch and the save, the RPC raises. Map
        // it to the same friendly validation error (item attribution is lost
        // at this layer, so it is field-scoped to the array).
        if (error.message?.includes('success criterion')) {
          return toolError(
            ErrorCodes.VALIDATION_ERROR,
            "the 'success' observation requires the unit to have a success_criterion; set one with create_unit first",
            { field: 'items' },
          );
        }
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
