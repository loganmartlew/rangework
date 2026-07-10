import type { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import type { UserContext } from '../auth/userContext.js';
import { z } from 'zod';
import type { CallToolResult } from '@modelcontextprotocol/sdk/types.js';
import { toolError, ErrorCodes } from '../validation/tool-errors.js';

/** Default number of completed sessions returned when no `limit` is given. */
const DEFAULT_LIMIT = 20;

interface SnapshotStep {
  unitIndex: number;
  ballCount?: number | null;
}

interface RangeSessionRow {
  id: string;
  session_name: string;
  source_session_id: string | null;
  started_at: string;
  completed_at: string | null;
  session_note: string | null;
  snapshot: { steps?: SnapshotStep[] } | null;
  snapshot_version: number;
  completed_steps: Array<{ stepIndex: number }> | null;
  block_results: Record<string, unknown> | null;
}

/**
 * Tool: `list_range_sessions`
 *
 * Thin summaries of the user's Completed range sessions (Active and Abandoned
 * sessions are invisible), newest first. Each row carries just enough to decide
 * which sessions are worth a `get_range_session` drill-down: total balls hit,
 * the session note, which blocks recorded a result, and whether any per-ball
 * observations exist. Interim state until app capture ships: pre-existing
 * completed sessions list cleanly with null/empty capture fields.
 */
export function registerListRangeSessionsTool(
  server: McpServer,
  ctx: UserContext,
): void {
  server.registerTool(
    'list_range_sessions',
    {
      description:
        "Lists the user's completed range sessions (a range session is one execution of a practice session on the range), newest first. Summaries only — call `get_range_session` for a session's block-level notes, observation counts, and raw per-ball detail. `balls_hit` is the number of balls actually hit (null for older coarse-grained sessions). `blocks_with_results` lists the block indices that recorded a note or manual count; `has_observations` is true when any per-ball observation was recorded. Only completed sessions appear. Pass `limit` to fetch more than the default 20.",
      inputSchema: {
        limit: z
          .number()
          .optional()
          .describe(
            'Maximum number of sessions to return, newest first. Defaults to 20. Raise it to see deeper history.',
          ),
      },
    },
    async (args): Promise<CallToolResult> => {
      const limit =
        args.limit !== undefined &&
        Number.isInteger(args.limit) &&
        args.limit > 0
          ? args.limit
          : DEFAULT_LIMIT;

      // Completed-only at the query level: Abandoned and Active sessions never
      // reach the coach. Ordered newest-completed first; bounded by `limit` so
      // the per-session snapshot work stays flat as history grows.
      const { data: sessions, error: sessionsError } = await ctx.supabaseClient
        .from('range_sessions')
        .select(
          'id, session_name, source_session_id, started_at, completed_at, session_note, snapshot, snapshot_version, completed_steps, block_results',
        )
        .not('completed_at', 'is', null)
        .is('abandoned_at', null)
        .order('completed_at', { ascending: false })
        .limit(limit);

      if (sessionsError) {
        return toolError(
          ErrorCodes.DATABASE_ERROR,
          'Failed to fetch range sessions.',
        );
      }

      const rows = (sessions ?? []) as RangeSessionRow[];

      if (rows.length === 0) {
        return {
          content: [{ type: 'text' as const, text: JSON.stringify({ sessions: [] }) }],
        };
      }

      // One grouped query over observations for the fetched sessions. An empty
      // `observed_values` ('{}') means "unobserved" (Stage 1 schema), so a row
      // only counts when it carries at least one non-empty value — matching the
      // filter `get_range_session` applies, so the two tools never disagree.
      const sessionIds = rows.map(r => r.id);
      const { data: observationRows, error: observationsError } =
        await ctx.supabaseClient
          .from('range_session_observations')
          .select('range_session_id, observed_values')
          .in('range_session_id', sessionIds);

      if (observationsError) {
        return toolError(
          ErrorCodes.DATABASE_ERROR,
          'Failed to fetch range session observations.',
        );
      }

      const sessionsWithObservations = new Set(
        (
          (observationRows ?? []) as Array<{
            range_session_id: string;
            observed_values: Record<string, unknown> | null;
          }>
        )
          .filter(o => hasObservedValue(o.observed_values))
          .map(o => o.range_session_id),
      );

      const sessionsOutput = rows.map(row => ({
        id: row.id,
        session_name: row.session_name,
        source_session_id: row.source_session_id,
        started_at: row.started_at,
        completed_at: row.completed_at,
        session_note: row.session_note,
        balls_hit: countBallsHit(row),
        blocks_with_results: blocksWithResults(row.block_results),
        has_observations: sessionsWithObservations.has(row.id),
      }));

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

/**
 * Count completed Ball Steps. In v≥2 snapshots each Ball Step carries one ball
 * (`ballCount === 1`); v1 snapshots are coarse (a step may carry many balls),
 * so ball counting degrades honestly to `null`.
 */
function countBallsHit(row: RangeSessionRow): number | null {
  if (row.snapshot_version < 2) return null;
  const steps = row.snapshot?.steps ?? [];
  const completed = new Set(
    (row.completed_steps ?? []).map(c => c.stepIndex),
  );
  let count = 0;
  for (const stepIndex of completed) {
    if (steps[stepIndex]?.ballCount === 1) count += 1;
  }
  return count;
}

/**
 * True when an observation row carries at least one non-empty string value.
 * Mirrors the per-ball filter in `get_range_session` so an all-empty ('{}' or
 * blank-valued) row — which the schema treats as "unobserved" — never flips
 * `has_observations` on for a session that `get_range_session` shows as empty.
 */
function hasObservedValue(
  observedValues: Record<string, unknown> | null,
): boolean {
  if (!observedValues) return false;
  return Object.values(observedValues).some(
    v => typeof v === 'string' && v.length > 0,
  );
}

/**
 * The numeric block indices that recorded a note or manual count, ascending.
 * An entry with neither is skipped (an empty '{}' result is not "a result"),
 * keeping this aligned with the tool's contract. Non-numeric or negative keys
 * are skipped defensively (never thrown on) — the tool must not brick on data
 * written by a future or buggy app version.
 */
function blocksWithResults(
  blockResults: Record<string, unknown> | null,
): number[] {
  if (!blockResults) return [];
  return Object.entries(blockResults)
    .filter(([k, v]) => {
      const n = Number(k);
      return Number.isInteger(n) && n >= 0 && hasResultContent(v);
    })
    .map(([k]) => Number(k))
    .sort((a, b) => a - b);
}

/** True when a `block_results` entry carries a note string or a numeric count. */
function hasResultContent(entry: unknown): boolean {
  if (typeof entry !== 'object' || entry === null) return false;
  const { note, manualCount } = entry as {
    note?: unknown;
    manualCount?: unknown;
  };
  return (
    (typeof note === 'string' && note.length > 0) ||
    typeof manualCount === 'number'
  );
}
