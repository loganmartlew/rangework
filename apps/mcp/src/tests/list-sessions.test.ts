import { describe, expect, it } from 'vitest';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { InMemoryTransport } from '@modelcontextprotocol/sdk/inMemory.js';
import { createServer } from '../server.js';
import type { UserContext } from '../auth/userContext.js';
import { mockR2Bucket } from './test-helpers.js';

describe('list_sessions tool', () => {
  it('returns sessions with items and computed ball counts', async () => {
    const mockSupabaseClient = {
      from: (table: string) => {
        if (table === 'practice_sessions') {
          return {
            select: () => ({
              order: async () => ({
                data: [
                  {
                    id: 'session-1',
                    name: 'Morning Practice',
                    notes: 'Focus on irons',
                  },
                ],
                error: null,
              }),
            }),
          };
        }
        if (table === 'practice_session_items') {
          return {
            select: () => ({
              in: () => ({
                order: async () => ({
                  data: [
                    {
                      practice_session_id: 'session-1',
                      practice_unit_id: 'unit-1',
                      sort_order: 1,
                      repeat_count: 2,
                      club_code: 'seven_iron',
                      notes: null,
                      focus_cue: 'Smooth tempo',
                    },
                  ],
                  error: null,
                }),
              }),
            }),
          };
        }
        if (table === 'practice_units') {
          return {
            select: () => ({
              in: async () => ({
                data: [{ id: 'unit-1', title: 'Gate Drill' }],
                error: null,
              }),
            }),
          };
        }
        if (table === 'practice_unit_instructions') {
          return {
            select: () => ({
              in: async () => ({
                data: [
                  { practice_unit_id: 'unit-1', ball_count: 5 },
                  { practice_unit_id: 'unit-1', ball_count: 10 },
                ],
                error: null,
              }),
            }),
          };
        }
        return {
          select: () => ({
            order: async () => ({ data: [], error: null }),
            in: async () => ({ data: [], error: null }),
          }),
        };
      },
    } as unknown as UserContext['supabaseClient'];

    const userContext: UserContext = {
      userId: 'test-user',
      supabaseClient: mockSupabaseClient,
    };

    const server = createServer(userContext, mockR2Bucket());
    const [clientTransport, serverTransport] =
      InMemoryTransport.createLinkedPair();
    const client = new Client({ name: 'test-client', version: '1.0.0' });

    await Promise.all([
      server.connect(serverTransport),
      client.connect(clientTransport),
    ]);

    const result = (await client.callTool({
      name: 'list_sessions',
      arguments: {},
    })) as { content: Array<{ type: string; text?: string }> };

    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.sessions).toHaveLength(1);
    expect(parsed.sessions[0]).toMatchObject({
      id: 'session-1',
      name: 'Morning Practice',
      notes: 'Focus on irons',
      total_ball_count: 30, // 2 repeats * 15 balls per unit
      has_uncounted_items: false,
    });
    expect(parsed.sessions[0].items).toEqual([
      {
        order: 1,
        unit_id: 'unit-1',
        unit_title: 'Gate Drill',
        repeat_count: 2,
        club_code: 'seven_iron',
        notes: null,
        focus_cue: 'Smooth tempo',
      },
    ]);
  });

  it('sets total_ball_count to null when any unit has uncounted instructions', async () => {
    const mockSupabaseClient = {
      from: (table: string) => {
        if (table === 'practice_sessions') {
          return {
            select: () => ({
              order: async () => ({
                data: [{ id: 'session-1', name: 'Session', notes: null }],
                error: null,
              }),
            }),
          };
        }
        if (table === 'practice_session_items') {
          return {
            select: () => ({
              in: () => ({
                order: async () => ({
                  data: [
                    {
                      practice_session_id: 'session-1',
                      practice_unit_id: 'unit-1',
                      sort_order: 1,
                      repeat_count: 1,
                      club_code: null,
                      notes: null,
                      focus_cue: null,
                    },
                  ],
                  error: null,
                }),
              }),
            }),
          };
        }
        if (table === 'practice_units') {
          return {
            select: () => ({
              in: async () => ({
                data: [{ id: 'unit-1', title: 'Drill' }],
                error: null,
              }),
            }),
          };
        }
        if (table === 'practice_unit_instructions') {
          return {
            select: () => ({
              in: async () => ({
                data: [
                  { practice_unit_id: 'unit-1', ball_count: 5 },
                  { practice_unit_id: 'unit-1', ball_count: null },
                ],
                error: null,
              }),
            }),
          };
        }
        return {
          select: () => ({
            order: async () => ({ data: [], error: null }),
            in: async () => ({ data: [], error: null }),
          }),
        };
      },
    } as unknown as UserContext['supabaseClient'];

    const userContext: UserContext = {
      userId: 'test-user',
      supabaseClient: mockSupabaseClient,
    };

    const server = createServer(userContext, mockR2Bucket());
    const [clientTransport, serverTransport] =
      InMemoryTransport.createLinkedPair();
    const client = new Client({ name: 'test-client', version: '1.0.0' });

    await Promise.all([
      server.connect(serverTransport),
      client.connect(clientTransport),
    ]);

    const result = (await client.callTool({
      name: 'list_sessions',
      arguments: {},
    })) as { content: Array<{ type: string; text?: string }> };

    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.sessions[0].total_ball_count).toBeNull();
    expect(parsed.sessions[0].has_uncounted_items).toBe(true);
  });

  it('returns empty array when user has no sessions', async () => {
    const mockSupabaseClient = {
      from: () => ({
        select: () => ({
          order: async () => ({ data: [], error: null }),
        }),
      }),
    } as unknown as UserContext['supabaseClient'];

    const userContext: UserContext = {
      userId: 'test-user',
      supabaseClient: mockSupabaseClient,
    };

    const server = createServer(userContext, mockR2Bucket());
    const [clientTransport, serverTransport] =
      InMemoryTransport.createLinkedPair();
    const client = new Client({ name: 'test-client', version: '1.0.0' });

    await Promise.all([
      server.connect(serverTransport),
      client.connect(clientTransport),
    ]);

    const result = (await client.callTool({
      name: 'list_sessions',
      arguments: {},
    })) as { content: Array<{ type: string; text?: string }> };

    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.sessions).toEqual([]);
  });
});
