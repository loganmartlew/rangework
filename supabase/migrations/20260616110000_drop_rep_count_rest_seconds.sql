alter table public.practice_unit_instructions
  drop column if exists rep_count;

alter table public.practice_session_items
  drop column if exists rest_seconds;
