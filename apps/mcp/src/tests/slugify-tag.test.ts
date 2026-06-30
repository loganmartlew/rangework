import { describe, expect, it } from 'vitest';
import { slugifyTag } from '../validation/tags.js';

describe('slugifyTag', () => {
  it('lowercases and underscores spaces', () => {
    expect(slugifyTag('Short Game')).toBe('short_game');
  });

  it('collapses punctuation and whitespace runs', () => {
    expect(slugifyTag('  Short — Game!!  ')).toBe('short_game');
    expect(slugifyTag('short___game')).toBe('short_game');
    expect(slugifyTag('Short\tGame')).toBe('short_game');
  });

  it('strips leading and trailing non-alphanumerics', () => {
    expect(slugifyTag('**putting**')).toBe('putting');
    expect(slugifyTag('_putting_')).toBe('putting');
  });

  it('is idempotent on an already-valid slug', () => {
    expect(slugifyTag('full_swing')).toBe('full_swing');
  });

  it('keeps digits', () => {
    expect(slugifyTag('3 Wood work')).toBe('3_wood_work');
  });

  it('returns null for input with no alphanumeric content', () => {
    expect(slugifyTag('   ')).toBeNull();
    expect(slugifyTag('---')).toBeNull();
    expect(slugifyTag('')).toBeNull();
  });
});
