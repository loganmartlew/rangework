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

describe('create_tag tool', () => {
  it('slugs the name, calls create_or_get_tag, and returns the resolved tag', async () => {
    let rpcArgs: Record<string, unknown> | null = null;
    const mockSupabaseClient = {
      rpc: async (name: string, params: Record<string, unknown>) => {
        expect(name).toBe('create_or_get_tag');
        rpcArgs = params;
        return { data: 'tag-id-1', error: null };
      },
      from: (table: string) => {
        expect(table).toBe('tags');
        return {
          select: () => ({
            eq: () => ({
              single: async () => ({
                data: {
                  code: 'lag_putting',
                  display_name: 'Lag Putting',
                  owner_id: 'test-user',
                },
                error: null,
              }),
            }),
          }),
        };
      },
    };

    const client = await connect(mockSupabaseClient);
    const result = (await client.callTool({
      name: 'create_tag',
      arguments: { name: '  Lag Putting  ' },
    })) as { content: Array<{ type: string; text?: string }> };

    expect(rpcArgs).toEqual({ p_code: 'lag_putting', p_name: 'Lag Putting' });
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.tag).toEqual({
      code: 'lag_putting',
      display_name: 'Lag Putting',
      is_default: false,
    });
  });

  it('rejects a name with no alphanumeric content', async () => {
    const mockSupabaseClient = {
      rpc: async () => ({ data: null, error: { message: 'should not be called' } }),
      from: () => ({}),
    };

    const client = await connect(mockSupabaseClient);
    const result = (await client.callTool({
      name: 'create_tag',
      arguments: { name: '   ' },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('VALIDATION_ERROR');
    expect(parsed.data.field).toBe('name');
  });
});
