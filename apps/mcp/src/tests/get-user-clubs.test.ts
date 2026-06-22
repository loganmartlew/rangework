import { describe, expect, it } from 'vitest';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { InMemoryTransport } from '@modelcontextprotocol/sdk/inMemory.js';
import { createServer } from '../server.js';
import type { UserContext } from '../auth/userContext.js';
import { mockR2Bucket } from './test-helpers.js';

describe('get_user_clubs tool', () => {
  it('returns clubs ordered by sort_order', async () => {
    const mockSupabaseClient = {
      from: (table: string) => {
        if (table === 'user_enabled_clubs') {
          return {
            select: () => ({
              order: async () => ({
                data: [
                  {
                    club_code: 'driver',
                    clubs: {
                      code: 'driver',
                      display_name: 'Driver',
                      category: 'WOOD',
                      sort_order: 1,
                    },
                  },
                  {
                    club_code: 'seven_iron',
                    clubs: {
                      code: 'seven_iron',
                      display_name: '7 Iron',
                      category: 'IRON',
                      sort_order: 7,
                    },
                  },
                  {
                    club_code: 'putter',
                    clubs: {
                      code: 'putter',
                      display_name: 'Putter',
                      category: 'PUTTER',
                      sort_order: 14,
                    },
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
      name: 'get_user_clubs',
      arguments: {},
    })) as { content: Array<{ type: string; text?: string }> };

    expect(result.content).toHaveLength(1);
    expect(result.content[0]?.type).toBe('text');

    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.clubs).toHaveLength(3);
    expect(parsed.clubs[0]).toEqual({
      code: 'driver',
      display_name: 'Driver',
      category: 'WOOD',
    });
    expect(parsed.clubs[1]).toEqual({
      code: 'seven_iron',
      display_name: '7 Iron',
      category: 'IRON',
    });
    expect(parsed.clubs[2]).toEqual({
      code: 'putter',
      display_name: 'Putter',
      category: 'PUTTER',
    });
  });

  it('returns empty array when user has no clubs', async () => {
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
      name: 'get_user_clubs',
      arguments: {},
    })) as { content: Array<{ type: string; text?: string }> };

    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.clubs).toEqual([]);
  });
});
