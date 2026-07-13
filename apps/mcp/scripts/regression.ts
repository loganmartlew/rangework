#!/usr/bin/env npx tsx
/**
 * MCP Inspector regression script for RWK-34 (Stage 5).
 *
 * Exercises all five data tools against a live Worker using a test JWT.
 * Designed to be run via `npx tsx apps/mcp/scripts/regression.ts` with
 * environment variables for configuration.
 *
 * Usage:
 *   MCP_WORKER_URL=https://rangework-mcp.your-username.workers.dev/mcp \
 *   MCP_TEST_TOKEN=<jwt> \
 *   npx tsx apps/mcp/scripts/regression.ts
 *
 * Auth-isolation check (S6):
 *   SECOND_TEST_TOKEN=<second-user-jwt> \
 *   npx tsx apps/mcp/scripts/regression.ts
 *
 * The test token can be obtained from a logged-in session (browser dev tools
 * → Application → Storage → Supabase access_token) or from the Supabase CLI:
 *   supabase auth user --token <user-id>
 */

const WORKER_URL = process.env.MCP_WORKER_URL;
const TEST_TOKEN = process.env.MCP_TEST_TOKEN;
const SECOND_TOKEN = process.env.SECOND_TEST_TOKEN;

if (!WORKER_URL || !TEST_TOKEN) {
  console.error(
    'MCP_WORKER_URL and MCP_TEST_TOKEN must be set.\n' +
      'Usage:\n' +
      '  MCP_WORKER_URL=<url> MCP_TEST_TOKEN=<jwt> npx tsx apps/mcp/scripts/regression.ts',
  );
  process.exit(1);
}

// Narrowed types after the early-exit check
const workerUrl: string = WORKER_URL;
const testToken: string = TEST_TOKEN;

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface McpRequest {
  jsonrpc: '2.0';
  id: number;
  method: string;
  params?: Record<string, unknown>;
}

interface McpCallResult {
  jsonrpc: '2.0';
  id: number;
  result?: {
    content: Array<{ type: string; text: string }>;
    isError?: boolean;
    _meta?: Record<string, unknown>;
  };
  error?: {
    code: number;
    message: string;
    data?: unknown;
  };
}

interface ToolResponse {
  status: number;
  body: McpCallResult | null;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

let passCount = 0;
let failCount = 0;

function pass(message: string): void {
  console.log(`  ✅ PASS: ${message}`);
  passCount++;
}

function fail(message: string): void {
  console.log(`  ❌ FAIL: ${message}`);
  failCount++;
  process.exitCode = 1;
}

function assert(condition: boolean, message: string): void {
  if (condition) {
    pass(message);
  } else {
    fail(message);
  }
}

async function callTool(
  toolName: string,
  args: Record<string, unknown> = {},
  token?: string,
): Promise<ToolResponse> {
  const request: McpRequest = {
    jsonrpc: '2.0',
    id: Date.now(),
    method: 'tools/call',
    params: { name: toolName, arguments: args },
  };

  const res = await fetch(workerUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token ?? testToken}`,
    },
    body: JSON.stringify(request),
  });

  let body: McpCallResult | null = null;
  try {
    body = (await res.json()) as McpCallResult;
  } catch {
    // non-JSON response
  }

  return { status: res.status, body };
}

async function callListTools(token?: string): Promise<ToolResponse> {
  const request: McpRequest = {
    jsonrpc: '2.0',
    id: Date.now(),
    method: 'tools/list',
  };

  const res = await fetch(workerUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token ?? testToken}`,
    },
    body: JSON.stringify(request),
  });

  let body: McpCallResult | null = null;
  try {
    body = (await res.json()) as McpCallResult;
  } catch {
    // non-JSON response
  }

  return { status: res.status, body };
}

function parseContent(
  body: McpCallResult | null,
): Record<string, unknown> | null {
  if (!body?.result?.content) return null;
  for (const c of body.result.content) {
    if (c.type === 'text' && c.text) {
      try {
        return JSON.parse(c.text) as Record<string, unknown>;
      } catch {
        return null;
      }
    }
  }
  return null;
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

async function main(): Promise<void> {
  console.log('='.repeat(60));
  console.log('RWK-34 MCP Regression Script');
  console.log(`Testing against: ${workerUrl}`);
  console.log('='.repeat(60));

  // ---- tools/list ----
  console.log('\n📋 tools/list');
  const toolList = await callListTools();
  assert(toolList.status === 200, 'tools/list returns 200');

  const toolNames = new Set<string>();
  if (toolList.body?.result) {
    const content = toolList.body.result as unknown as {
      tools?: Array<{ name: string }>;
    };
    if (content.tools) {
      for (const t of content.tools) {
        toolNames.add(t.name);
      }
    }
  }

  const expectedTools = [
    'ping',
    'get_user_clubs',
    'list_units',
    'list_sessions',
    'list_range_sessions',
    'get_range_session',
    'create_unit',
    'create_session',
    'get_coaching_guide',
    'archive_session',
    'unarchive_session',
    'promote_unit',
  ];

  for (const name of expectedTools) {
    assert(toolNames.has(name), `tool "${name}" is registered`);
  }

  // ---- ping ----
  console.log('\n🏓 ping');
  const ping = await callTool('ping');
  assert(ping.status === 200, 'ping returns 200');
  assert(ping.body?.result != null, 'ping has a result');
  const pingData = parseContent(ping.body);
  assert(pingData?.status === 'ok', 'ping returns { status: "ok" }');

  // ---- get_user_clubs ----
  console.log('\n🏌️  get_user_clubs');
  const clubs = await callTool('get_user_clubs');
  assert(clubs.status === 200, 'get_user_clubs returns 200');
  const clubsData = parseContent(clubs.body);
  if (clubsData?.clubs && Array.isArray(clubsData.clubs)) {
    assert(clubsData.clubs.length > 0, 'user has at least one club enabled');
    // Each club should have code + display_name
    const first = clubsData.clubs[0] as Record<string, unknown>;
    assert(
      typeof first?.code === 'string',
      'club entry has a "code" string field',
    );
    assert(
      typeof first?.display_name === 'string',
      'club entry has a "display_name" string field',
    );
    assert(
      typeof first?.category === 'string',
      'club entry has a "category" string field',
    );
  } else {
    fail('get_user_clubs response contains a "clubs" array');
  }

  // ---- list_units ----
  console.log('\n📝 list_units');
  const units = await callTool('list_units');
  assert(units.status === 200, 'list_units returns 200');
  const unitsData = parseContent(units.body);
  if (unitsData?.units && Array.isArray(unitsData.units)) {
    pass('list_units returns a "units" array');
    // If there are units, validate structure
    if (unitsData.units.length > 0) {
      const first = unitsData.units[0] as Record<string, unknown>;
      assert(
        typeof first?.id === 'string',
        'unit entry has an "id" string field',
      );
      assert(
        typeof first?.title === 'string',
        'unit entry has a "title" string field',
      );
    }
  } else {
    fail('list_units response contains a "units" array');
  }

  // ---- list_sessions ----
  console.log('\n📋 list_sessions');
  const sessions = await callTool('list_sessions');
  assert(sessions.status === 200, 'list_sessions returns 200');
  const sessionsData = parseContent(sessions.body);
  if (sessionsData?.sessions && Array.isArray(sessionsData.sessions)) {
    pass('list_sessions returns a "sessions" array');
    if (sessionsData.sessions.length > 0) {
      const first = sessionsData.sessions[0] as Record<string, unknown>;
      assert(
        typeof first?.id === 'string',
        'session entry has an "id" string field',
      );
      assert(
        typeof first?.name === 'string',
        'session entry has a "name" string field',
      );
    }
  } else {
    fail('list_sessions response contains a "sessions" array');
  }

  // ---- create_unit ----
  console.log('\n✨ create_unit');
  const timestamp = Date.now();
  const newUnit = await callTool('create_unit', {
    title: `[TEST] Regression unit ${timestamp}`,
    notes: 'Created by RWK-34 regression script',
    focus: 'Testing',
    success_criterion: 'inside 5m of the target (regression check)',
    instructions: [
      { order: 1, text: 'Test instruction 1', ball_count: 10 },
      { order: 2, text: 'Test instruction 2', ball_count: 15 },
    ],
  });
  assert(newUnit.status === 200, 'create_unit returns 200');
  assert(!newUnit.body?.error, 'create_unit has no JSON-RPC error');

  const unitData = parseContent(newUnit.body);
  let createdUnitId: string | undefined;
  if (
    unitData?.unit_id &&
    typeof unitData.unit_id === 'string' &&
    unitData.unit_id.length > 0
  ) {
    createdUnitId = unitData.unit_id as string;
    pass(`create_unit returned unit_id: ${createdUnitId}`);
  } else {
    fail('create_unit response contains a non-empty "unit_id" string');
  }

  // ---- create_session (using the created unit) ----
  console.log('\n🎯 create_session');
  let createdSessionId: string | undefined;
  if (createdUnitId) {
    const newSession = await callTool('create_session', {
      name: `[TEST] Regression session ${timestamp}`,
      notes: 'Created by RWK-34 regression script',
      items: [
        {
          practice_unit_id: createdUnitId,
          order: 1,
          repeat_count: 3,
          // `success` is valid because the unit was created with a criterion.
          observation_types: ['success', 'shape'],
        },
      ],
    });
    assert(newSession.status === 200, 'create_session returns 200');
    assert(!newSession.body?.error, 'create_session has no JSON-RPC error');
    assert(
      !newSession.body?.result?.isError,
      'create_session accepts observation_types on a unit with a criterion',
    );

    const sessionData = parseContent(newSession.body);
    if (
      sessionData?.session_id &&
      typeof sessionData.session_id === 'string' &&
      sessionData.session_id.length > 0
    ) {
      createdSessionId = sessionData.session_id;
      pass(`create_session returned session_id: ${sessionData.session_id}`);
    } else {
      fail('create_session response contains a non-empty "session_id" string');
    }

    // `success` on a unit with no criterion must be rejected (friendly error).
    const noCriterionUnit = await callTool('create_unit', {
      title: `[TEST] Regression no-criterion unit ${timestamp}`,
      instructions: [{ order: 1, text: 'Hit balls', ball_count: 5 }],
    });
    const noCriterionUnitId = parseContent(noCriterionUnit.body)?.unit_id as
      | string
      | undefined;
    if (noCriterionUnitId) {
      const badSession = await callTool('create_session', {
        name: `[TEST] Regression bad-success session ${timestamp}`,
        items: [
          {
            practice_unit_id: noCriterionUnitId,
            order: 1,
            repeat_count: 1,
            observation_types: ['success'],
          },
        ],
      });
      const badData = parseContent(badSession.body);
      assert(
        badSession.body?.result?.isError === true &&
          badData?.code === 'VALIDATION_ERROR',
        "create_session rejects 'success' on a unit without a success_criterion",
      );
    }
  } else {
    console.log(
      '  ⏭️  Skipping create_session (no unit_id from previous step)',
    );
  }

  // ---- inline units / promote_unit ----
  console.log('\n🧩 inline units / promote_unit');
  const inlineSession = await callTool('create_session', {
    name: `[TEST] Regression inline session ${timestamp}`,
    items: [
      {
        inline_unit: {
          title: `[TEST] Regression inline unit ${timestamp}`,
          instructions: [{ order: 1, text: 'Hit balls at the flag', ball_count: 10 }],
        },
        order: 1,
        repeat_count: 1,
      },
    ],
  });
  assert(inlineSession.status === 200, 'create_session with inline_unit returns 200');
  assert(
    !inlineSession.body?.result?.isError,
    'create_session with inline_unit is not an error',
  );
  const inlineSessionData = parseContent(inlineSession.body);
  const inlineSessionId = inlineSessionData?.session_id as string | undefined;

  if (inlineSessionId) {
    const sessionsAfterInline = await callTool('list_sessions');
    const sessionsAfterInlineData = parseContent(sessionsAfterInline.body);
    const inlineSessionEntry = (
      (sessionsAfterInlineData?.sessions as Array<Record<string, unknown>>) ?? []
    ).find(s => s.id === inlineSessionId);
    const inlineItems =
      (inlineSessionEntry?.items as Array<Record<string, unknown>>) ?? [];
    const inlineItem = inlineItems[0];
    assert(
      inlineItem?.inline === true,
      'list_sessions marks the inline item with inline: true',
    );
    const inlineUnitId = inlineItem?.unit_id as string | undefined;

    if (inlineUnitId) {
      const unitsBeforePromote = await callTool('list_units');
      const unitsBeforePromoteData = parseContent(unitsBeforePromote.body);
      const inlineUnitVisibleBeforePromote = (
        (unitsBeforePromoteData?.units as Array<Record<string, unknown>>) ?? []
      ).some(u => u.id === inlineUnitId);
      assert(
        !inlineUnitVisibleBeforePromote,
        'the inline unit does not appear in list_units before promotion',
      );

      const promoted = await callTool('promote_unit', { unit_id: inlineUnitId });
      assert(promoted.status === 200, 'promote_unit returns 200');
      const promotedData = parseContent(promoted.body);
      assert(
        (promotedData?.unit as Record<string, unknown> | undefined)?.inline === false,
        'promote_unit returns inline: false',
      );

      const unitsAfterPromote = await callTool('list_units');
      const unitsAfterPromoteData = parseContent(unitsAfterPromote.body);
      const inlineUnitVisibleAfterPromote = (
        (unitsAfterPromoteData?.units as Array<Record<string, unknown>>) ?? []
      ).some(u => u.id === inlineUnitId);
      assert(
        inlineUnitVisibleAfterPromote,
        'the promoted unit now appears in list_units',
      );

      const sessionsAfterPromote = await callTool('list_sessions');
      const sessionsAfterPromoteData = parseContent(sessionsAfterPromote.body);
      const sessionAfterPromote = (
        (sessionsAfterPromoteData?.sessions as Array<Record<string, unknown>>) ?? []
      ).find(s => s.id === inlineSessionId);
      const itemAfterPromote = (
        (sessionAfterPromote?.items as Array<Record<string, unknown>>) ?? []
      )[0];
      assert(
        itemAfterPromote?.unit_id === inlineUnitId,
        'the session still references the promoted unit',
      );
      assert(
        itemAfterPromote?.inline === false,
        "the session's item now reports inline: false after promotion",
      );
    } else {
      fail('inline session item carries a unit_id');
    }

    // A well-formed but nonexistent id must return UNIT_NOT_FOUND.
    const missingUnit = await callTool('promote_unit', {
      unit_id: '00000000-0000-0000-0000-000000000000',
    });
    const missingUnitData = parseContent(missingUnit.body);
    assert(
      missingUnit.body?.result?.isError === true &&
        missingUnitData?.code === 'UNIT_NOT_FOUND',
      'promote_unit returns UNIT_NOT_FOUND for a nonexistent id',
    );
  } else {
    fail('create_session with inline_unit returns a non-empty "session_id" string');
  }

  // ---- archive_session / unarchive_session ----
  console.log('\n🗄️  archive_session / unarchive_session');
  if (createdSessionId) {
    const archived = await callTool('archive_session', {
      session_id: createdSessionId,
    });
    assert(archived.status === 200, 'archive_session returns 200');
    const archivedData = parseContent(archived.body);
    assert(
      archivedData?.session != null &&
        (archivedData.session as Record<string, unknown>).archived === true,
      'archive_session returns archived: true',
    );

    const defaultAfterArchive = await callTool('list_sessions');
    const defaultAfterArchiveData = parseContent(defaultAfterArchive.body);
    const defaultSessions =
      (defaultAfterArchiveData?.sessions as Array<Record<string, unknown>>) ??
      [];
    assert(
      !defaultSessions.some(s => s.id === createdSessionId),
      'default list_sessions hides the archived session',
    );

    const includeArchived = await callTool('list_sessions', {
      include_archived: true,
    });
    const includeArchivedData = parseContent(includeArchived.body);
    const includedSessions =
      (includeArchivedData?.sessions as Array<Record<string, unknown>>) ?? [];
    const archivedEntry = includedSessions.find(
      s => s.id === createdSessionId,
    );
    assert(
      archivedEntry?.archived === true,
      'list_sessions with include_archived: true returns the session with archived: true',
    );

    const unarchived = await callTool('unarchive_session', {
      session_id: createdSessionId,
    });
    assert(unarchived.status === 200, 'unarchive_session returns 200');
    const unarchivedData = parseContent(unarchived.body);
    assert(
      unarchivedData?.session != null &&
        (unarchivedData.session as Record<string, unknown>).archived ===
          false,
      'unarchive_session returns archived: false',
    );

    const defaultAfterUnarchive = await callTool('list_sessions');
    const defaultAfterUnarchiveData = parseContent(defaultAfterUnarchive.body);
    const reappearedSessions =
      (defaultAfterUnarchiveData?.sessions as Array<
        Record<string, unknown>
      >) ?? [];
    assert(
      reappearedSessions.some(s => s.id === createdSessionId),
      'default list_sessions shows the session again after unarchive',
    );

    // A well-formed but nonexistent id must return SESSION_NOT_FOUND.
    const missingSession = await callTool('archive_session', {
      session_id: '00000000-0000-0000-0000-000000000000',
    });
    const missingSessionData = parseContent(missingSession.body);
    assert(
      missingSession.body?.result?.isError === true &&
        missingSessionData?.code === 'SESSION_NOT_FOUND',
      'archive_session returns SESSION_NOT_FOUND for a nonexistent id',
    );
  } else {
    console.log(
      '  ⏭️  Skipping archive_session / unarchive_session (no session_id from previous step)',
    );
  }

  // ---- get_coaching_guide ----
  console.log('\n📖 get_coaching_guide');
  const guide = await callTool('get_coaching_guide');
  assert(guide.status === 200, 'get_coaching_guide returns 200');
  const guideData = parseContent(guide.body);
  if (guideData?.guide && typeof guideData.guide === 'string') {
    pass('get_coaching_guide returns a "guide" text field');
    assert(
      (guideData.guide as string).length > 0,
      'coaching guide is non-empty',
    );
  } else {
    fail('get_coaching_guide response contains a non-empty "guide" string');
  }

  // ---- list_range_sessions ----
  console.log('\n📊 list_range_sessions');
  const rangeSessions = await callTool('list_range_sessions');
  assert(rangeSessions.status === 200, 'list_range_sessions returns 200');
  const rangeSessionsData = parseContent(rangeSessions.body);
  let firstCompletedRangeSessionId: string | undefined;
  if (
    rangeSessionsData?.sessions &&
    Array.isArray(rangeSessionsData.sessions)
  ) {
    pass('list_range_sessions returns a "sessions" array');
    if (rangeSessionsData.sessions.length > 0) {
      const first = rangeSessionsData.sessions[0] as Record<string, unknown>;
      assert(
        typeof first?.id === 'string',
        'range session entry has an "id" string field',
      );
      assert(
        'balls_hit' in first &&
          'blocks_with_results' in first &&
          'has_observations' in first,
        'range session summary carries capture fields',
      );
      firstCompletedRangeSessionId = first.id as string;
    } else {
      console.log(
        '  ⏭️  No completed range sessions yet (empty-but-valid interim state)',
      );
    }
  } else {
    fail('list_range_sessions response contains a "sessions" array');
  }

  // ---- get_range_session ----
  console.log('\n🔎 get_range_session');
  // A well-formed but nonexistent id must return RANGE_SESSION_NOT_FOUND.
  const missing = await callTool('get_range_session', {
    range_session_id: '00000000-0000-0000-0000-000000000000',
  });
  assert(missing.status === 200, 'get_range_session returns 200 (transport)');
  const missingData = parseContent(missing.body);
  assert(
    missing.body?.result?.isError === true &&
      missingData?.code === 'RANGE_SESSION_NOT_FOUND',
    'get_range_session returns RANGE_SESSION_NOT_FOUND for a nonexistent id',
  );
  if (firstCompletedRangeSessionId) {
    const detail = await callTool('get_range_session', {
      range_session_id: firstCompletedRangeSessionId,
    });
    assert(detail.status === 200, 'get_range_session returns 200');
    const detailData = parseContent(detail.body);
    assert(
      Array.isArray(detailData?.blocks),
      'get_range_session returns a "blocks" array',
    );
  } else {
    console.log('  ⏭️  Skipping detail check (no completed range session)');
  }

  // ---- Auth isolation check (S6) ----
  if (SECOND_TOKEN) {
    console.log('\n🔒 Auth isolation (S6)');
    const otherUnits = await callTool('list_units', {}, SECOND_TOKEN);
    assert(otherUnits.status === 200, 'second user list_units returns 200');
    const otherUnitsData = parseContent(otherUnits.body);
    if (otherUnitsData?.units && Array.isArray(otherUnitsData.units)) {
      // The regression unit we just created should NOT be visible to the second user
      const hasOurUnit = (
        otherUnitsData.units as Array<Record<string, unknown>>
      ).some(u => u.id === createdUnitId);
      assert(!hasOurUnit, "second user cannot see first user's units");
    } else {
      fail('second user list_units returns a "units" array');
    }

    const otherSessions = await callTool('list_sessions', {}, SECOND_TOKEN);
    assert(
      otherSessions.status === 200,
      'second user list_sessions returns 200',
    );

    // Second user can create their own unit
    const otherUnit = await callTool(
      'create_unit',
      {
        title: `[TEST] Second user unit ${timestamp}`,
        instructions: [
          { order: 1, text: 'Second user instruction', ball_count: 5 },
        ],
      },
      SECOND_TOKEN,
    );
    assert(otherUnit.status === 200, 'second user create_unit returns 200');
    const otherUnitData = parseContent(otherUnit.body);
    if (otherUnitData?.unit_id && typeof otherUnitData.unit_id === 'string') {
      pass('second user create_unit returns their own unit_id');
    } else {
      fail('second user create_unit returns a unit_id');
    }

    // Verify first user still can't see second user's data
    const ourUnitsAgain = await callTool('list_units');
    assert(ourUnitsAgain.status === 200, 'first user list_units still works');
    const ourUnitsData = parseContent(ourUnitsAgain.body);
    if (ourUnitsData?.units && Array.isArray(ourUnitsData.units)) {
      const hasSecondUserUnit = (
        ourUnitsData.units as Array<Record<string, unknown>>
      ).some(u => u.id === otherUnitData?.unit_id);
      assert(!hasSecondUserUnit, "first user cannot see second user's units");
    }
  } else {
    console.log('\n⏭️  Skipping auth isolation check (set SECOND_TEST_TOKEN)');
  }

  // ---- Summary ----
  console.log('\n' + '='.repeat(60));
  console.log(`Results: ${passCount} passed, ${failCount} failed`);
  console.log('='.repeat(60));

  if (failCount > 0) {
    process.exitCode = 1;
  }
}

main().catch(err => {
  console.error('Unhandled error:', err);
  process.exitCode = 1;
});
