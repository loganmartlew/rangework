-- Relax the practice instruction ball-count check so a deliberate zero-ball
-- instruction can persist. Zero ("deliberately no balls") becomes valid while
-- negatives stay rejected as a DB backstop, and null ("Uncounted") is unchanged.
-- See ADR 0003 and issue #24 / #28.

alter table public.practice_unit_instructions
  drop constraint if exists practice_unit_instructions_ball_count_check;

alter table public.practice_unit_instructions
  add constraint practice_unit_instructions_ball_count_check
    check (ball_count is null or ball_count >= 0);
