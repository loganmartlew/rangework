import { describe, expect, it } from 'vitest';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { InMemoryTransport } from '@modelcontextprotocol/sdk/inMemory.js';
import { createServer } from '../server.js';
import type { UserContext } from '../auth/userContext.js';
import { mockR2Bucket } from './test-helpers.js';

/**
 * Build a mock Supabase client for `get_range_session`.
 *
 * `range_sessions` chain: select → eq → not → is → maybeSingle (resolves).
 * `range_session_observations` chain: select → eq → order (resolves).
 */
function buildClient(opts: {
  session: unknown;
  sessionError?: unknown;
  observations?: unknown[];
  observationsError?: unknown;
}): UserContext['supabaseClient'] {
  return {
    from: (table: string) => {
      if (table === 'range_sessions') {
        return {
          select: () => ({
            eq: () => ({
              not: () => ({
                is: () => ({
                  maybeSingle: async () => ({
                    data: opts.sessionError ? null : opts.session,
                    error: opts.sessionError ?? null,
                  }),
                }),
              }),
            }),
          }),
        };
      }
      if (table === 'range_session_observations') {
        return {
          select: () => ({
            eq: () => ({
              order: async () => ({
                data: opts.observationsError ? null : (opts.observations ?? []),
                error: opts.observationsError ?? null,
              }),
            }),
          }),
        };
      }
      return { select: () => ({}) };
    },
  } as unknown as UserContext['supabaseClient'];
}

async function callGet(
  client: UserContext['supabaseClient'],
  args: Record<string, unknown>,
) {
  const userContext: UserContext = {
    userId: 'test-user',
    supabaseClient: client,
  };
  const server = createServer(userContext, mockR2Bucket());
  const [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair();
  const client2 = new Client({ name: 'test-client', version: '1.0.0' });
  await Promise.all([
    server.connect(serverTransport),
    client2.connect(clientTransport),
  ]);
  return (await client2.callTool({
    name: 'get_range_session',
    arguments: args,
  })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };
}

describe('get_range_session tool', () => {
  it('returns RANGE_SESSION_NOT_FOUND when no row resolves', async () => {
    const client = buildClient({ session: null });
    const result = await callGet(client, { range_session_id: 'missing' });
    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('RANGE_SESSION_NOT_FOUND');
  });

  it('assembles a v3 session with aggregates, raw balls, and results', async () => {
    const session = {
      id: 'rs-1',
      session_name: 'Wedge Wednesday',
      started_at: '2026-07-12T09:14:00Z',
      completed_at: '2026-07-12T10:02:00Z',
      session_note: 'Struck it great until the last block',
      snapshot_version: 3,
      snapshot: {
        sessionNotes: 'Tournament prep',
        units: [
          {
            unitTitle: '60m wedge ladder',
            itemFocusCue: 'Hinge earlier',
            successCriterion: 'inside 5m of the 60m flag',
            observationTypes: ['success', 'shape'],
          },
        ],
        steps: [
          { unitIndex: 0, ballCount: 1 },
          { unitIndex: 0, ballCount: 1 },
          { unitIndex: 0, ballCount: 1 },
        ],
      },
      completed_steps: [{ stepIndex: 0 }, { stepIndex: 1 }],
      block_results: { '0': { note: 'left misses all day' } },
    };
    const observations = [
      { step_index: 0, observed_values: { success: 'hit', shape: 'straight' } },
      { step_index: 1, observed_values: { success: 'miss' } },
      { step_index: 2, observed_values: { success: 'hit', shape: 'fade' } },
    ];
    const client = buildClient({ session, observations });
    const result = await callGet(client, { range_session_id: 'rs-1' });
    expect(result.isError).toBeFalsy();
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');

    expect(parsed).toMatchObject({
      id: 'rs-1',
      session_name: 'Wedge Wednesday',
      session_note: 'Struck it great until the last block',
      planning_notes: 'Tournament prep',
    });
    expect(parsed.blocks).toHaveLength(1);
    const block = parsed.blocks[0];
    expect(block).toMatchObject({
      block_index: 0,
      unit_title: '60m wedge ladder',
      focus_cue: 'Hinge earlier',
      success_criterion: 'inside 5m of the 60m flag',
      observation_types: ['success', 'shape'],
      balls_planned: 3,
      balls_hit: 2,
      result: { note: 'left misses all day', manual_count: null },
    });
    expect(block.observations).toEqual({
      success: { observed: 3, counts: { hit: 2, miss: 1 } },
      shape: { observed: 2, counts: { straight: 1, fade: 1 } },
    });
    expect(block.balls).toEqual([
      { step_index: 0, values: { success: 'hit', shape: 'straight' } },
      { step_index: 1, values: { success: 'miss' } },
      { step_index: 2, values: { success: 'hit', shape: 'fade' } },
    ]);
  });

  it('emits null criterion, empty types, and null ball counts for a v1 snapshot', async () => {
    const session = {
      id: 'rs-v1',
      session_name: 'Old session',
      started_at: '2026-01-01T00:00:00Z',
      completed_at: '2026-01-01T01:00:00Z',
      session_note: null,
      snapshot_version: 1,
      snapshot: {
        sessionNotes: null,
        units: [{ unitTitle: 'Legacy drill' }],
        steps: [{ unitIndex: 0, ballCount: 20 }],
      },
      completed_steps: [{ stepIndex: 0 }],
      block_results: {},
    };
    const client = buildClient({ session, observations: [] });
    const result = await callGet(client, { range_session_id: 'rs-v1' });
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    const block = parsed.blocks[0];
    expect(block.success_criterion).toBeNull();
    expect(block.observation_types).toEqual([]);
    expect(block.balls_planned).toBeNull();
    expect(block.balls_hit).toBeNull();
    expect(block.result).toBeNull();
    expect(block.observations).toEqual({});
    expect(block.balls).toEqual([]);
  });

  it('computes ball counts for a v2 snapshot (no criterion/types keys)', async () => {
    const session = {
      id: 'rs-v2',
      session_name: 'v2 session',
      started_at: '2026-05-01T00:00:00Z',
      completed_at: '2026-05-01T01:00:00Z',
      session_note: null,
      snapshot_version: 2,
      snapshot: {
        units: [{ unitTitle: 'Drill', unitFocus: 'Tempo' }],
        steps: [
          { unitIndex: 0, ballCount: 1 },
          { unitIndex: 0, ballCount: 1 },
        ],
      },
      completed_steps: [{ stepIndex: 0 }],
      block_results: {},
    };
    const client = buildClient({ session, observations: [] });
    const result = await callGet(client, { range_session_id: 'rs-v2' });
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    const block = parsed.blocks[0];
    expect(block.balls_planned).toBe(2);
    expect(block.balls_hit).toBe(1);
    expect(block.success_criterion).toBeNull();
    expect(block.observation_types).toEqual([]);
    expect(block.focus_cue).toBe('Tempo'); // falls back to unitFocus
  });

  it('omits empty-value observation records and skips unresolvable step indices', async () => {
    const session = {
      id: 'rs-edge',
      session_name: 'Edge session',
      started_at: '2026-06-01T00:00:00Z',
      completed_at: '2026-06-01T01:00:00Z',
      session_note: null,
      snapshot_version: 3,
      snapshot: {
        units: [{ unitTitle: 'Drill', successCriterion: 'target', observationTypes: ['shape'] }],
        steps: [{ unitIndex: 0, ballCount: 1 }],
      },
      completed_steps: [],
      block_results: {},
    };
    const observations = [
      { step_index: 0, observed_values: { shape: 'straight' } },
      { step_index: 0, observed_values: {} }, // empty record — omitted (same step id can't really repeat, but proves the filter)
      { step_index: 99, observed_values: { shape: 'fade' } }, // unresolvable — skipped
    ];
    const client = buildClient({ session, observations });
    const result = await callGet(client, { range_session_id: 'rs-edge' });
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    const block = parsed.blocks[0];
    expect(block.balls).toEqual([
      { step_index: 0, values: { shape: 'straight' } },
    ]);
    expect(block.observations).toEqual({
      shape: { observed: 1, counts: { straight: 1 } },
    });
  });

  it('maps a count-only block result with a null note half', async () => {
    const session = {
      id: 'rs-count',
      session_name: 'Count session',
      started_at: '2026-06-01T00:00:00Z',
      completed_at: '2026-06-01T01:00:00Z',
      session_note: null,
      snapshot_version: 3,
      snapshot: {
        units: [{ unitTitle: 'Drill' }],
        steps: [{ unitIndex: 0, ballCount: 1 }],
      },
      completed_steps: [],
      block_results: { '0': { manualCount: 7 } },
    };
    const client = buildClient({ session, observations: [] });
    const result = await callGet(client, { range_session_id: 'rs-count' });
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.blocks[0].result).toEqual({ note: null, manual_count: 7 });
  });

  it('maps a malformed-uuid DB error (22P02) to RANGE_SESSION_NOT_FOUND', async () => {
    const client = buildClient({
      session: null,
      sessionError: { code: '22P02', message: 'invalid input syntax for type uuid' },
    });
    const result = await callGet(client, { range_session_id: 'not-a-uuid' });
    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('RANGE_SESSION_NOT_FOUND');
  });

  it('returns DATABASE_ERROR for a non-22P02 session query failure', async () => {
    const client = buildClient({
      session: null,
      sessionError: { code: '08006', message: 'connection failure' },
    });
    const result = await callGet(client, { range_session_id: 'rs-1' });
    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('DATABASE_ERROR');
  });

  it('rejects an empty range_session_id', async () => {
    const client = buildClient({ session: null });
    const result = await callGet(client, { range_session_id: '  ' });
    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('VALIDATION_ERROR');
  });
});
