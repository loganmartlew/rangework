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
          // create_session awaits select('id') directly — no .order() chain
          return {
            select: () => Promise.resolve({
              data: [{ id: 'unit-1' }, { id: 'unit-2' }],
              error: null,
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
            select: () => Promise.resolve({ data: [{ id: 'unit-1' }], error: null }),
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
            select: () => Promise.resolve({ data: [{ id: 'unit-1' }], error: null }),
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
            select: () => Promise.resolve({
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
          // create_session awaits select('id') directly — no .order() chain
          return {
            select: () => Promise.resolve({
              data: [{ id: 'unit-1' }],
              error: null,
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
});
