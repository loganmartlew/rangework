import { tokensGenerator } from './generators/tokens/index.mjs';
import { brandGenerator } from './generators/brand/index.mjs';

/**
 * Ordered list of design-asset generators. Adding a capability = new generator
 * module + one line here. Generators run sequentially over a shared
 * BuildContext (see scripts/build.mjs); there is no dependency graph because the
 * pre-resolved `ctx.tokens` removes the only ordering coupling.
 */
export default { generators: [tokensGenerator(), brandGenerator()] };
