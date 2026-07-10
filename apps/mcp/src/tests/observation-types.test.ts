import { describe, expect, it } from 'vitest';
import {
  OBSERVATION_TYPES,
  isObservationType,
  validateObservationTypes,
} from '../validation/observation-types.js';

describe('observation-types vocabulary helper', () => {
  it('exposes exactly the six fixed type identifiers', () => {
    expect([...OBSERVATION_TYPES]).toEqual([
      'success',
      'strike_location',
      'contact',
      'shape',
      'distance',
      'direction',
    ]);
  });

  it('recognizes known identifiers and rejects unknown ones', () => {
    expect(isObservationType('shape')).toBe(true);
    expect(isObservationType('Success')).toBe(false);
    expect(isObservationType('spin')).toBe(false);
  });

  it('returns an empty array for absent or empty input', () => {
    expect(validateObservationTypes(undefined, 'field')).toEqual({ types: [] });
    expect(validateObservationTypes([], 'field')).toEqual({ types: [] });
  });

  it('passes through valid types, preserving order', () => {
    const result = validateObservationTypes(['shape', 'success'], 'field');
    expect(result).toEqual({ types: ['shape', 'success'] });
  });

  it('deduplicates repeated types silently', () => {
    const result = validateObservationTypes(
      ['shape', 'shape', 'success'],
      'field',
    );
    expect(result).toEqual({ types: ['shape', 'success'] });
  });

  it('rejects an unknown type with a field-scoped error listing the vocabulary', () => {
    const result = validateObservationTypes(['shape', 'spin'], 'items[1].observation_types');
    expect('error' in result).toBe(true);
    if (!('error' in result)) return;
    const content = result.error.content as Array<{ text?: string }>;
    const parsed = JSON.parse(content[0]?.text ?? '{}');
    expect(parsed.code).toBe('VALIDATION_ERROR');
    expect(parsed.message).toContain('spin');
    expect(parsed.data.field).toBe('items[1].observation_types');
    expect(parsed.data.valid_codes).toEqual([...OBSERVATION_TYPES]);
  });
});
