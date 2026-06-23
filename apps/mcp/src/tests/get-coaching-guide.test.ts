import { describe, expect, it, beforeEach } from 'vitest';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { InMemoryTransport } from '@modelcontextprotocol/sdk/inMemory.js';
import { createServer } from '../server.js';
import type { UserContext } from '../auth/userContext.js';
import { mockR2Bucket } from './test-helpers.js';
import { _resetCache } from '../methodology/loader.js';

const MOCK_METHODOLOGY =
  '# Rangework Coaching Guide\n\nmethodology_version: "2.3.1"\n\nFull coaching guide content.';

const mockUserContext: UserContext = {
  userId: 'test-user-id',
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  supabaseClient: {} as any,
};

describe('get_coaching_guide tool', () => {
  beforeEach(() => {
    _resetCache();
  });

  it('is registered in tools/list', async () => {
    const server = createServer(mockUserContext, mockR2Bucket());
    const [clientTransport, serverTransport] =
      InMemoryTransport.createLinkedPair();

    const client = new Client({ name: 'test-client', version: '1.0.0' });

    await Promise.all([
      server.connect(serverTransport),
      client.connect(clientTransport),
    ]);

    const tools = await client.listTools();
    const guideTool = tools.tools.find(t => t.name === 'get_coaching_guide');
    expect(guideTool).toBeDefined();
    expect(guideTool?.description).toContain('coaching guide');
  });

  it('returns { methodology_version, guide } shape', async () => {
    const server = createServer(
      mockUserContext,
      mockR2Bucket(MOCK_METHODOLOGY),
    );
    const [clientTransport, serverTransport] =
      InMemoryTransport.createLinkedPair();

    const client = new Client({ name: 'test-client', version: '1.0.0' });

    await Promise.all([
      server.connect(serverTransport),
      client.connect(clientTransport),
    ]);

    const result = (await client.callTool({
      name: 'get_coaching_guide',
      arguments: {},
    })) as { content: Array<{ type: string; text?: string }> };

    expect(result.content).toHaveLength(1);
    expect(result.content[0]?.type).toBe('text');

    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.methodology_version).toBeDefined();
    expect(parsed.guide).toBeDefined();
  });

  it('extracts methodology_version from preamble', async () => {
    const server = createServer(
      mockUserContext,
      mockR2Bucket(MOCK_METHODOLOGY),
    );
    const [clientTransport, serverTransport] =
      InMemoryTransport.createLinkedPair();

    const client = new Client({ name: 'test-client', version: '1.0.0' });

    await Promise.all([
      server.connect(serverTransport),
      client.connect(clientTransport),
    ]);

    const result = (await client.callTool({
      name: 'get_coaching_guide',
      arguments: {},
    })) as { content: Array<{ type: string; text?: string }> };

    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.methodology_version).toBe('2.3.1');
  });

  it('returns full methodology text in guide field', async () => {
    const server = createServer(
      mockUserContext,
      mockR2Bucket(MOCK_METHODOLOGY),
    );
    const [clientTransport, serverTransport] =
      InMemoryTransport.createLinkedPair();

    const client = new Client({ name: 'test-client', version: '1.0.0' });

    await Promise.all([
      server.connect(serverTransport),
      client.connect(clientTransport),
    ]);

    const result = (await client.callTool({
      name: 'get_coaching_guide',
      arguments: {},
    })) as { content: Array<{ type: string; text?: string }> };

    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.guide).toBe(MOCK_METHODOLOGY);
  });

  it('returns CONTENT_UNAVAILABLE error when R2 fails', async () => {
    const server = createServer(mockUserContext, mockR2Bucket(null));
    const [clientTransport, serverTransport] =
      InMemoryTransport.createLinkedPair();

    const client = new Client({ name: 'test-client', version: '1.0.0' });

    await Promise.all([
      server.connect(serverTransport),
      client.connect(clientTransport),
    ]);

    const result = (await client.callTool({
      name: 'get_coaching_guide',
      arguments: {},
    })) as {
      content: Array<{ type: string; text?: string }>;
      isError?: boolean;
    };

    expect(result.isError).toBe(true);
    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.code).toBe('CONTENT_UNAVAILABLE');
    expect(parsed.message).toContain('temporarily unavailable');
  });

  it('falls back to "unknown" when version is missing from text', async () => {
    const noVersionText =
      '# Rangework Coaching Guide\n\nNo version line in this text.';
    const server = createServer(mockUserContext, mockR2Bucket(noVersionText));
    const [clientTransport, serverTransport] =
      InMemoryTransport.createLinkedPair();

    const client = new Client({ name: 'test-client', version: '1.0.0' });

    await Promise.all([
      server.connect(serverTransport),
      client.connect(clientTransport),
    ]);

    const result = (await client.callTool({
      name: 'get_coaching_guide',
      arguments: {},
    })) as { content: Array<{ type: string; text?: string }> };

    const parsed = JSON.parse(result.content[0]?.text ?? '{}');
    expect(parsed.methodology_version).toBe('unknown');
  });
});
