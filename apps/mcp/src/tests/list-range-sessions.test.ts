import { describe, expect, it } from 'vitest';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { InMemoryTransport } from '@modelcontextprotocol/sdk/inMemory.js';
import { createServer } from '../server.js';
import type { UserContext } from '../auth/userContext.js';
import { mockR2Bucket } from './test-helpers.js';

/**
 * Build a mock Supabase client for `list_range_sessions`.
 *
 * `range_sessions` chain: select → not → is → order → limit (resolves).
 * `range_session_observations` chain: select → in (resolves).
 */
function buildClient(opts: {
  sessions: unknown[];
  sessionsError?: unknown;
  observationSessionIds?: string[];
  observationRows?: Array<{
    range_session_id: string;
    observed_values: Record<string, unknown> | null;
  }>;
  observationsError?: unknown;
  captureLimit?: (limit: number) => void;
}): UserContext['supabaseClient'] {
  return {
    from: (table: string) => {
      if (table === 'range_sessions') {
        return {
          select: () => ({
            not: () => ({
              is: () => ({
                order: () => ({
                  limit: async (limit: number) => {
                    opts.captureLimit?.(limit);
                    return {
                      data: opts.sessionsError ? null : opts.sessions,
                      error: opts.sessionsError ?? null,
                    };
                  },
                }),
              }),
            }),
          }),
        };
      }
      if (table === 'range_session_observations') {
        return {
          select: () => ({
            in: async () => ({
              data: opts.observationsError
                ? null
                : (opts.observationRows ??
                  // Convenience: each id gets one non-empty observation row.
                  (opts.observationSessionIds ?? []).map(id => ({
                    range_session_id: id,
                    observed_values: { shape: 'straight' },
                  }))),
              error: opts.observationsError ?? null,
            }),
          }),
        };
      }
      return { select: () => ({}) };
    },
  } as unknown as UserContext['supabaseClient'];
}

async function callList(
  client: UserContext['supabaseClient'],
  args: Record<string, unknown> = {},
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
  const result = (await client2.callTool({
    name: 'list_range_sessions',
    arguments: args,
  })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };
  return result;
}

describe('list_range_sessions tool', () => {
  it('returns an empty array when there are no completed sessions', async () => {
    const client = buildClient({ sessions: [] });
    const result = await callList(client);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed).toEqual({ sessions: [] });
  });

  it('summarizes a v3 session with balls_hit, blocks_with_results, has_observations', async () => {
    const session = {
      id: 'rs-1',
      session_name: 'Wedge Wednesday',
      source_session_id: 'ps-1',
      started_at: '2026-07-12T09:14:00Z',
      completed_at: '2026-07-12T10:02:00Z',
      session_note: 'Struck it great',
      snapshot_version: 3,
      snapshot: {
        steps: [
          { unitIndex: 0, ballCount: 1 },
          { unitIndex: 0, ballCount: 1 },
          { unitIndex: 1, ballCount: 1 },
          { unitIndex: 1, ballCount: 0 }, // action step, not a ball
        ],
      },
      completed_steps: [{ stepIndex: 0 }, { stepIndex: 1 }, { stepIndex: 3 }],
      block_results: { '0': { note: 'left misses' }, '2': { manualCount: 5 } },
    };
    const client = buildClient({
      sessions: [session],
      observationSessionIds: ['rs-1'],
    });
    const result = await callList(client);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.sessions).toHaveLength(1);
    expect(parsed.sessions[0]).toEqual({
      id: 'rs-1',
      session_name: 'Wedge Wednesday',
      source_session_id: 'ps-1',
      started_at: '2026-07-12T09:14:00Z',
      completed_at: '2026-07-12T10:02:00Z',
      session_note: 'Struck it great',
      balls_hit: 2, // steps 0 and 1 completed with ballCount 1; step 3 is an action step
      blocks_with_results: [0, 2],
      has_observations: true,
    });
  });

  it('reports balls_hit as null for a v1 (coarse) snapshot', async () => {
    const session = {
      id: 'rs-v1',
      session_name: 'Old session',
      source_session_id: null,
      started_at: '2026-01-01T00:00:00Z',
      completed_at: '2026-01-01T01:00:00Z',
      session_note: null,
      snapshot_version: 1,
      snapshot: { steps: [{ unitIndex: 0, ballCount: 20 }] },
      completed_steps: [{ stepIndex: 0 }],
      block_results: {},
    };
    const client = buildClient({ sessions: [session] });
    const result = await callList(client);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.sessions[0].balls_hit).toBeNull();
    expect(parsed.sessions[0].blocks_with_results).toEqual([]);
    expect(parsed.sessions[0].has_observations).toBe(false);
  });

  it('reports has_observations false when every observation row is empty', async () => {
    const session = {
      id: 'rs-empty',
      session_name: 'All-cleared session',
      source_session_id: null,
      started_at: '2026-03-01T00:00:00Z',
      completed_at: '2026-03-01T01:00:00Z',
      session_note: null,
      snapshot_version: 3,
      snapshot: { steps: [{ unitIndex: 0, ballCount: 1 }] },
      completed_steps: [{ stepIndex: 0 }],
      block_results: {},
    };
    const client = buildClient({
      sessions: [session],
      // Rows exist but carry no non-empty value — "unobserved" per the schema.
      observationRows: [
        { range_session_id: 'rs-empty', observed_values: {} },
        { range_session_id: 'rs-empty', observed_values: { shape: '' } },
      ],
    });
    const result = await callList(client);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.sessions[0].has_observations).toBe(false);
  });

  it('excludes empty block_results entries from blocks_with_results', async () => {
    const session = {
      id: 'rs-blocks',
      session_name: 'Empty result entry',
      source_session_id: null,
      started_at: '2026-04-01T00:00:00Z',
      completed_at: '2026-04-01T01:00:00Z',
      session_note: null,
      snapshot_version: 3,
      snapshot: { steps: [{ unitIndex: 0, ballCount: 1 }] },
      completed_steps: [{ stepIndex: 0 }],
      block_results: {
        '0': { note: 'real note' },
        '1': {}, // no note or count — not a recorded result
        '2': { manualCount: 4 },
      },
    };
    const client = buildClient({ sessions: [session] });
    const result = await callList(client);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.sessions[0].blocks_with_results).toEqual([0, 2]);
  });

  it('defaults the limit to 20 and honors an explicit limit', async () => {
    let capturedDefault = -1;
    const client1 = buildClient({
      sessions: [],
      captureLimit: l => (capturedDefault = l),
    });
    await callList(client1);
    expect(capturedDefault).toBe(20);

    let capturedExplicit = -1;
    const client2 = buildClient({
      sessions: [],
      captureLimit: l => (capturedExplicit = l),
    });
    await callList(client2, { limit: 50 });
    expect(capturedExplicit).toBe(50);
  });

  it('returns a DATABASE_ERROR when the session query fails', async () => {
    const client = buildClient({
      sessions: [],
      sessionsError: { message: 'boom' },
    });
    const result = await callList(client);
    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('DATABASE_ERROR');
  });
});
