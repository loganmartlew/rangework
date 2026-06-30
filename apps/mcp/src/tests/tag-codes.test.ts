import { describe, expect, it } from 'vitest';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { InMemoryTransport } from '@modelcontextprotocol/sdk/inMemory.js';
import { createServer } from '../server.js';
import type { UserContext } from '../auth/userContext.js';
import { mockR2Bucket } from './test-helpers.js';

const VISIBLE_TAGS = [
  { id: 'tag-putting', code: 'putting', display_name: 'Putting', owner_id: null },
  { id: 'tag-short', code: 'short_game', display_name: 'Short Game', owner_id: null },
];

function tagsTable() {
  return {
    select: () => ({
      order: async () => ({ data: VISIBLE_TAGS, error: null }),
    }),
  };
}

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

describe('create_unit tag_codes', () => {
  it('resolves known tag codes to ids and passes them to the save RPC', async () => {
    const rpcCalls: string[] = [];
    let savedTagIds: unknown = null;
    const mockSupabaseClient = {
      from: (table: string) =>
        table === 'tags'
          ? tagsTable()
          : { select: () => ({ order: async () => ({ data: [], error: null }) }) },
      rpc: async (name: string, params: Record<string, unknown>) => {
        rpcCalls.push(name);
        if (name === 'save_practice_unit') savedTagIds = params.p_tag_ids;
        return { error: null };
      },
    };

    const client = await connect(mockSupabaseClient);
    const result = (await client.callTool({
      name: 'create_unit',
      arguments: {
        title: 'Drill',
        instructions: [{ order: 1, text: 'Step 1' }],
        tag_codes: ['putting', 'short_game', 'putting'],
      },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBeFalsy();
    // De-duplicated, mapped to ids.
    expect(savedTagIds).toEqual(['tag-putting', 'tag-short']);
    expect(rpcCalls).toEqual(['save_practice_unit']);
  });

  it('rejects an unknown tag code and never creates a tag', async () => {
    const rpcCalls: string[] = [];
    const mockSupabaseClient = {
      from: (table: string) =>
        table === 'tags'
          ? tagsTable()
          : { select: () => ({ order: async () => ({ data: [], error: null }) }) },
      rpc: async (name: string) => {
        rpcCalls.push(name);
        return { error: null };
      },
    };

    const client = await connect(mockSupabaseClient);
    const result = (await client.callTool({
      name: 'create_unit',
      arguments: {
        title: 'Drill',
        instructions: [{ order: 1, text: 'Step 1' }],
        tag_codes: ['putting', 'made_up_code'],
      },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('UNKNOWN_TAG_CODE');
    expect(parsed.data.valid_codes).toEqual(['putting', 'short_game']);
    // Did not write the unit and did not mint a tag.
    expect(rpcCalls).toEqual([]);
  });

  it('rejects more than 8 tag codes', async () => {
    const mockSupabaseClient = {
      from: (table: string) =>
        table === 'tags'
          ? tagsTable()
          : { select: () => ({ order: async () => ({ data: [], error: null }) }) },
      rpc: async () => ({ error: null }),
    };

    const client = await connect(mockSupabaseClient);
    const result = (await client.callTool({
      name: 'create_unit',
      arguments: {
        title: 'Drill',
        instructions: [{ order: 1, text: 'Step 1' }],
        tag_codes: Array.from({ length: 9 }, (_, i) => `code_${i}`),
      },
    })) as { content: Array<{ type: string; text?: string }>; isError?: boolean };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('VALIDATION_ERROR');
    expect(parsed.data.field).toBe('tag_codes');
  });
});
