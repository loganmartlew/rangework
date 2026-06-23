import type { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import type { R2Bucket } from '@cloudflare/workers-types';
import { z } from 'zod';
import type { GetPromptResult } from '@modelcontextprotocol/sdk/types.js';
import { loadMethodology } from '../methodology/loader.js';

/**
 * Register the `build_practice_plan` MCP prompt.
 *
 * The prompt returns the full coaching methodology as a single `user` role
 * message. If the optional `focus` argument is provided, it is appended to
 * the methodology text so the LLM knows the user's primary focus area.
 */
export function registerBuildPracticePlanPrompt(
  server: McpServer,
  bucket: R2Bucket,
): void {
  server.registerPrompt(
    'build_practice_plan',
    {
      description:
        'Plan a focused, purposeful golf practice session. Guides you through a conversation to understand your game, then creates drills and a session plan in your Rangework account.',
      argsSchema: {
        focus: z
          .string()
          .optional()
          .describe(
            'Optional primary focus for the session (e.g. "driver distance", "putting").',
          ),
      },
    },
    async ({ focus }): Promise<GetPromptResult> => {
      const methodology = await loadMethodology(bucket);

      if (!methodology) {
        return {
          messages: [
            {
              role: 'user',
              content: {
                type: 'text',
                text: 'The coaching methodology is temporarily unavailable. Please try again later.',
              },
            },
          ],
        };
      }

      let text = methodology;
      if (focus) {
        text += `\n\nThe user wants to focus on: ${focus}`;
      }

      return {
        messages: [
          {
            role: 'user',
            content: { type: 'text', text },
          },
        ],
      };
    },
  );
}
