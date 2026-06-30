import { describe, expect, it } from 'vitest';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { InMemoryTransport } from '@modelcontextprotocol/sdk/inMemory.js';
import { createServer } from '../server.js';
import type { UserContext } from '../auth/userContext.js';
import { mockR2Bucket } from './test-helpers.js';

describe('list_units tool', () => {
  it('returns units with full instructions and computed ball counts', async () => {
    const mockSupabaseClient = {
      from: (table: string) => {
        if (table === 'practice_units') {
          return {
            select: () => ({
              order: async () => ({
                data: [
                  {
                    id: 'unit-1',
                    title: 'Gate Drill',
                    notes: 'Use alignment stick',
                    focus: 'Square face',
                    default_club_code: 'seven_iron',
                  },
                ],
                error: null,
              }),
            }),
          };
        }
        if (table === 'practice_unit_instructions') {
          return {
            select: () => ({
              in: () => ({
                order: async () => ({
                  data: [
                    {
                      practice_unit_id: 'unit-1',
                      sort_order: 1,
                      text: 'Set up gate',
                      ball_count: 5,
                    },
                    {
                      practice_unit_id: 'unit-1',
                      sort_order: 2,
                      text: 'Hit through gate',
                      ball_count: 10,
                    },
                  ],
                  error: null,
                }),
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
      name: 'list_units',
      arguments: {},
    })) as { content: Array<{ type: string; text?: string }> };

    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.units).toHaveLength(1);
    expect(parsed.units[0]).toMatchObject({
      id: 'unit-1',
      title: 'Gate Drill',
      notes: 'Use alignment stick',
      focus: 'Square face',
      default_club_code: 'seven_iron',
      instruction_count: 2,
      total_ball_count: 15,
      has_uncounted_instructions: false,
    });
    expect(parsed.units[0].instructions).toEqual([
      { order: 1, text: 'Set up gate', ball_count: 5 },
      { order: 2, text: 'Hit through gate', ball_count: 10 },
    ]);
  });

  it('sets total_ball_count to null when any instruction has no ball_count', async () => {
    const mockSupabaseClient = {
      from: (table: string) => {
        if (table === 'practice_units') {
          return {
            select: () => ({
              order: async () => ({
                data: [
                  {
                    id: 'unit-1',
                    title: 'Drill',
                    notes: null,
                    focus: null,
                    default_club_code: null,
                  },
                ],
                error: null,
              }),
            }),
          };
        }
        if (table === 'practice_unit_instructions') {
          return {
            select: () => ({
              in: () => ({
                order: async () => ({
                  data: [
                    {
                      practice_unit_id: 'unit-1',
                      sort_order: 1,
                      text: 'Step 1',
                      ball_count: 5,
                    },
                    {
                      practice_unit_id: 'unit-1',
                      sort_order: 2,
                      text: 'Step 2',
                      ball_count: null,
                    },
                  ],
                  error: null,
                }),
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
      name: 'list_units',
      arguments: {},
    })) as { content: Array<{ type: string; text?: string }> };

    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.units[0].total_ball_count).toBeNull();
    expect(parsed.units[0].has_uncounted_instructions).toBe(true);
  });

  it('counts a 0 ball_count instruction and does not flag it as uncounted', async () => {
    const mockSupabaseClient = {
      from: (table: string) => {
        if (table === 'practice_units') {
          return {
            select: () => ({
              order: async () => ({
                data: [
                  {
                    id: 'unit-1',
                    title: 'Feel work',
                    notes: null,
                    focus: null,
                    default_club_code: null,
                  },
                ],
                error: null,
              }),
            }),
          };
        }
        if (table === 'practice_unit_instructions') {
          return {
            select: () => ({
              in: () => ({
                order: async () => ({
                  data: [
                    {
                      practice_unit_id: 'unit-1',
                      sort_order: 1,
                      text: 'Five practice swings',
                      ball_count: 0,
                    },
                    {
                      practice_unit_id: 'unit-1',
                      sort_order: 2,
                      text: 'Hit 10 wedges',
                      ball_count: 10,
                    },
                  ],
                  error: null,
                }),
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
      name: 'list_units',
      arguments: {},
    })) as { content: Array<{ type: string; text?: string }> };

    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.units[0].has_uncounted_instructions).toBe(false);
    expect(parsed.units[0].total_ball_count).toBe(10);
  });

  it('returns empty array when user has no units', async () => {
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
      name: 'list_units',
      arguments: {},
    })) as { content: Array<{ type: string; text?: string }> };

    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.units).toEqual([]);
  });
});
