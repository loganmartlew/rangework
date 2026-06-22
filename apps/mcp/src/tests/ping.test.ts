import { describe, expect, it } from 'vitest';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { InMemoryTransport } from '@modelcontextprotocol/sdk/inMemory.js';
import { createServer } from '../server.js';
import type { UserContext } from '../auth/userContext.js';

// Mock UserContext for tests that don't exercise Supabase queries
const mockUserContext: UserContext = {
  userId: 'test-user-id',
  supabaseClient: {} as any,
};

describe('ping tool', () => {
  it('is registered in tools/list', async () => {
    const server = createServer(mockUserContext);
    const [clientTransport, serverTransport] =
      InMemoryTransport.createLinkedPair();

    const client = new Client({ name: 'test-client', version: '1.0.0' });

    await Promise.all([
      server.connect(serverTransport),
      client.connect(clientTransport),
    ]);

    const tools = await client.listTools();
    const pingTool = tools.tools.find(t => t.name === 'ping');
    expect(pingTool).toBeDefined();
    expect(pingTool?.description).toContain('Health-check');
  });

  it('returns { status: "ok" } when called', async () => {
    const server = createServer(mockUserContext);
    const [clientTransport, serverTransport] =
      InMemoryTransport.createLinkedPair();

    const client = new Client({ name: 'test-client', version: '1.0.0' });

    await Promise.all([
      server.connect(serverTransport),
      client.connect(clientTransport),
    ]);

    const result = (await client.callTool({ name: 'ping', arguments: {} })) as {
      content: Array<{ type: string; text?: string }>;
    };
    expect(result.content).toHaveLength(1);
    expect(result.content[0]?.type).toBe('text');
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed).toEqual({ status: 'ok' });
  });
});
