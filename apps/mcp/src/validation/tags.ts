import type { SupabaseClient } from '@supabase/supabase-js';
import type { CallToolResult } from '@modelcontextprotocol/sdk/types.js';
import { toolError, ErrorCodes } from './tool-errors.js';

/** The maximum number of tags that may be attached to a single Unit or Session. */
export const MAX_TAGS_PER_ITEM = 8;

/**
 * Generate a Tag Code (lowercase-underscore slug) from a raw name.
 *
 * Lowercases, collapses every run of non-alphanumeric characters into a single
 * underscore, and strips leading/trailing underscores. Returns `null` when the
 * input has no alphanumeric content.
 *
 * Mirrors `slugify_tag` in SQL and `slugifyTag` in shared Kotlin — keep in sync.
 */
export function slugifyTag(raw: string): string | null {
  const slug = raw
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '');
  return slug.length > 0 ? slug : null;
}

export interface TagRecord {
  id: string;
  code: string;
  display_name: string;
  owner_id: string | null;
}

/**
 * Fetch every tag visible to the user (Default Tags + the user's Custom Tags).
 * RLS scopes the result to the authenticated user.
 */
export async function fetchVisibleTags(
  supabaseClient: SupabaseClient,
): Promise<TagRecord[]> {
  const { data, error } = await supabaseClient
    .from('tags')
    .select('id, code, display_name, owner_id')
    .order('display_name', { ascending: true });

  if (error) {
    throw new Error(`Failed to fetch tags: ${error.message}`);
  }

  return (data ?? []) as TagRecord[];
}

/**
 * Resolve an array of existing tag codes to tag ids for a write tool.
 *
 * Returns `{ ids }` on success or `{ error }` (a structured `CallToolResult`)
 * when a code is unknown or the per-item cap is exceeded. Never creates a tag —
 * minting a Custom Tag is the deliberate job of the `create_tag` tool.
 */
export async function resolveTagCodes(
  supabaseClient: SupabaseClient,
  codes: string[],
  field: string,
): Promise<{ ids: string[] } | { error: CallToolResult }> {
  if (codes.length > MAX_TAGS_PER_ITEM) {
    return {
      error: toolError(
        ErrorCodes.VALIDATION_ERROR,
        `at most ${MAX_TAGS_PER_ITEM} tags can be attached`,
        { field },
      ),
    };
  }

  let tags: TagRecord[];
  try {
    tags = await fetchVisibleTags(supabaseClient);
  } catch {
    return {
      error: toolError(
        ErrorCodes.DATABASE_ERROR,
        'Failed to validate tag codes. Please try again.',
      ),
    };
  }

  const idByCode = new Map(tags.map(t => [t.code, t.id]));
  const ids: string[] = [];
  for (const code of codes) {
    const id = idByCode.get(code);
    if (!id) {
      return {
        error: toolError(
          ErrorCodes.UNKNOWN_TAG_CODE,
          `Unknown tag code: ${code}`,
          { field, valid_codes: tags.map(t => t.code) },
        ),
      };
    }
    if (!ids.includes(id)) ids.push(id);
  }

  return { ids };
}
