import { describe, expect, it, beforeEach } from 'vitest';
import { loadMethodology, _resetCache } from '../methodology/loader.js';
import type { R2Bucket } from '@cloudflare/workers-types';

/**
 * Create a minimal R2Bucket mock for testing.
 */
function createMockR2Bucket(
  getText: string | null | (() => string | null),
  throwError = false,
): R2Bucket {
  const resolveText = typeof getText === 'function' ? getText() : getText;

  return {
    get: async () => {
      if (throwError) throw new Error('R2 fetch failed');
      if (resolveText === null) return null;
      return {
        text: async () => resolveText,
      } as R2ObjectBody;
    },
  } as unknown as R2Bucket;
}

describe('methodology loader', () => {
  beforeEach(() => {
    _resetCache();
  });

  it('returns methodology text when object exists in R2', async () => {
    const mockMethodology =
      '# Rangework Coaching Guide\n\nmethodology_version: "1.0.0"';
    const bucket = createMockR2Bucket(mockMethodology);

    const result = await loadMethodology(bucket);
    expect(result).toBe(mockMethodology);
  });

  it('returns null when object is missing in R2', async () => {
    const bucket = createMockR2Bucket(null);

    const result = await loadMethodology(bucket);
    expect(result).toBeNull();
  });

  it('returns cached text on second call without fetching from R2', async () => {
    let getCallCount = 0;
    const bucket = createMockR2Bucket(() => {
      getCallCount++;
      return 'methodology text';
    });

    const result1 = await loadMethodology(bucket);
    const result2 = await loadMethodology(bucket);

    expect(result1).toBe('methodology text');
    expect(result2).toBe('methodology text');
    expect(getCallCount).toBe(1);
  });

  it('returns null when R2 fetch throws an error', async () => {
    const bucket = createMockR2Bucket('ignored', true);

    const result = await loadMethodology(bucket);
    expect(result).toBeNull();
  });

  it('does not cache null result on R2 error', async () => {
    // First call: mock throws, returns null (not cached)
    const errorBucket = createMockR2Bucket('ignored', true);
    const result1 = await loadMethodology(errorBucket);
    expect(result1).toBeNull();

    // After reset, a good bucket should work
    _resetCache();
    const goodBucket = createMockR2Bucket('good methodology text');
    const result2 = await loadMethodology(goodBucket);
    expect(result2).toBe('good methodology text');
  });
});
