import { describe, expect, it } from 'vitest';
import { toolError, ErrorCodes, type ToolError } from '../validation/tool-errors.js';

describe('toolError', () => {
  it('produces correct shape with code and message', () => {
    const result = toolError(ErrorCodes.VALIDATION_ERROR, 'title must not be empty');

    expect(result.isError).toBe(true);
    expect(result.content).toHaveLength(1);
    expect(result.content[0]?.type).toBe('text');

    const body = JSON.parse((result.content[0] as { type: string; text: string }).text) as ToolError;
    expect(body.code).toBe('VALIDATION_ERROR');
    expect(body.message).toBe('title must not be empty');
    expect(body.data).toBeUndefined();
  });

  it('includes data when provided', () => {
    const result = toolError(ErrorCodes.VALIDATION_ERROR, 'title must not be empty', {
      field: 'title',
    });

    const body = JSON.parse((result.content[0] as { type: string; text: string }).text) as ToolError;
    expect(body.data).toEqual({ field: 'title' });
  });

  it('includes valid_codes in data when provided', () => {
    const result = toolError(ErrorCodes.UNKNOWN_CLUB_CODE, 'Unknown club code: bogus', {
      field: 'default_club_code',
      valid_codes: ['driver', 'seven_iron'],
    });

    const body = JSON.parse((result.content[0] as { type: string; text: string }).text) as ToolError;
    expect(body.code).toBe('UNKNOWN_CLUB_CODE');
    expect(body.data?.field).toBe('default_club_code');
    expect(body.data?.valid_codes).toEqual(['driver', 'seven_iron']);
  });

  it('includes invalid_unit_ids in data when provided', () => {
    const result = toolError(ErrorCodes.UNIT_NOT_FOUND, 'unit abc not found or does not belong to you', {
      invalid_unit_ids: ['abc', 'def'],
    });

    const body = JSON.parse((result.content[0] as { type: string; text: string }).text) as ToolError;
    expect(body.code).toBe('UNIT_NOT_FOUND');
    expect(body.data?.invalid_unit_ids).toEqual(['abc', 'def']);
  });

  it('omits data key entirely when not provided', () => {
    const result = toolError(ErrorCodes.DATABASE_ERROR, 'Failed to create unit. Please try again.');

    const body = JSON.parse((result.content[0] as { type: string; text: string }).text) as ToolError;
    expect(Object.prototype.hasOwnProperty.call(body, 'data')).toBe(false);
  });

  it('ErrorCodes contains all expected codes', () => {
    expect(ErrorCodes.VALIDATION_ERROR).toBe('VALIDATION_ERROR');
    expect(ErrorCodes.UNKNOWN_CLUB_CODE).toBe('UNKNOWN_CLUB_CODE');
    expect(ErrorCodes.UNIT_NOT_FOUND).toBe('UNIT_NOT_FOUND');
    expect(ErrorCodes.DATABASE_ERROR).toBe('DATABASE_ERROR');
  });
});
