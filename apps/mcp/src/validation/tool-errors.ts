import type { CallToolResult } from '@modelcontextprotocol/sdk/types.js';

/**
 * Structured error shape for all tool errors (X2).
 */
export interface ToolError {
  code: string;
  message: string;
  data?: {
    field?: string;
    valid_codes?: string[];
    invalid_unit_ids?: string[];
  };
}

/**
 * Error codes used across tools.
 */
export const ErrorCodes = {
  VALIDATION_ERROR: 'VALIDATION_ERROR',
  UNKNOWN_CLUB_CODE: 'UNKNOWN_CLUB_CODE',
  UNKNOWN_TAG_CODE: 'UNKNOWN_TAG_CODE',
  UNIT_NOT_FOUND: 'UNIT_NOT_FOUND',
  RANGE_SESSION_NOT_FOUND: 'RANGE_SESSION_NOT_FOUND',
  SESSION_NOT_FOUND: 'SESSION_NOT_FOUND',
  DATABASE_ERROR: 'DATABASE_ERROR',
  CONTENT_UNAVAILABLE: 'CONTENT_UNAVAILABLE',
} as const;

/**
 * Factory function to create a structured tool error response.
 *
 * All tool errors use the MCP `isError: true` flag and include a structured
 * JSON body with `code`, `message`, and optional `data` fields.
 */
export function toolError(
  code: string,
  message: string,
  data?: ToolError['data'],
): CallToolResult {
  const body: ToolError = { code, message };
  if (data) body.data = data;
  return {
    content: [{ type: 'text', text: JSON.stringify(body) }],
    isError: true,
  };
}
