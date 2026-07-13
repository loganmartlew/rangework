import type { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import type { UserContext } from '../auth/userContext.js';
import { z } from 'zod';
import type { CallToolResult } from '@modelcontextprotocol/sdk/types.js';
import { toolError, ErrorCodes } from '../validation/tool-errors.js';

const sessionIdInput = {
  session_id: z.string().describe("The session's `id` from `list_sessions`."),
};

/**
 * Shared write path for `archive_session` / `unarchive_session`: both are a
 * same-owner single-column update on `archived_at`, re-read via `select` so
 * the model can confirm the outcome. RLS scopes the row to the caller, so a
 * well-formed id belonging to another user is indistinguishable from a
 * nonexistent one — both resolve to zero matched rows.
 */
async function setArchived(
  ctx: UserContext,
  sessionId: string,
  archived: boolean,
): Promise<CallToolResult> {
  const { data: session, error } = await ctx.supabaseClient
    .from('practice_sessions')
    .update({ archived_at: archived ? new Date().toISOString() : null })
    .eq('id', sessionId)
    .select('id, name, archived_at')
    .maybeSingle();

  if (error) {
    // A malformed id (not a valid uuid) makes Postgres raise 22P02 rather
    // than matching no row. Treat it the same as a well-formed id that
    // matches nothing (see get_range_session) — a bad id never leaks a
    // scary transport error.
    if (error.code === '22P02') {
      return toolError(
        ErrorCodes.SESSION_NOT_FOUND,
        'session not found or does not belong to you',
      );
    }
    return toolError(
      ErrorCodes.DATABASE_ERROR,
      `Failed to ${archived ? 'archive' : 'unarchive'} session.`,
    );
  }

  if (!session) {
    return toolError(
      ErrorCodes.SESSION_NOT_FOUND,
      'session not found or does not belong to you',
    );
  }

  return {
    content: [
      {
        type: 'text' as const,
        text: JSON.stringify({
          session: {
            id: session.id,
            name: session.name,
            archived: session.archived_at != null,
          },
        }),
      },
    ],
  };
}

/**
 * Tools: `archive_session` / `unarchive_session`
 *
 * Two explicit verbs over the archived lifecycle state landed in Stage 1
 * (design §5 rejected a generic state-setting tool, so the model sees two
 * distinct verbs rather than a boolean it could get backwards). Mirror
 * operations sharing the `setArchived` write path above.
 */
export function registerArchiveSessionTools(
  server: McpServer,
  ctx: UserContext,
): void {
  server.registerTool(
    'archive_session',
    {
      description:
        'Archive a practice session: hide it from the default list_sessions view while keeping it fully re-runnable and its range-session history intact. Archived is not deleted — the session can be viewed, duplicated, and unarchived at any time. Safe to call even if a range session is currently in progress. Only archive when the player asks to tidy up. Returns SESSION_NOT_FOUND if the session does not exist or does not belong to you.',
      inputSchema: sessionIdInput,
    },
    async (args): Promise<CallToolResult> => {
      const sessionId = args.session_id?.trim();
      if (!sessionId) {
        return toolError(
          ErrorCodes.VALIDATION_ERROR,
          'session_id must not be empty',
          { field: 'session_id' },
        );
      }
      return setArchived(ctx, sessionId, true);
    },
  );

  server.registerTool(
    'unarchive_session',
    {
      description:
        'Unarchive a previously archived practice session, returning it to the default list_sessions view so it can be started or edited again. Call this when the player wants to reuse an archived session. Returns SESSION_NOT_FOUND if the session does not exist or does not belong to you.',
      inputSchema: sessionIdInput,
    },
    async (args): Promise<CallToolResult> => {
      const sessionId = args.session_id?.trim();
      if (!sessionId) {
        return toolError(
          ErrorCodes.VALIDATION_ERROR,
          'session_id must not be empty',
          { field: 'session_id' },
        );
      }
      return setArchived(ctx, sessionId, false);
    },
  );
}
