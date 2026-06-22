import { describe, expect, it } from 'vitest';
import { fetchAllClubCodes, validateClubCode } from '../validation/club-codes.js';
import type { UserContext } from '../auth/userContext.js';

function makeMockClient(codes: string[]): UserContext['supabaseClient'] {
  return {
    from: () => ({
      select: () => ({
        order: async () => ({
          data: codes.map(code => ({ code })),
          error: null,
        }),
      }),
    }),
  } as unknown as UserContext['supabaseClient'];
}

describe('fetchAllClubCodes', () => {
  it('returns codes in catalog order', async () => {
    const client = makeMockClient(['driver', 'three_wood', 'seven_iron', 'putter']);
    const codes = await fetchAllClubCodes(client);
    expect(codes).toEqual(['driver', 'three_wood', 'seven_iron', 'putter']);
  });

  it('returns empty array when catalog is empty', async () => {
    const client = makeMockClient([]);
    const codes = await fetchAllClubCodes(client);
    expect(codes).toEqual([]);
  });

  it('throws when supabase returns an error', async () => {
    const client = {
      from: () => ({
        select: () => ({
          order: async () => ({ data: null, error: { message: 'connection refused' } }),
        }),
      }),
    } as unknown as UserContext['supabaseClient'];

    await expect(fetchAllClubCodes(client)).rejects.toThrow('connection refused');
  });
});

describe('validateClubCode', () => {
  const allCodes = ['driver', 'three_wood', 'seven_iron', 'putter'];

  it('returns null for a valid code', () => {
    const result = validateClubCode('seven_iron', allCodes, 'default_club_reference');
    expect(result).toBeNull();
  });

  it('returns UNKNOWN_CLUB_CODE error for an unknown code', () => {
    const result = validateClubCode('bogus_club', allCodes, 'default_club_reference');
    expect(result).not.toBeNull();
    expect(result!.isError).toBe(true);

    const body = JSON.parse((result!.content[0] as { type: string; text: string }).text);
    expect(body.code).toBe('UNKNOWN_CLUB_CODE');
    expect(body.message).toContain('bogus_club');
    expect(body.data.field).toBe('default_club_reference');
  });

  it('includes valid_codes from allCodes when no userEnabledCodes provided', () => {
    const result = validateClubCode('bogus_club', allCodes, 'default_club_reference');
    const body = JSON.parse((result!.content[0] as { type: string; text: string }).text);
    expect(body.data.valid_codes).toEqual(allCodes);
  });

  it('includes userEnabledCodes in valid_codes hint when provided', () => {
    const userCodes = ['seven_iron', 'putter'];
    const result = validateClubCode('bogus_club', allCodes, 'default_club_reference', userCodes);
    const body = JSON.parse((result!.content[0] as { type: string; text: string }).text);
    expect(body.data.valid_codes).toEqual(userCodes);
  });

  it('uses the correct field name in the error', () => {
    const result = validateClubCode('bogus_club', allCodes, 'items[2].club_reference');
    const body = JSON.parse((result!.content[0] as { type: string; text: string }).text);
    expect(body.data.field).toBe('items[2].club_reference');
  });
});
