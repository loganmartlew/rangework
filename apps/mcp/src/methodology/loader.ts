/**
 * Methodology loader — fetches the coaching methodology markdown from R2
 * and caches it in-memory within the Worker isolate.
 *
 * Shared by the `build_practice_plan` prompt and the `get_coaching_guide`
 * fallback tool. The cache naturally expires when the Worker isolate is
 * evicted (typically seconds to minutes on Cloudflare Workers).
 */

const R2_KEY = 'mcp/coaching-guide.md';

let cachedMethodology: string | null = null;

/**
 * Load the coaching methodology from R2.
 *
 * Returns the methodology text on success, or `null` if the object doesn't
 * exist or the fetch fails. Subsequent calls within the same isolate reuse
 * the cached value.
 */
export async function loadMethodology(
  bucket: R2Bucket,
): Promise<string | null> {
  if (cachedMethodology !== null) return cachedMethodology;

  try {
    const object = await bucket.get(R2_KEY);
    console.log(bucket);
    if (!object) return null;

    const text = await object.text();
    if (!text) return null;

    cachedMethodology = text;
    return text;
  } catch {
    return null;
  }
}

/**
 * Reset the in-memory cache. Test-only utility.
 */
export function _resetCache(): void {
  cachedMethodology = null;
}
