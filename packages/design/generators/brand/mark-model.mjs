import fs from 'node:fs/promises';
import path from 'node:path';
import { parse } from 'svgson';

/**
 * Parse the canonical base SVG into a format-agnostic geometry model. Every
 * brand emitter builds its target format from this model — none text-transforms
 * one output format into another. That is what lets the Android emitter emit
 * `@color/...` token references rather than baked hex.
 *
 * @typedef {object} MarkModel
 * @property {[number, number, number, number]} viewBox
 * @property {string} transform  e.g. "translate(0,4)"
 * @property {{ role: 'band' | 'rod', opacity: number, d: string }[]} paths
 *
 * @param {string} packageDir absolute package root
 * @returns {Promise<MarkModel>}
 */
export async function parseMarkModel(packageDir) {
  const raw = await fs.readFile(path.join(packageDir, 'brand', 'mark.base.svg'), 'utf8');
  // Strip XML comments so the documentation block doesn't confuse the parser.
  const svg = raw.replace(/<!--[\s\S]*?-->/g, '');
  const root = await parse(svg);

  const viewBox = root.attributes.viewBox.trim().split(/[\s,]+/).map(Number);
  if (viewBox.length !== 4) {
    throw new Error(`mark.base.svg: expected a 4-value viewBox, got "${root.attributes.viewBox}"`);
  }

  const group = root.children.find(node => node.name === 'g');
  if (!group) throw new Error('mark.base.svg: expected a single <g> wrapper');

  const paths = group.children
    .filter(node => node.name === 'path')
    .map(node => {
      const role = node.attributes['data-role'];
      if (role !== 'band' && role !== 'rod') {
        throw new Error(`mark.base.svg: path missing valid data-role (got "${role}")`);
      }
      return {
        role,
        opacity: Number(node.attributes['fill-opacity'] ?? '1'),
        d: node.attributes.d,
      };
    });

  return {
    viewBox: /** @type {[number, number, number, number]} */ (viewBox),
    transform: group.attributes.transform ?? '',
    paths,
  };
}
