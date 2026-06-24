import type { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import type { UserContext } from '../auth/userContext.js';

/**
 * Tool: `list_sessions`
 *
 * Returns all of the user's practice sessions, including their item lineup,
 * club overrides, repeat counts, and coaching notes. Call this to understand
 * how the user's existing sessions are structured before creating a new one.
 * If `has_uncounted_items` is true, one or more units in the session have no
 * ball count on some instructions — treat the total as a partial estimate.
 */
export function registerListSessionsTool(
  server: McpServer,
  ctx: UserContext,
): void {
  server.registerTool(
    'list_sessions',
    {
      description:
        "Returns all of the user's practice sessions, including their item lineup, club overrides, repeat counts, and coaching notes. Call this to understand how the user's existing sessions are structured before creating a new one. If `has_uncounted_items` is true, one or more units in the session have no ball count on some instructions — treat the total as a partial estimate.",
      inputSchema: {},
    },
    async () => {
      // Fetch all practice sessions for the user, ordered by updated_at DESC
      const { data: sessions, error: sessionsError } = await ctx.supabaseClient
        .from('practice_sessions')
        .select('id, name, notes')
        .order('updated_at', { ascending: false });

      if (sessionsError) {
        return {
          content: [
            {
              type: 'text' as const,
              text: JSON.stringify({
                code: 'DATABASE_ERROR',
                message: 'Failed to fetch practice sessions.',
              }),
            },
          ],
          isError: true,
        };
      }

      if (!sessions || sessions.length === 0) {
        return {
          content: [
            {
              type: 'text' as const,
              text: JSON.stringify({ sessions: [] }),
            },
          ],
        };
      }

      // Fetch all session items for these sessions
      const sessionIds = sessions.map(s => s.id);
      const { data: items, error: itemsError } = await ctx.supabaseClient
        .from('practice_session_items')
        .select(
          'practice_session_id, practice_unit_id, sort_order, repeat_count, club_code, notes, focus_cue',
        )
        .in('practice_session_id', sessionIds)
        .order('sort_order', { ascending: true });

      if (itemsError) {
        return {
          content: [
            {
              type: 'text' as const,
              text: JSON.stringify({
                code: 'DATABASE_ERROR',
                message: 'Failed to fetch session items.',
              }),
            },
          ],
          isError: true,
        };
      }

      // Collect all unique unit IDs referenced in items
      const unitIds = [...new Set((items ?? []).map(i => i.practice_unit_id))];

      // Fetch units and their instructions in parallel — both depend only on unitIds
      const [
        { data: units, error: unitsError },
        { data: unitInstructions, error: unitInstructionsError },
      ] = await Promise.all([
        ctx.supabaseClient
          .from('practice_units')
          .select('id, title')
          .in('id', unitIds),
        ctx.supabaseClient
          .from('practice_unit_instructions')
          .select('practice_unit_id, ball_count')
          .in('practice_unit_id', unitIds),
      ]);

      if (unitsError) {
        return {
          content: [
            {
              type: 'text' as const,
              text: JSON.stringify({
                code: 'DATABASE_ERROR',
                message: 'Failed to fetch referenced units.',
              }),
            },
          ],
          isError: true,
        };
      }

      if (unitInstructionsError) {
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

      // Build unit lookup map
      const unitMap = new Map<
        string,
        { title: string; totalBallCount: number | null }
      >();
      for (const unit of units ?? []) {
        unitMap.set(unit.id, { title: unit.title, totalBallCount: 0 });
      }

      // Compute per-unit ball totals
      const instructionsByUnit = new Map<string, typeof unitInstructions>();
      for (const inst of unitInstructions ?? []) {
        const existing = instructionsByUnit.get(inst.practice_unit_id) ?? [];
        existing.push(inst);
        instructionsByUnit.set(inst.practice_unit_id, existing);
      }

      for (const [unitId, instructions] of instructionsByUnit) {
        const unit = unitMap.get(unitId);
        if (!unit) continue;

        const hasUncounted = instructions.some(i => i.ball_count == null);
        unit.totalBallCount = hasUncounted
          ? null
          : instructions.reduce((sum, i) => sum + (i.ball_count ?? 0), 0);
      }

      // Group items by session_id
      const itemsBySession = new Map<string, typeof items>();
      for (const item of items ?? []) {
        const existing = itemsBySession.get(item.practice_session_id) ?? [];
        existing.push(item);
        itemsBySession.set(item.practice_session_id, existing);
      }

      // Build output for each session
      const sessionsOutput = sessions.map(session => {
        const sessionItems = itemsBySession.get(session.id) ?? [];

        let hasUncountedItems = false;
        let totalBallCount = 0;

        for (const item of sessionItems) {
          const unit = unitMap.get(item.practice_unit_id);
          if (!unit) continue;

          if (unit.totalBallCount === null) {
            hasUncountedItems = true;
          } else {
            totalBallCount += item.repeat_count * unit.totalBallCount;
          }
        }

        return {
          id: session.id,
          name: session.name,
          notes: session.notes,
          total_ball_count: hasUncountedItems ? null : totalBallCount,
          has_uncounted_items: hasUncountedItems,
          items: sessionItems.map(item => {
            const unit = unitMap.get(item.practice_unit_id);
            return {
              order: item.sort_order,
              unit_id: item.practice_unit_id,
              unit_title: unit?.title ?? null,
              repeat_count: item.repeat_count,
              club_code: item.club_code,
              notes: item.notes,
              focus_cue: item.focus_cue,
            };
          }),
        };
      });

      return {
        content: [
          {
            type: 'text' as const,
            text: JSON.stringify({ sessions: sessionsOutput }),
          },
        ],
      };
    },
  );
}
