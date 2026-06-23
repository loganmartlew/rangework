import type { SupabaseClient } from '@supabase/supabase-js';
import type { CallToolResult } from '@modelcontextprotocol/sdk/types.js';
import { toolError, ErrorCodes } from './tool-errors.js';

/**
 * Fetch all club codes from the catalog.
 *
 * Returns the full list of valid club codes ordered by `sort_order ASC`.
 * This is used to validate club references in write tools.
 */
export async function fetchAllClubCodes(
  supabaseClient: SupabaseClient,
): Promise<string[]> {
  const { data, error } = await supabaseClient
    .from('clubs')
    .select('code')
    .order('sort_order', { ascending: true });

  if (error) {
    throw new Error(`Failed to fetch club codes: ${error.message}`);
  }

  return data?.map(c => c.code) ?? [];
}

/**
 * Validate a single club code against the catalog.
 *
 * Returns a structured error `CallToolResult` if the code is not found,
 * or `null` if the code is valid. The error includes the user's enabled
 * club codes as a convenience hint for the LLM.
 */
export function validateClubCode(
  code: string,
  allCodes: string[],
  field: string,
  userEnabledCodes?: string[],
): CallToolResult | null {
  if (!allCodes.includes(code)) {
    return toolError(
      ErrorCodes.UNKNOWN_CLUB_CODE,
      `Unknown club code: ${code}`,
      {
        field,
        valid_codes: userEnabledCodes ?? allCodes,
      },
    );
  }
  return null;
}
