import type { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import type { UserContext } from '../auth/userContext.js';

/**
 * Tool: `list_units`
 *
 * Returns all of the user's practice units, including full instruction text,
 * ball counts, club assignment, and coaching notes. Call this before creating
 * new units to avoid duplication and to find units that can be reused in a
 * new session. If `has_uncounted_instructions` is true, some instructions
 * have no ball count — treat `total_ball_count` as a partial estimate.
 */
export function registerListUnitsTool(
  server: McpServer,
  ctx: UserContext,
): void {
  server.registerTool(
    'list_units',
    {
      description:
        "Returns all of the user's practice units, including full instruction text, ball counts, club assignment, and coaching notes. Call this before creating new units to avoid duplication and to find units that can be reused in a new session. If `has_uncounted_instructions` is true, some instructions have no ball count — treat `total_ball_count` as a partial estimate.",
      inputSchema: {},
    },
    async () => {
      // Fetch all practice units for the user, ordered by updated_at DESC
      const { data: units, error: unitsError } = await ctx.supabaseClient
        .from('practice_units')
        .select('id, title, notes, focus, default_club_reference')
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

      // Build output for each unit
      const unitsOutput = units.map(unit => {
        const unitInstructions = instructionsByUnit.get(unit.id) ?? [];

        const hasUncountedInstructions = unitInstructions.some(
          i => i.ball_count == null,
        );
        const totalBallCount = hasUncountedInstructions
          ? null
          : unitInstructions.reduce((sum, i) => sum + (i.ball_count ?? 0), 0);

        return {
          id: unit.id,
          title: unit.title,
          notes: unit.notes,
          focus: unit.focus,
          default_club_reference: unit.default_club_reference,
          instruction_count: unitInstructions.length,
          total_ball_count: totalBallCount,
          has_uncounted_instructions: hasUncountedInstructions,
          instructions: unitInstructions.map(i => ({
            order: i.sort_order,
            text: i.text,
            ball_count: i.ball_count,
          })),
        };
      });

      return {
        content: [
          {
            type: 'text' as const,
            text: JSON.stringify({ units: unitsOutput }),
          },
        ],
      };
    },
  );
}
