import { describe, expect, it } from 'vitest';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { InMemoryTransport } from '@modelcontextprotocol/sdk/inMemory.js';
import { createServer } from '../server.js';
import type { UserContext } from '../auth/userContext.js';
import { mockR2Bucket } from './test-helpers.js';

describe('create_unit tool', () => {
  it('creates a unit and returns unit_id', async () => {
    let rpcCalled = false;
    const mockSupabaseClient = {
      from: () => ({
        select: () => ({
          order: async () => ({ data: [], error: null }),
        }),
      }),
      rpc: async (name: string, params: Record<string, unknown>) => {
        if (name === 'save_practice_unit') {
          rpcCalled = true;
          expect(params.p_title).toBe('Gate Drill');
          expect(params.p_instructions).toHaveLength(2);
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
      name: 'create_unit',
      arguments: {
        title: 'Gate Drill',
        instructions: [
          { order: 1, text: 'Set up gate', ball_count: 5 },
          { order: 2, text: 'Hit through gate', ball_count: 10 },
        ],
      },
    })) as { content: Array<{ type: string; text?: string }> };

    expect(rpcCalled).toBe(true);
    expect(result.content).toHaveLength(1);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.unit_id).toBeDefined();
    expect(typeof parsed.unit_id).toBe('string');
  });

  it('rejects empty title', async () => {
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
      name: 'create_unit',
      arguments: {
        title: '   ',
        instructions: [{ order: 1, text: 'Step 1' }],
      },
    })) as {
      content: Array<{ type: string; text?: string }>;
      isError?: boolean;
    };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('VALIDATION_ERROR');
    expect(parsed.message).toContain('title');
  });

  it('rejects more than 10 instructions', async () => {
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

    const instructions = Array.from({ length: 11 }, (_, i) => ({
      order: i + 1,
      text: `Step ${i + 1}`,
    }));

    const result = (await client.callTool({
      name: 'create_unit',
      arguments: {
        title: 'Drill',
        instructions,
      },
    })) as {
      content: Array<{ type: string; text?: string }>;
      isError?: boolean;
    };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('VALIDATION_ERROR');
  });

  it('rejects duplicate order values', async () => {
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
      name: 'create_unit',
      arguments: {
        title: 'Drill',
        instructions: [
          { order: 1, text: 'Step 1' },
          { order: 1, text: 'Step 2' },
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

  it('accepts and forwards a per-instruction club_code', async () => {
    let forwardedInstructions: Array<Record<string, unknown>> = [];
    const mockSupabaseClient = {
      from: (table: string) => {
        if (table === 'clubs') {
          return {
            select: () => ({
              order: async () => ({
                data: [
                  { code: 'gap_wedge' },
                  { code: 'sand_wedge' },
                  { code: 'lob_wedge' },
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
        if (name === 'save_practice_unit') {
          forwardedInstructions = params.p_instructions as Array<
            Record<string, unknown>
          >;
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
      name: 'create_unit',
      arguments: {
        title: 'Wedge ladder',
        instructions: [
          { order: 1, text: 'Gap wedges', club_code: 'gap_wedge' },
          { order: 2, text: 'Sand wedges', club_code: 'sand_wedge' },
          { order: 3, text: 'Inherit the default' },
        ],
        default_club_code: 'lob_wedge',
      },
    })) as {
      content: Array<{ type: string; text?: string }>;
      isError?: boolean;
    };

    expect(result.isError).toBeFalsy();
    expect(forwardedInstructions).toHaveLength(3);
    expect(forwardedInstructions[0]?.club_code).toBe('gap_wedge');
    expect(forwardedInstructions[1]?.club_code).toBe('sand_wedge');
    // Omitted club_code is not forwarded (falls back to the unit default).
    expect('club_code' in (forwardedInstructions[2] ?? {})).toBe(false);
  });

  it('rejects an unknown per-instruction club_code', async () => {
    const mockSupabaseClient = {
      from: (table: string) => {
        if (table === 'clubs') {
          return {
            select: () => ({
              order: async () => ({
                data: [{ code: 'gap_wedge' }, { code: 'sand_wedge' }],
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
      name: 'create_unit',
      arguments: {
        title: 'Wedge ladder',
        instructions: [
          { order: 1, text: 'Gap wedges', club_code: 'gap_wedge' },
          { order: 2, text: 'Bad club', club_code: 'not_a_club' },
        ],
      },
    })) as {
      content: Array<{ type: string; text?: string }>;
      isError?: boolean;
    };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('UNKNOWN_CLUB_CODE');
    expect(parsed.data.field).toBe('instructions[1].club_code');
    expect(parsed.data.valid_codes).toBeDefined();
  });

  it('creates a unit without any club_code (forwards no club_code key)', async () => {
    let forwardedInstructions: Array<Record<string, unknown>> = [];
    const mockSupabaseClient = {
      from: () => ({
        select: () => ({ order: async () => ({ data: [], error: null }) }),
      }),
      rpc: async (name: string, params: Record<string, unknown>) => {
        if (name === 'save_practice_unit') {
          forwardedInstructions = params.p_instructions as Array<
            Record<string, unknown>
          >;
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
      name: 'create_unit',
      arguments: {
        title: 'Plain drill',
        instructions: [{ order: 1, text: 'Hit balls', ball_count: 10 }],
      },
    })) as {
      content: Array<{ type: string; text?: string }>;
      isError?: boolean;
    };

    expect(result.isError).toBeFalsy();
    expect(forwardedInstructions).toHaveLength(1);
    expect('club_code' in (forwardedInstructions[0] ?? {})).toBe(false);
  });

  it('rejects unknown club code', async () => {
    const mockSupabaseClient = {
      from: (table: string) => {
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
      name: 'create_unit',
      arguments: {
        title: 'Drill',
        instructions: [{ order: 1, text: 'Step 1' }],
        default_club_code: 'invalid_club',
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
