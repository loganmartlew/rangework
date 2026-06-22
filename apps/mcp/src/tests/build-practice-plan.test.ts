import { describe, expect, it, beforeEach } from 'vitest';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { InMemoryTransport } from '@modelcontextprotocol/sdk/inMemory.js';
import { createServer } from '../server.js';
import type { UserContext } from '../auth/userContext.js';
import { mockR2Bucket } from './test-helpers.js';
import { _resetCache } from '../methodology/loader.js';

const mockUserContext: UserContext = {
  userId: 'test-user-id',
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  supabaseClient: {} as any,
};

describe('build_practice_plan prompt', () => {
  beforeEach(() => {
    _resetCache();
  });

  it('is listed in prompts/list', async () => {
    const server = createServer(mockUserContext, mockR2Bucket());
    const [clientTransport, serverTransport] =
      InMemoryTransport.createLinkedPair();

    const client = new Client({ name: 'test-client', version: '1.0.0' });

    await Promise.all([
      server.connect(serverTransport),
      client.connect(clientTransport),
    ]);

    const prompts = await client.listPrompts();
    const buildPlanPrompt = prompts.prompts.find(
      p => p.name === 'build_practice_plan',
    );
    expect(buildPlanPrompt).toBeDefined();
    expect(buildPlanPrompt?.description).toContain('golf practice session');
  });

  it('returns a user role message with methodology text', async () => {
    const server = createServer(mockUserContext, mockR2Bucket());
    const [clientTransport, serverTransport] =
      InMemoryTransport.createLinkedPair();

    const client = new Client({ name: 'test-client', version: '1.0.0' });

    await Promise.all([
      server.connect(serverTransport),
      client.connect(clientTransport),
    ]);

    const result = await client.getPrompt({
      name: 'build_practice_plan',
      arguments: {},
    });

    expect(result.messages).toHaveLength(1);
    expect(result.messages[0]?.role).toBe('user');
    const content = result.messages[0]?.content as {
      type: string;
      text?: string;
    };
    expect(content.type).toBe('text');
    expect(content.text).toContain('Rangework Coaching Guide');
    expect(content.text).toContain('methodology_version: "1.0.0"');
  });

  it('appends focus argument when provided', async () => {
    const server = createServer(mockUserContext, mockR2Bucket());
    const [clientTransport, serverTransport] =
      InMemoryTransport.createLinkedPair();

    const client = new Client({ name: 'test-client', version: '1.0.0' });

    await Promise.all([
      server.connect(serverTransport),
      client.connect(clientTransport),
    ]);

    const result = await client.getPrompt({
      name: 'build_practice_plan',
      arguments: { focus: 'putting' },
    });

    const content = result.messages[0]?.content as {
      type: string;
      text?: string;
    };
    expect(content.text).toContain('The user wants to focus on: putting');
  });

  it('does not append focus line when argument is absent', async () => {
    const server = createServer(mockUserContext, mockR2Bucket());
    const [clientTransport, serverTransport] =
      InMemoryTransport.createLinkedPair();

    const client = new Client({ name: 'test-client', version: '1.0.0' });

    await Promise.all([
      server.connect(serverTransport),
      client.connect(clientTransport),
    ]);

    const result = await client.getPrompt({
      name: 'build_practice_plan',
      arguments: {},
    });

    const content = result.messages[0]?.content as {
      type: string;
      text?: string;
    };
    expect(content.text).not.toContain('The user wants to focus on');
  });

  it('returns unavailability message when methodology is null', async () => {
    const server = createServer(mockUserContext, mockR2Bucket(null));
    const [clientTransport, serverTransport] =
      InMemoryTransport.createLinkedPair();

    const client = new Client({ name: 'test-client', version: '1.0.0' });

    await Promise.all([
      server.connect(serverTransport),
      client.connect(clientTransport),
    ]);

    const result = await client.getPrompt({
      name: 'build_practice_plan',
      arguments: {},
    });

    const content = result.messages[0]?.content as {
      type: string;
      text?: string;
    };
    expect(content.text).toContain('temporarily unavailable');
  });
});
