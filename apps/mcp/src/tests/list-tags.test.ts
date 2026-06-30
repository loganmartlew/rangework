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

describe('list_tags tool', () => {
  it('returns default and custom tags with is_default flag', async () => {
    const mockSupabaseClient = {
      from: (table: string) => {
        if (table === 'tags') {
          return {
            select: () => ({
              order: async () => ({
                data: [
                  {
                    id: 't1',
                    code: 'putting',
                    display_name: 'Putting',
                    owner_id: null,
                  },
                  {
                    id: 't2',
                    code: 'lag_putting',
                    display_name: 'Lag putting',
                    owner_id: 'test-user',
                  },
                ],
                error: null,
              }),
            }),
          };
        }
        return { select: () => ({ order: async () => ({ data: [], error: null }) }) };
      },
    };

    const client = await connect(mockSupabaseClient);
    const result = (await client.callTool({
      name: 'list_tags',
      arguments: {},
    })) as { content: Array<{ type: string; text?: string }> };

    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.tags).toEqual([
      { code: 'putting', display_name: 'Putting', is_default: true },
      { code: 'lag_putting', display_name: 'Lag putting', is_default: false },
    ]);
  });

  it('returns a DATABASE_ERROR when the query fails', async () => {
    const mockSupabaseClient = {
      from: () => ({
        select: () => ({
          order: async () => ({ data: null, error: { message: 'boom' } }),
        }),
      }),
    };

    const client = await connect(mockSupabaseClient);
    const result = (await client.callTool({
      name: 'list_tags',
      arguments: {},
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('DATABASE_ERROR');
  });
});
