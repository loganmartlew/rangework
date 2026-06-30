import type { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import type { UserContext } from '../auth/userContext.js';
import { z } from 'zod';

/**
 * Tool: `list_units`
 *
 * Returns all of the user's practice units, including full instruction text,
 * ball counts, club assignment, coaching notes, and tags. Call this before
 * creating new units to avoid duplication and to find units that can be reused
 * in a new session. Pass `tag_codes` to find units in a skill area (OR: a unit
 * matches if it carries any of the codes). If `has_uncounted_instructions` is
 * true, some instructions have no ball count — treat `total_ball_count` as a
 * partial estimate.
 */
export function registerListUnitsTool(
  server: McpServer,
  ctx: UserContext,
): void {
  server.registerTool(
    'list_units',
    {
      description:
        "Returns all of the user's practice units, including full instruction text, ball counts, club assignment, coaching notes, and tags. Call this before creating new units to avoid duplication and to find units that can be reused in a new session. Pass `tag_codes` to filter by skill area (OR semantics). If `has_uncounted_instructions` is true, some instructions have no ball count — treat `total_ball_count` as a partial estimate.",
      inputSchema: {
        tag_codes: z
          .array(z.string())
          .optional()
          .describe(
            'Optional tag codes to filter by, using OR semantics: a unit is returned if it carries at least one of these codes. Use codes from `list_tags`.',
          ),
      },
    },
    async args => {
      // Fetch all practice units for the user, ordered by updated_at DESC
      const { data: units, error: unitsError } = await ctx.supabaseClient
        .from('practice_units')
        .select('id, title, notes, focus, default_club_code')
        .order('updated_at', { ascending: false });

      if (unitsError) {
        return {
          content: [
            {
              type: 'text' as const,
              text: JSON.stringify({
                code: 'DATABASE_ERROR',
                message: 'Failed to fetch practice units.',
              }),
            },
          ],
          isError: true,
        };
      }

      if (!units || units.length === 0) {
        return {
          content: [
            {
              type: 'text' as const,
              text: JSON.stringify({ units: [] }),
            },
          ],
        };
      }

      // Fetch all instructions for these units
      const unitIds = units.map(u => u.id);
      const { data: instructions, error: instructionsError } =
        await ctx.supabaseClient
          .from('practice_unit_instructions')
          .select('practice_unit_id, sort_order, text, ball_count')
          .in('practice_unit_id', unitIds)
          .order('sort_order', { ascending: true });

      if (instructionsError) {
        return {
          content: [
            {
              type: 'text' as const,
              text: JSON.stringify({
                code: 'DATABASE_ERROR',
                message: 'Failed to fetch unit instructions.',
              }),
            },
          ],
          isError: true,
        };
      }

      // Group instructions by unit_id
      const instructionsByUnit = new Map<string, typeof instructions>();
      for (const inst of instructions ?? []) {
        const existing = instructionsByUnit.get(inst.practice_unit_id) ?? [];
        existing.push(inst);
        instructionsByUnit.set(inst.practice_unit_id, existing);
      }

      // Fetch tags for these units
      const { data: unitTags, error: unitTagsError } = await ctx.supabaseClient
        .from('practice_unit_tags')
        .select('practice_unit_id, tags(code, display_name)')
        .in('practice_unit_id', unitIds);

      if (unitTagsError) {
        return {
          content: [
            {
              type: 'text' as const,
              text: JSON.stringify({
                code: 'DATABASE_ERROR',
                message: 'Failed to fetch unit tags.',
              }),
            },
          ],
          isError: true,
        };
      }

      const tagsByUnit = new Map<
        string,
        Array<{ code: string; display_name: string }>
      >();
      for (const row of unitTags ?? []) {
        const tag = row.tags as unknown as {
          code: string;
          display_name: string;
        } | null;
        if (!tag) continue;
        const existing = tagsByUnit.get(row.practice_unit_id) ?? [];
        existing.push({ code: tag.code, display_name: tag.display_name });
        tagsByUnit.set(row.practice_unit_id, existing);
      }

      const filterCodes = args.tag_codes ?? [];

      // Build output for each unit
      const unitsOutput = units.map(unit => {
        const unitInstructions = instructionsByUnit.get(unit.id) ?? [];

        const hasUncountedInstructions = unitInstructions.some(
          i => i.ball_count == null,
        );
        const totalBallCount = hasUncountedInstructions
          ? null
          : unitInstructions.reduce((sum, i) => sum + (i.ball_count ?? 0), 0);

        const tags = tagsByUnit.get(unit.id) ?? [];

        return {
          id: unit.id,
          title: unit.title,
          notes: unit.notes,
          focus: unit.focus,
          default_club_code: unit.default_club_code,
          instruction_count: unitInstructions.length,
          total_ball_count: totalBallCount,
          has_uncounted_instructions: hasUncountedInstructions,
          tags,
          instructions: unitInstructions.map(i => ({
            order: i.sort_order,
            text: i.text,
            ball_count: i.ball_count,
          })),
        };
      });

      // Apply OR tag filter: keep units carrying at least one of the codes.
      const filteredUnits =
        filterCodes.length === 0
          ? unitsOutput
          : unitsOutput.filter(u =>
              u.tags.some(t => filterCodes.includes(t.code)),
            );

      return {
        content: [
          {
            type: 'text' as const,
            text: JSON.stringify({ units: filteredUnits }),
          },
        ],
      };
    },
  );
}
