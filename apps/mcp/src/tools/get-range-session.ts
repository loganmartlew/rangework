import type { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import type { UserContext } from '../auth/userContext.js';
import { z } from 'zod';
import type { CallToolResult } from '@modelcontextprotocol/sdk/types.js';
import { toolError, ErrorCodes } from '../validation/tool-errors.js';

interface SnapshotStep {
  unitIndex: number;
  ballCount?: number | null;
}

interface SnapshotUnit {
  unitTitle: string;
  unitFocus?: string | null;
  itemFocusCue?: string | null;
  successCriterion?: string | null;
  observationTypes?: string[] | null;
}

interface Snapshot {
  sessionNotes?: string | null;
  units?: SnapshotUnit[];
  steps?: SnapshotStep[];
}

interface RangeSessionRow {
  id: string;
  session_name: string;
  started_at: string;
  completed_at: string | null;
  session_note: string | null;
  snapshot: Snapshot | null;
  snapshot_version: number;
  completed_steps: Array<{ stepIndex: number }> | null;
  block_results: Record<string, { note?: unknown; manualCount?: unknown }> | null;
}

interface ObservationRow {
  step_index: number;
  observed_values: Record<string, string> | null;
}

/**
 * Tool: `get_range_session`
 *
 * One completed range session's block-level detail: the player's notes, the
 * per-block manual counts, per-type observation aggregates (each with its own
 * observed denominator), and the raw per-ball observations. Only completed
 * sessions resolve — an Active, Abandoned, nonexistent, or other-user id all
 * return `RANGE_SESSION_NOT_FOUND` (invisibility without leaking which state).
 */
export function registerGetRangeSessionTool(
  server: McpServer,
  ctx: UserContext,
): void {
  server.registerTool(
    'get_range_session',
    {
      description:
        "Returns one completed range session's full detail: session and per-block notes, per-block manual counts, per-observation-type aggregates, and the raw per-ball observations. Each block corresponds to one unit in the session as it was planned. Observation counts always name their denominator (`observed` = balls actually observed for that type) — never present an observed count as a rate of all balls hit. A block's `result.manual_count` and its derived `observations.success` count never coexist. Observation values are stored from a canonical (right-handed) perspective; do not mirror-flip them for a left-handed player. Returns RANGE_SESSION_NOT_FOUND if the session does not exist or is not yet completed.",
      inputSchema: {
        range_session_id: z
          .string()
          .describe(
            'The `id` of a completed range session, from `list_range_sessions`.',
          ),
      },
    },
    async (args): Promise<CallToolResult> => {
      const rangeSessionId = args.range_session_id?.trim();
      if (!rangeSessionId) {
        return toolError(
          ErrorCodes.VALIDATION_ERROR,
          'range_session_id must not be empty',
          { field: 'range_session_id' },
        );
      }

      // Completed-only predicate at the query level, same as the list tool.
      // RLS scopes to the user; a well-formed id belonging to another user
      // simply returns no row — indistinguishable from nonexistent.
      const { data: session, error: sessionError } = await ctx.supabaseClient
        .from('range_sessions')
        .select(
          'id, session_name, started_at, completed_at, session_note, snapshot, snapshot_version, completed_steps, block_results',
        )
        .eq('id', rangeSessionId)
        .not('completed_at', 'is', null)
        .is('abandoned_at', null)
        .maybeSingle();

      if (sessionError) {
        // A malformed id (not a valid uuid) makes Postgres raise 22P02 rather
        // than returning no row. Treat it as unresolvable — same invisibility
        // response as a well-formed id that matches nothing — so a bad id never
        // leaks a scary transport error.
        if (sessionError.code === '22P02') {
          return toolError(
            ErrorCodes.RANGE_SESSION_NOT_FOUND,
            'range session not found or not yet completed',
          );
        }
        return toolError(
          ErrorCodes.DATABASE_ERROR,
          'Failed to fetch range session.',
        );
      }

      if (!session) {
        return toolError(
          ErrorCodes.RANGE_SESSION_NOT_FOUND,
          'range session not found or not yet completed',
        );
      }

      const row = session as RangeSessionRow;

      const { data: observations, error: observationsError } =
        await ctx.supabaseClient
          .from('range_session_observations')
          .select('step_index, observed_values')
          .eq('range_session_id', rangeSessionId)
          .order('step_index', { ascending: true });

      if (observationsError) {
        return toolError(
          ErrorCodes.DATABASE_ERROR,
          'Failed to fetch range session observations.',
        );
      }

      const blocks = buildBlocks(row, (observations ?? []) as ObservationRow[]);

      return {
        content: [
          {
            type: 'text' as const,
            text: JSON.stringify({
              id: row.id,
              session_name: row.session_name,
              started_at: row.started_at,
              completed_at: row.completed_at,
              session_note: row.session_note,
              planning_notes: row.snapshot?.sessionNotes ?? null,
              blocks,
            }),
          },
        ],
      };
    },
  );
}

interface PerTypeAggregate {
  observed: number;
  counts: Record<string, number>;
}

function buildBlocks(row: RangeSessionRow, observations: ObservationRow[]) {
  const snapshot = row.snapshot ?? {};
  const units = snapshot.units ?? [];
  const steps = snapshot.steps ?? [];
  const ballGranular = row.snapshot_version >= 2;

  const completed = new Set((row.completed_steps ?? []).map(c => c.stepIndex));

  // Group snapshot step indices by their block (unitIndex).
  const stepIndicesByBlock = new Map<number, number[]>();
  steps.forEach((step, index) => {
    const list = stepIndicesByBlock.get(step.unitIndex) ?? [];
    list.push(index);
    stepIndicesByBlock.set(step.unitIndex, list);
  });

  // Group observations by block via each row's step_index → unitIndex. An
  // observation whose step_index doesn't resolve against the snapshot is
  // skipped, never thrown on (future/buggy app tolerance).
  const observationsByBlock = new Map<number, ObservationRow[]>();
  for (const obs of observations) {
    const step = steps[obs.step_index];
    if (!step) continue;
    const list = observationsByBlock.get(step.unitIndex) ?? [];
    list.push(obs);
    observationsByBlock.set(step.unitIndex, list);
  }

  return units.map((unit, blockIndex) => {
    const blockStepIndices = stepIndicesByBlock.get(blockIndex) ?? [];

    const ballsPlanned = ballGranular
      ? blockStepIndices.filter(i => steps[i]?.ballCount === 1).length
      : null;
    const ballsHit = ballGranular
      ? blockStepIndices.filter(
          i => steps[i]?.ballCount === 1 && completed.has(i),
        ).length
      : null;

    const focusCue =
      nonBlank(unit.itemFocusCue) ?? nonBlank(unit.unitFocus) ?? null;

    const blockObservations = observationsByBlock.get(blockIndex) ?? [];

    // Aggregates: value→count per type, plus the per-type observed denominator.
    const aggregates = new Map<string, PerTypeAggregate>();
    const rawBalls: Array<{ step_index: number; values: Record<string, string> }> =
      [];

    for (const obs of blockObservations) {
      const values = obs.observed_values ?? {};
      const entries = Object.entries(values).filter(
        ([, v]) => typeof v === 'string' && v.length > 0,
      );
      // The DB's "no row and empty row both mean unobserved" carries through:
      // a ball with no observed value is omitted from the raw list.
      if (entries.length === 0) continue;

      rawBalls.push({
        step_index: obs.step_index,
        values: Object.fromEntries(entries),
      });

      for (const [type, value] of entries) {
        const agg = aggregates.get(type) ?? { observed: 0, counts: {} };
        agg.observed += 1;
        agg.counts[value] = (agg.counts[value] ?? 0) + 1;
        aggregates.set(type, agg);
      }
    }

    return {
      block_index: blockIndex,
      unit_title: unit.unitTitle,
      focus_cue: focusCue,
      success_criterion: unit.successCriterion ?? null,
      observation_types: unit.observationTypes ?? [],
      balls_planned: ballsPlanned,
      balls_hit: ballsHit,
      result: mapResult(row.block_results?.[String(blockIndex)]),
      observations: Object.fromEntries(aggregates),
      balls: rawBalls,
    };
  });
}

/**
 * Map a stored `block_results` entry to the response shape. Absent entry → null;
 * the two inner fields are independently optional, so an absent half is null.
 */
function mapResult(
  entry: { note?: unknown; manualCount?: unknown } | undefined,
): { note: string | null; manual_count: number | null } | null {
  if (!entry) return null;
  const note = typeof entry.note === 'string' ? entry.note : null;
  const manualCount =
    typeof entry.manualCount === 'number' ? entry.manualCount : null;
  return { note, manual_count: manualCount };
}

function nonBlank(value: string | null | undefined): string | null {
  if (typeof value !== 'string') return null;
  return value.trim().length > 0 ? value : null;
}
