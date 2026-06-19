export const themeTransitionClass =
  'transition-[background-color,border-color,color,box-shadow,filter] duration-300 ease-[var(--ease-standard)]';

export const focusRingClass =
  'focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-background';

export const revealClass =
  'motion-reduce:opacity-100 motion-reduce:translate-y-0 opacity-0 translate-y-5 transition-[opacity,transform] duration-[640ms] ease-[var(--ease-standard)] [transition-delay:var(--stagger,0ms)]';

export const eyebrowClass =
  'font-mono text-[0.72rem] uppercase tracking-[0.18em] text-[var(--label-muted)]';

export const pillButtonClass = `${focusRingClass} inline-flex items-center justify-center gap-2 rounded-full bg-primary text-onprimary font-medium tracking-[-0.01em] transition-[transform,filter,background-color,box-shadow] duration-200 ease-[var(--ease-standard)] hover:brightness-105 active:translate-y-px`;

export const textButtonClass = `${focusRingClass} inline-flex items-center gap-[0.45rem] rounded-full px-[0.95rem] py-[0.8rem] text-[0.92rem] font-medium text-primary transition-[background-color,color,box-shadow] duration-200 ease-[var(--ease-standard)] hover:bg-[var(--tag-bg)]`;

export const cardClass = `${themeTransitionClass} rounded-[0.95rem] border border-[var(--card-border)] bg-surface`;

export const liftedCardClass = `${cardClass} transition-[background-color,border-color,color,box-shadow,transform] duration-[220ms] ease-[var(--ease-standard)] hover:-translate-y-0.5`;

export const chipClass =
  'inline-flex items-center justify-center rounded-full bg-[var(--tag-bg)] px-[0.8rem] py-[0.45rem] font-mono text-[0.72rem] uppercase tracking-[0.12em] text-primary';

export const sectionClass = `${themeTransitionClass} border-t border-outlinevariant/80 px-5 py-18 md:px-8 md:py-22 lg:px-14 xl:px-[4.5rem]`;
