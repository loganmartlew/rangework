import type { CallToolResult } from '@modelcontextprotocol/sdk/types.js';
import type { UserContext } from '../auth/userContext.js';
import { toolError, ErrorCodes } from './tool-errors.js';
import { fetchAllClubCodes, validateClubCode } from './club-codes.js';
import { resolveTagCodes } from './tags.js';

export interface InlineUnitInput {
  title: string;
  instructions: Array<{
    order: number;
    text: string;
    ball_count?: number;
    club_code?: string;
  }>;
  focus?: string;
  notes?: string;
  default_club_code?: string;
  success_criterion?: string;
  tag_codes?: string[];
}

export interface NormalizedInstruction {
  order: number;
  text: string;
  ball_count?: number;
  club_code?: string;
}

export interface ValidatedInlineUnit {
  title: string;
  instructions: NormalizedInstruction[];
  focus?: string;
  notes?: string;
  defaultClubCode?: string;
  successCriterion?: string;
  tagIds: string[];
}

function scopedField(prefix: string, suffix: string): string {
  return prefix ? `${prefix}.${suffix}` : suffix;
}

/**
 * Validate a `create_unit`-shaped drill definition: title, instructions
 * (order/text/ball_count/club_code), club codes against the catalog, and
 * tag_codes against the vocabulary. Shared by the `create_unit` tool and
 * `create_session`'s embedded `inline_unit` items so the two paths validate
 * identically and can't drift.
 *
 * `fieldPrefix` scopes validation-error field names to the caller's payload
 * shape — empty for `create_unit`; `items[idx].inline_unit` for an embedded
 * inline item.
 */
export async function validateInlineUnit(
  ctx: UserContext,
  input: InlineUnitInput,
  fieldPrefix: string,
): Promise<{ unit: ValidatedInlineUnit } | { error: CallToolResult }> {
  if (!input.instructions || input.instructions.length === 0) {
    return {
      error: toolError(
        ErrorCodes.VALIDATION_ERROR,
        'at least one instruction is required',
        { field: scopedField(fieldPrefix, 'instructions') },
      ),
    };
  }
  if (input.instructions.length > 10) {
    return {
      error: toolError(
        ErrorCodes.VALIDATION_ERROR,
        'a unit may have at most 10 instructions',
        { field: scopedField(fieldPrefix, 'instructions') },
      ),
    };
  }

  const title = input.title.trim();
  if (!title) {
    return {
      error: toolError(
        ErrorCodes.VALIDATION_ERROR,
        'title must not be empty',
        { field: scopedField(fieldPrefix, 'title') },
      ),
    };
  }

  const instructions: NormalizedInstruction[] = [];
  for (let idx = 0; idx < input.instructions.length; idx++) {
    const inst = input.instructions[idx]!;
    const text = inst.text.trim();
    if (!text) {
      return {
        error: toolError(
          ErrorCodes.VALIDATION_ERROR,
          'instruction text must not be empty',
          { field: scopedField(fieldPrefix, `instructions[${idx}].text`) },
        ),
      };
    }
    if (!Number.isInteger(inst.order) || inst.order < 1) {
      return {
        error: toolError(
          ErrorCodes.VALIDATION_ERROR,
          'instruction order must be a positive integer',
          { field: scopedField(fieldPrefix, `instructions[${idx}].order`) },
        ),
      };
    }
    if (
      inst.ball_count !== undefined &&
      (!Number.isInteger(inst.ball_count) || inst.ball_count < 0)
    ) {
      return {
        error: toolError(
          ErrorCodes.VALIDATION_ERROR,
          'ball_count must not be negative',
          { field: scopedField(fieldPrefix, `instructions[${idx}].ball_count`) },
        ),
      };
    }
    // Treat a blank per-instruction club_code as "use default" (absent).
    const clubCode = inst.club_code?.trim() ? inst.club_code.trim() : undefined;
    instructions.push({
      order: inst.order,
      text,
      ball_count: inst.ball_count,
      club_code: clubCode,
    });
  }

  // Check for duplicate order values
  const orderValues = instructions.map(i => i.order);
  const uniqueOrders = new Set(orderValues);
  if (uniqueOrders.size !== orderValues.length) {
    return {
      error: toolError(
        ErrorCodes.VALIDATION_ERROR,
        'instruction order values must be unique',
        { field: scopedField(fieldPrefix, 'instructions') },
      ),
    };
  }

  // Validate every club code provided — the unit default plus any
  // per-instruction club — against the catalog in a single fetch. This is
  // the primary guard; the RPC's FK is a safety net the caller may add.
  const clubCodesToValidate: Array<{ code: string; field: string }> = [];
  if (input.default_club_code) {
    clubCodesToValidate.push({
      code: input.default_club_code,
      field: scopedField(fieldPrefix, 'default_club_code'),
    });
  }
  instructions.forEach((inst, idx) => {
    if (inst.club_code) {
      clubCodesToValidate.push({
        code: inst.club_code,
        field: scopedField(fieldPrefix, `instructions[${idx}].club_code`),
      });
    }
  });

  if (clubCodesToValidate.length > 0) {
    let allCodes: string[];
    try {
      allCodes = await fetchAllClubCodes(ctx.supabaseClient);
    } catch {
      return {
        error: toolError(
          ErrorCodes.DATABASE_ERROR,
          'Failed to validate club code. Please try again.',
        ),
      };
    }
    for (const { code, field } of clubCodesToValidate) {
      const clubError = validateClubCode(code, allCodes, field);
      if (clubError) return { error: clubError };
    }
  }

  // Resolve tag codes to ids (rejects unknown codes; never creates tags)
  let tagIds: string[] = [];
  if (input.tag_codes && input.tag_codes.length > 0) {
    const resolved = await resolveTagCodes(
      ctx.supabaseClient,
      input.tag_codes,
      scopedField(fieldPrefix, 'tag_codes'),
    );
    if ('error' in resolved) return { error: resolved.error };
    tagIds = resolved.ids;
  }

  const unit: ValidatedInlineUnit = { title, instructions, tagIds };
  if (input.focus !== undefined) unit.focus = input.focus;
  if (input.notes !== undefined) unit.notes = input.notes;
  if (input.default_club_code !== undefined)
    unit.defaultClubCode = input.default_club_code;
  if (input.success_criterion !== undefined)
    unit.successCriterion = input.success_criterion;

  return { unit };
}

/**
 * Build the jsonb-ready instructions array for a validated inline unit,
 * omitting the `ball_count` / `club_code` keys the definition didn't provide.
 */
export function buildInlineUnitJsonb(
  instructions: NormalizedInstruction[],
): Array<Record<string, unknown>> {
  return instructions.map(inst => {
    const obj: Record<string, unknown> = {
      order: inst.order,
      text: inst.text,
    };
    if (inst.ball_count !== undefined) {
      obj.ball_count = inst.ball_count;
    }
    if (inst.club_code !== undefined) {
      obj.club_code = inst.club_code;
    }
    return obj;
  });
}
