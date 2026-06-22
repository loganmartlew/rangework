import type { R2Bucket } from '@cloudflare/workers-types';

const DEFAULT_METHODOLOGY =
  '# Rangework Coaching Guide\n\nmethodology_version: "1.0.0"';

/**
 * Create a minimal R2Bucket mock for tests.
 *
 * @param getText - The text to return, or `null` to simulate a missing object.
 * @returns A mock R2Bucket.
 */
export function mockR2Bucket(
  getText: string | null = DEFAULT_METHODOLOGY,
): R2Bucket {
  return {
    get: async () => {
      if (getText === null) return null;
      return {
        text: async () => getText,
      } as R2ObjectBody;
    },
  } as unknown as R2Bucket;
}
