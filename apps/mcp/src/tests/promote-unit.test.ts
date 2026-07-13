import { describe, expect, it } from 'vitest';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { InMemoryTransport } from '@modelcontextprotocol/sdk/inMemory.js';
import { createServer } from '../server.js';
import type { UserContext } from '../auth/userContext.js';
import { mockR2Bucket } from './test-helpers.js';

function connect(supabaseClient: unknown) {
  const userContext: UserContext = {
    userId: 'test-user',
    supabaseClient: supabaseClient as UserContext['supabaseClient'],
  };
  const server = createServer(userContext, mockR2Bucket());
  const [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair();
  const client = new Client({ name: 'test-client', version: '1.0.0' });
  return Promise.all([
    server.connect(serverTransport),
    client.connect(clientTransport),
  ]).then(() => client);
}

function mockUpdate(
  onUpdate: (payload: Record<string, unknown>) => {
    data: Record<string, unknown> | null;
    error: { code?: string; message: string } | null;
  },
) {
  return {
    from: (table: string) => {
      expect(table).toBe('practice_units');
      return {
        update: (payload: Record<string, unknown>) => ({
          eq: (column: string, value: unknown) => {
            expect(column).toBe('id');
            expect(value).toBe('unit-1');
            return {
              select: () => ({
                maybeSingle: async () => onUpdate(payload),
              }),
            };
          },
        }),
      };
    },
  };
}

describe('promote_unit tool', () => {
  it('detaches scope and returns inline: false', async () => {
    let updatePayload: Record<string, unknown> | null = null;
    const mockSupabaseClient = mockUpdate(payload => {
      updatePayload = payload;
      return {
        data: { id: 'unit-1', title: 'Gate Drill', scoped_to_session_id: null },
        error: null,
      };
    });

    const client = await connect(mockSupabaseClient);
    const result = (await client.callTool({
      name: 'promote_unit',
      arguments: { unit_id: 'unit-1' },
    })) as { content: Array<{ type: string; text?: string }> };

    expect(
      (updatePayload as unknown as { scoped_to_session_id: null })
        ?.scoped_to_session_id,
    ).toBeNull();
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.unit).toEqual({
      id: 'unit-1',
      title: 'Gate Drill',
      inline: false,
    });
  });

  it('is idempotent on an already-library unit', async () => {
    const mockSupabaseClient = mockUpdate(() => ({
      data: { id: 'unit-1', title: 'Library Drill', scoped_to_session_id: null },
      error: null,
    }));

    const client = await connect(mockSupabaseClient);
    const result = (await client.callTool({
      name: 'promote_unit',
      arguments: { unit_id: 'unit-1' },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBeFalsy();
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.unit.inline).toBe(false);
  });

  it('returns UNIT_NOT_FOUND when the row is missing', async () => {
    const mockSupabaseClient = mockUpdate(() => ({ data: null, error: null }));

    const client = await connect(mockSupabaseClient);
    const result = (await client.callTool({
      name: 'promote_unit',
      arguments: { unit_id: 'unit-1' },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('UNIT_NOT_FOUND');
  });

  it('returns DATABASE_ERROR on a DB failure', async () => {
    const mockSupabaseClient = mockUpdate(() => ({
      data: null,
      error: { message: 'connection reset' },
    }));

    const client = await connect(mockSupabaseClient);
    const result = (await client.callTool({
      name: 'promote_unit',
      arguments: { unit_id: 'unit-1' },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('DATABASE_ERROR');
  });

  it('rejects an empty unit_id', async () => {
    const mockSupabaseClient = {
      from: () => {
        throw new Error('should not query when unit_id is empty');
      },
    };

    const client = await connect(mockSupabaseClient);
    const result = (await client.callTool({
      name: 'promote_unit',
      arguments: { unit_id: '   ' },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('VALIDATION_ERROR');
    expect(parsed.data.field).toBe('unit_id');
  });
});
