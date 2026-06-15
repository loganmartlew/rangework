alter table public.practice_units
  drop column if exists tags,
  drop column if exists default_ball_count;

alter table public.practice_unit_instructions
  drop column if exists club_reference;

alter table public.practice_session_items
  add column if not exists repeat_count integer,
  add column if not exists club_reference text;

update public.practice_session_items
set repeat_count = 1
where repeat_count is null;

alter table public.practice_session_items
  alter column repeat_count set not null;

alter table public.practice_session_items
  drop column if exists override_ball_count;

alter table public.practice_session_items
  drop constraint if exists practice_session_items_override_ball_count_check;

alter table public.practice_session_items
  drop constraint if exists practice_session_items_repeat_count_check;

alter table public.practice_session_items
  add constraint practice_session_items_repeat_count_check
    check (repeat_count > 0);
