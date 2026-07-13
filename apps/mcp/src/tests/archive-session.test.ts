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
      expect(table).toBe('practice_sessions');
      return {
        update: (payload: Record<string, unknown>) => ({
          eq: (column: string, value: unknown) => {
            expect(column).toBe('id');
            expect(value).toBe('session-1');
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

describe('archive_session tool', () => {
  it('sets archived_at and returns archived: true', async () => {
    let updatePayload: Record<string, unknown> | null = null;
    const mockSupabaseClient = mockUpdate(payload => {
      updatePayload = payload;
      return {
        data: { id: 'session-1', name: 'Morning Practice', archived_at: '2026-07-13T00:00:00.000Z' },
        error: null,
      };
    });

    const client = await connect(mockSupabaseClient);
    const result = (await client.callTool({
      name: 'archive_session',
      arguments: { session_id: 'session-1' },
    })) as { content: Array<{ type: string; text?: string }> };

    expect(typeof (updatePayload as unknown as { archived_at: string })?.archived_at).toBe(
      'string',
    );
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.session).toEqual({
      id: 'session-1',
      name: 'Morning Practice',
      archived: true,
    });
  });

  it('returns SESSION_NOT_FOUND when the row is missing', async () => {
    const mockSupabaseClient = mockUpdate(() => ({ data: null, error: null }));

    const client = await connect(mockSupabaseClient);
    const result = (await client.callTool({
      name: 'archive_session',
      arguments: { session_id: 'session-1' },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('SESSION_NOT_FOUND');
  });

  it('returns DATABASE_ERROR on a DB failure', async () => {
    const mockSupabaseClient = mockUpdate(() => ({
      data: null,
      error: { message: 'connection reset' },
    }));

    const client = await connect(mockSupabaseClient);
    const result = (await client.callTool({
      name: 'archive_session',
      arguments: { session_id: 'session-1' },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('DATABASE_ERROR');
  });

  it('rejects an empty session_id', async () => {
    const mockSupabaseClient = {
      from: () => {
        throw new Error('should not query when session_id is empty');
      },
    };

    const client = await connect(mockSupabaseClient);
    const result = (await client.callTool({
      name: 'archive_session',
      arguments: { session_id: '   ' },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('VALIDATION_ERROR');
    expect(parsed.data.field).toBe('session_id');
  });
});

describe('unarchive_session tool', () => {
  it('clears archived_at and returns archived: false', async () => {
    let updatePayload: Record<string, unknown> | null = null;
    const mockSupabaseClient = mockUpdate(payload => {
      updatePayload = payload;
      return {
        data: { id: 'session-1', name: 'Morning Practice', archived_at: null },
        error: null,
      };
    });

    const client = await connect(mockSupabaseClient);
    const result = (await client.callTool({
      name: 'unarchive_session',
      arguments: { session_id: 'session-1' },
    })) as { content: Array<{ type: string; text?: string }> };

    expect((updatePayload as unknown as { archived_at: null })?.archived_at).toBeNull();
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.session).toEqual({
      id: 'session-1',
      name: 'Morning Practice',
      archived: false,
    });
  });

  it('returns SESSION_NOT_FOUND when the row is missing', async () => {
    const mockSupabaseClient = mockUpdate(() => ({ data: null, error: null }));

    const client = await connect(mockSupabaseClient);
    const result = (await client.callTool({
      name: 'unarchive_session',
      arguments: { session_id: 'session-1' },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('SESSION_NOT_FOUND');
  });

  it('returns DATABASE_ERROR on a DB failure', async () => {
    const mockSupabaseClient = mockUpdate(() => ({
      data: null,
      error: { message: 'connection reset' },
    }));

    const client = await connect(mockSupabaseClient);
    const result = (await client.callTool({
      name: 'unarchive_session',
      arguments: { session_id: 'session-1' },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('DATABASE_ERROR');
  });
});
