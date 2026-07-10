import type { CallToolResult } from '@modelcontextprotocol/sdk/types.js';
import { toolError, ErrorCodes } from './tool-errors.js';

/**
 * The fixed Observation Type vocabulary (design §5.1, Stage 1 schema).
 *
 * These six identifiers are the canonical wire values shared across the DB
 * (`practice_session_items.observation_types`, `range_session_observations`
 * value keys, snapshot v3 `observationTypes`) and the Stage 2 enum. Mirrors
 * the SQL CHECK constraint in the `range_session_data_recording` migration —
 * keep in sync. `success` is special: it may only be enabled on an item whose
 * unit carries a success criterion (enforced separately in `create_session`).
 */
export const OBSERVATION_TYPES = [
  'success',
  'strike_location',
  'contact',
  'shape',
  'distance',
  'direction',
] as const;

export type ObservationType = (typeof OBSERVATION_TYPES)[number];

/** True when `value` is one of the six known Observation Type identifiers. */
export function isObservationType(value: string): value is ObservationType {
  return (OBSERVATION_TYPES as readonly string[]).includes(value);
}

/**
 * Validate and normalize a raw `observation_types` array for one session item.
 *
 * Returns `{ types }` with duplicates removed (order preserved) on success, or
 * `{ error }` (a structured `CallToolResult`) when any entry is not in the
 * vocabulary. An empty or absent array yields `{ types: [] }` — no per-ball
 * capture, the default. The `success`-requires-criterion rule is enforced by
 * the caller, which alone knows each item's unit criterion.
 */
export function validateObservationTypes(
  raw: string[] | undefined,
  field: string,
): { types: ObservationType[] } | { error: CallToolResult } {
  if (!raw || raw.length === 0) return { types: [] };

  const types: ObservationType[] = [];
  for (const value of raw) {
    if (!isObservationType(value)) {
      return {
        error: toolError(
          ErrorCodes.VALIDATION_ERROR,
          `Unknown observation type: ${value}`,
          { field, valid_codes: [...OBSERVATION_TYPES] },
        ),
      };
    }
    if (!types.includes(value)) types.push(value);
  }

  return { types };
}
