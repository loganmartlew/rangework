import { describe, expect, it } from 'vitest';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { InMemoryTransport } from '@modelcontextprotocol/sdk/inMemory.js';
import { createServer } from '../server.js';
import type { UserContext } from '../auth/userContext.js';
import { mockR2Bucket } from './test-helpers.js';

describe('create_session tool', () => {
  it('creates a session and returns session_id', async () => {
    const mockSupabaseClient = {
      from: (table: string) => {
        if (table === 'practice_units') {
          // create_session awaits select('id').is(...) directly — no .order() chain
          return {
            select: () => ({
              is: () =>
                Promise.resolve({
                  data: [{ id: 'unit-1' }, { id: 'unit-2' }],
                  error: null,
                }),
            }),
          };
        }
        if (table === 'clubs') {
          return {
            select: () => ({
              order: async () => ({
                data: [{ code: 'driver' }, { code: 'seven_iron' }],
                error: null,
              }),
            }),
          };
        }
        return {
          select: () => ({ order: async () => ({ data: [], error: null }) }),
        };
      },
      rpc: async (name: string, params: Record<string, unknown>) => {
        if (name === 'save_practice_session') {
          expect(params.p_name).toBe('Morning Practice');
          expect(params.p_items).toHaveLength(2);
          return { error: null };
        }
        return { error: { message: 'Unknown RPC' } };
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
      name: 'create_session',
      arguments: {
        name: 'Morning Practice',
        items: [
          {
            practice_unit_id: 'unit-1',
            order: 1,
            repeat_count: 2,
            club_code: 'seven_iron',
          },
          {
            practice_unit_id: 'unit-2',
            order: 2,
            repeat_count: 1,
          },
        ],
      },
    })) as { content: Array<{ type: string; text?: string }> };

    expect(result.content).toHaveLength(1);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.session_id).toBeDefined();
    expect(typeof parsed.session_id).toBe('string');
  });

  it('rejects empty name', async () => {
    const mockSupabaseClient = {
      from: () => ({
        select: () => ({ order: async () => ({ data: [], error: null }) }),
      }),
      rpc: async () => ({ error: null }),
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
      name: 'create_session',
      arguments: {
        name: '   ',
        items: [{ practice_unit_id: 'unit-1', order: 1, repeat_count: 1 }],
      },
    })) as {
      content: Array<{ type: string; text?: string }>;
      isError?: boolean;
    };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('VALIDATION_ERROR');
    expect(parsed.message).toContain('name');
  });

  it('rejects duplicate order values', async () => {
    const mockSupabaseClient = {
      from: (table: string) => {
        if (table === 'practice_units') {
          return {
            select: () => ({
              is: () =>
                Promise.resolve({ data: [{ id: 'unit-1' }], error: null }),
            }),
          };
        }
        return {
          select: () => ({ order: async () => ({ data: [], error: null }) }),
        };
      },
      rpc: async () => ({ error: null }),
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
      name: 'create_session',
      arguments: {
        name: 'Session',
        items: [
          { practice_unit_id: 'unit-1', order: 1, repeat_count: 1 },
          { practice_unit_id: 'unit-1', order: 1, repeat_count: 2 },
        ],
      },
    })) as {
      content: Array<{ type: string; text?: string }>;
      isError?: boolean;
    };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('VALIDATION_ERROR');
    expect(parsed.message).toContain('unique');
  });

  it('rejects unknown unit id', async () => {
    const mockSupabaseClient = {
      from: (table: string) => {
        if (table === 'practice_units') {
          return {
            select: () => ({
              is: () =>
                Promise.resolve({ data: [{ id: 'unit-1' }], error: null }),
            }),
          };
        }
        return {
          select: () => ({ order: async () => ({ data: [], error: null }) }),
        };
      },
      rpc: async () => ({ error: null }),
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
      name: 'create_session',
      arguments: {
        name: 'Session',
        items: [
          { practice_unit_id: 'unit-1', order: 1, repeat_count: 1 },
          { practice_unit_id: 'unit-999', order: 2, repeat_count: 1 },
        ],
      },
    })) as {
      content: Array<{ type: string; text?: string }>;
      isError?: boolean;
    };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('UNIT_NOT_FOUND');
    expect(parsed.data.invalid_unit_ids).toContain('unit-999');
  });

  it('strips per-item overrides that equal the unit base values', async () => {
    let capturedItems: Array<Record<string, unknown>> = [];
    const mockSupabaseClient = {
      from: (table: string) => {
        if (table === 'practice_units') {
          return {
            select: () => ({
              is: () =>
                Promise.resolve({
                  data: [
                    {
                      id: 'unit-1',
                      notes: 'Use an alignment stick',
                      focus: 'Tempo',
                      default_club_code: 'seven_iron',
                    },
                  ],
                  error: null,
                }),
            }),
          };
        }
        if (table === 'clubs') {
          return {
            select: () => ({
              order: async () => ({
                data: [{ code: 'driver' }, { code: 'seven_iron' }],
                error: null,
              }),
            }),
          };
        }
        return {
          select: () => ({ order: async () => ({ data: [], error: null }) }),
        };
      },
      rpc: async (name: string, params: Record<string, unknown>) => {
        if (name === 'save_practice_session') {
          capturedItems = params.p_items as Array<Record<string, unknown>>;
          return { error: null };
        }
        return { error: { message: 'Unknown RPC' } };
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
      name: 'create_session',
      arguments: {
        name: 'Session',
        items: [
          {
            practice_unit_id: 'unit-1',
            order: 1,
            repeat_count: 1,
            // Copies of the unit's base values — all should be stripped.
            club_code: 'seven_iron',
            notes: 'Use an alignment stick',
            focus_cue: 'Tempo',
          },
          {
            practice_unit_id: 'unit-1',
            order: 2,
            repeat_count: 1,
            // Genuine overrides — all should survive.
            club_code: 'driver',
            notes: 'Use the 50y stake',
            focus_cue: 'Hinge earlier',
          },
        ],
      },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBeUndefined();
    expect(capturedItems).toHaveLength(2);
    expect(capturedItems[0]).toEqual({
      practice_unit_id: 'unit-1',
      order: 1,
      repeat_count: 1,
    });
    expect(capturedItems[1]).toEqual({
      practice_unit_id: 'unit-1',
      order: 2,
      repeat_count: 1,
      club_code: 'driver',
      notes: 'Use the 50y stake',
      focus_cue: 'Hinge earlier',
    });
  });

  it('rejects unknown club code', async () => {
    const mockSupabaseClient = {
      from: (table: string) => {
        if (table === 'practice_units') {
          // create_session awaits select('id').is(...) directly — no .order() chain
          return {
            select: () => ({
              is: () =>
                Promise.resolve({
                  data: [{ id: 'unit-1' }],
                  error: null,
                }),
            }),
          };
        }
        if (table === 'clubs') {
          return {
            select: () => ({
              order: async () => ({
                data: [{ code: 'driver' }, { code: 'seven_iron' }],
                error: null,
              }),
            }),
          };
        }
        return {
          select: () => ({ order: async () => ({ data: [], error: null }) }),
        };
      },
      rpc: async () => ({ error: null }),
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
      name: 'create_session',
      arguments: {
        name: 'Session',
        items: [
          {
            practice_unit_id: 'unit-1',
            order: 1,
            repeat_count: 1,
            club_code: 'invalid_club',
          },
        ],
      },
    })) as {
      content: Array<{ type: string; text?: string }>;
      isError?: boolean;
    };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('UNKNOWN_CLUB_CODE');
    expect(parsed.data.valid_codes).toBeDefined();
  });

  it('forwards observation_types (deduped) when the unit has a criterion', async () => {
    let capturedItems: Array<Record<string, unknown>> = [];
    const mockSupabaseClient = {
      from: (table: string) => {
        if (table === 'practice_units') {
          return {
            select: () => ({
              is: () =>
                Promise.resolve({
                  data: [
                    {
                      id: 'unit-1',
                      notes: null,
                      focus: null,
                      default_club_code: null,
                      success_criterion: 'inside 5m of the flag',
                    },
                  ],
                  error: null,
                }),
            }),
          };
        }
        return {
          select: () => ({ order: async () => ({ data: [], error: null }) }),
        };
      },
      rpc: async (name: string, params: Record<string, unknown>) => {
        if (name === 'save_practice_session') {
          capturedItems = params.p_items as Array<Record<string, unknown>>;
          return { error: null };
        }
        return { error: { message: 'Unknown RPC' } };
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
      name: 'create_session',
      arguments: {
        name: 'Session',
        items: [
          {
            practice_unit_id: 'unit-1',
            order: 1,
            repeat_count: 1,
            observation_types: ['success', 'shape', 'shape'],
          },
        ],
      },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBeFalsy();
    expect(capturedItems[0]?.observation_types).toEqual(['success', 'shape']);
  });

  it('omits observation_types entirely when the array is empty', async () => {
    let capturedItems: Array<Record<string, unknown>> = [];
    const mockSupabaseClient = {
      from: (table: string) => {
        if (table === 'practice_units') {
          return {
            select: () => ({
              is: () =>
                Promise.resolve({
                  data: [
                    { id: 'unit-1', notes: null, focus: null, default_club_code: null },
                  ],
                  error: null,
                }),
            }),
          };
        }
        return {
          select: () => ({ order: async () => ({ data: [], error: null }) }),
        };
      },
      rpc: async (name: string, params: Record<string, unknown>) => {
        if (name === 'save_practice_session') {
          capturedItems = params.p_items as Array<Record<string, unknown>>;
          return { error: null };
        }
        return { error: { message: 'Unknown RPC' } };
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
      name: 'create_session',
      arguments: {
        name: 'Session',
        items: [
          {
            practice_unit_id: 'unit-1',
            order: 1,
            repeat_count: 1,
            observation_types: [],
          },
        ],
      },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBeFalsy();
    expect('observation_types' in (capturedItems[0] ?? {})).toBe(false);
  });

  it("rejects enabling 'success' on a unit without a success_criterion", async () => {
    const mockSupabaseClient = {
      from: (table: string) => {
        if (table === 'practice_units') {
          return {
            select: () => ({
              is: () =>
                Promise.resolve({
                  data: [
                    {
                      id: 'unit-1',
                      notes: null,
                      focus: null,
                      default_club_code: null,
                      success_criterion: null,
                    },
                  ],
                  error: null,
                }),
            }),
          };
        }
        return {
          select: () => ({ order: async () => ({ data: [], error: null }) }),
        };
      },
      rpc: async () => ({ error: null }),
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
      name: 'create_session',
      arguments: {
        name: 'Session',
        items: [
          {
            practice_unit_id: 'unit-1',
            order: 1,
            repeat_count: 1,
            observation_types: ['success'],
          },
        ],
      },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('VALIDATION_ERROR');
    expect(parsed.message).toContain('success_criterion');
    expect(parsed.data.field).toBe('items[0].observation_types');
  });

  it('rejects an unknown observation type', async () => {
    const mockSupabaseClient = {
      from: (table: string) => {
        if (table === 'practice_units') {
          return {
            select: () => ({
              is: () =>
                Promise.resolve({
                  data: [
                    { id: 'unit-1', notes: null, focus: null, default_club_code: null },
                  ],
                  error: null,
                }),
            }),
          };
        }
        return {
          select: () => ({ order: async () => ({ data: [], error: null }) }),
        };
      },
      rpc: async () => ({ error: null }),
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
      name: 'create_session',
      arguments: {
        name: 'Session',
        items: [
          {
            practice_unit_id: 'unit-1',
            order: 1,
            repeat_count: 1,
            observation_types: ['spin'],
          },
        ],
      },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('VALIDATION_ERROR');
    expect(parsed.message).toContain('spin');
    expect(parsed.data.field).toBe('items[0].observation_types');
  });

  it('rejects an item with neither practice_unit_id nor inline_unit', async () => {
    const mockSupabaseClient = {
      from: () => ({
        select: () => ({ order: async () => ({ data: [], error: null }) }),
      }),
      rpc: async () => ({ error: null }),
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
      name: 'create_session',
      arguments: {
        name: 'Session',
        items: [{ order: 1, repeat_count: 1 }],
      },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('VALIDATION_ERROR');
    expect(parsed.data.field).toBe('items[0]');
  });

  it('rejects an item with both practice_unit_id and inline_unit', async () => {
    const mockSupabaseClient = {
      from: () => ({
        select: () => ({ order: async () => ({ data: [], error: null }) }),
      }),
      rpc: async () => ({ error: null }),
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
      name: 'create_session',
      arguments: {
        name: 'Session',
        items: [
          {
            practice_unit_id: 'unit-1',
            inline_unit: {
              title: 'Gate drill',
              instructions: [{ order: 1, text: 'Hit balls', ball_count: 5 }],
            },
            order: 1,
            repeat_count: 1,
          },
        ],
      },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('VALIDATION_ERROR');
    expect(parsed.data.field).toBe('items[0]');
  });

  it('builds an inline_unit RPC payload and strips duplicate inline item overrides', async () => {
    let capturedItems: Array<Record<string, unknown>> = [];
    const mockSupabaseClient = {
      from: (table: string) => {
        if (table === 'practice_units') {
          return {
            select: () => ({
              is: () =>
                Promise.resolve({
                  data: [
                    {
                      id: 'unit-1',
                      notes: null,
                      focus: null,
                      default_club_code: null,
                      success_criterion: null,
                    },
                  ],
                  error: null,
                }),
            }),
          };
        }
        if (table === 'clubs') {
          return {
            select: () => ({
              order: async () => ({
                data: [{ code: 'seven_iron' }],
                error: null,
              }),
            }),
          };
        }
        return {
          select: () => ({ order: async () => ({ data: [], error: null }) }),
        };
      },
      rpc: async (name: string, params: Record<string, unknown>) => {
        if (name === 'save_practice_session') {
          capturedItems = params.p_items as Array<Record<string, unknown>>;
          return { error: null };
        }
        return { error: { message: 'Unknown RPC' } };
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
      name: 'create_session',
      arguments: {
        name: 'Session',
        items: [
          {
            practice_unit_id: 'unit-1',
            order: 1,
            repeat_count: 1,
          },
          {
            inline_unit: {
              title: 'Gate drill',
              instructions: [
                { order: 1, text: 'Set up gate', ball_count: 5 },
                { order: 2, text: 'Hit through gate', ball_count: 10 },
              ],
              focus: 'Square face',
              notes: 'Use an alignment stick',
              default_club_code: 'seven_iron',
            },
            order: 2,
            repeat_count: 1,
            // These repeat the inline unit's bases, so they must not become
            // stored item overrides that would later pin stale values.
            club_code: 'seven_iron',
            notes: 'Use an alignment stick',
            focus_cue: 'Square face',
          },
        ],
      },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBeFalsy();
    expect(capturedItems).toHaveLength(2);
    expect(capturedItems[0]).toEqual({
      practice_unit_id: 'unit-1',
      order: 1,
      repeat_count: 1,
    });
    expect(capturedItems[1]).toEqual({
      order: 2,
      repeat_count: 1,
      inline_unit: {
        title: 'Gate drill',
        instructions: [
          { order: 1, text: 'Set up gate', ball_count: 5 },
          { order: 2, text: 'Hit through gate', ball_count: 10 },
        ],
        notes: 'Use an alignment stick',
        focus: 'Square face',
        default_club_code: 'seven_iron',
        success_criterion: null,
        tag_ids: [],
      },
    });
  });

  it('inline success observation reads the embedded success_criterion', async () => {
    let capturedItems: Array<Record<string, unknown>> = [];
    const mockSupabaseClient = {
      from: (table: string) => {
        if (table === 'practice_units') {
          return {
            select: () => ({
              is: () => Promise.resolve({ data: [], error: null }),
            }),
          };
        }
        return {
          select: () => ({ order: async () => ({ data: [], error: null }) }),
        };
      },
      rpc: async (name: string, params: Record<string, unknown>) => {
        if (name === 'save_practice_session') {
          capturedItems = params.p_items as Array<Record<string, unknown>>;
          return { error: null };
        }
        return { error: { message: 'Unknown RPC' } };
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
      name: 'create_session',
      arguments: {
        name: 'Session',
        items: [
          {
            inline_unit: {
              title: 'Gate drill',
              instructions: [{ order: 1, text: 'Hit to the flag', ball_count: 10 }],
              success_criterion: 'inside 5m of the flag',
            },
            order: 1,
            repeat_count: 1,
            observation_types: ['success'],
          },
        ],
      },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBeFalsy();
    expect(capturedItems[0]?.observation_types).toEqual(['success']);
  });

  it("rejects 'success' on an inline item with no embedded success_criterion", async () => {
    const mockSupabaseClient = {
      from: (table: string) => {
        if (table === 'practice_units') {
          return {
            select: () => ({
              is: () => Promise.resolve({ data: [], error: null }),
            }),
          };
        }
        return {
          select: () => ({ order: async () => ({ data: [], error: null }) }),
        };
      },
      rpc: async () => ({ error: null }),
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
      name: 'create_session',
      arguments: {
        name: 'Session',
        items: [
          {
            inline_unit: {
              title: 'Gate drill',
              instructions: [{ order: 1, text: 'Hit to the flag', ball_count: 10 }],
            },
            order: 1,
            repeat_count: 1,
            observation_types: ['success'],
          },
        ],
      },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('VALIDATION_ERROR');
    expect(parsed.message).toContain('success_criterion');
    expect(parsed.data.field).toBe('items[0].observation_types');
  });

  it('rejects invalid fields inside an inline_unit, scoped to the item', async () => {
    const mockSupabaseClient = {
      from: (table: string) => {
        if (table === 'practice_units') {
          return {
            select: () => ({
              is: () => Promise.resolve({ data: [], error: null }),
            }),
          };
        }
        return {
          select: () => ({ order: async () => ({ data: [], error: null }) }),
        };
      },
      rpc: async () => ({ error: null }),
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
      name: 'create_session',
      arguments: {
        name: 'Session',
        items: [
          {
            inline_unit: {
              title: '   ',
              instructions: [{ order: 1, text: 'Hit balls', ball_count: 5 }],
            },
            order: 1,
            repeat_count: 1,
          },
        ],
      },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('VALIDATION_ERROR');
    expect(parsed.data.field).toBe('items[0].inline_unit.title');
  });

  it("rejects an inline unit's id used as a practice_unit_id reference (excluded from the owned-units pre-fetch)", async () => {
    const mockSupabaseClient = {
      from: (table: string) => {
        if (table === 'practice_units') {
          // The .is('scoped_to_session_id', null) filter excludes inline
          // units — the mock simulates that by simply not returning the
          // inline unit's id in the owned set.
          return {
            select: () => ({
              is: () =>
                Promise.resolve({ data: [{ id: 'unit-1' }], error: null }),
            }),
          };
        }
        return {
          select: () => ({ order: async () => ({ data: [], error: null }) }),
        };
      },
      rpc: async () => ({ error: null }),
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
      name: 'create_session',
      arguments: {
        name: 'Session',
        items: [
          { practice_unit_id: 'inline-unit-1', order: 1, repeat_count: 1 },
        ],
      },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('UNIT_NOT_FOUND');
    expect(parsed.data.invalid_unit_ids).toContain('inline-unit-1');
  });
});
