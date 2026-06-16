create or replace function public.save_practice_unit(
  p_unit_id uuid,
  p_title text,
  p_notes text,
  p_focus text,
  p_default_club_reference text,
  p_instructions jsonb
) returns void
language plpgsql
set search_path = ''
as $$
begin
  insert into public.practice_units (id, owner_id, title, notes, focus, default_club_reference)
  values (p_unit_id, auth.uid(), p_title, p_notes, p_focus, p_default_club_reference)
  on conflict (id) do update set
    title                 = excluded.title,
    notes                 = excluded.notes,
    focus                 = excluded.focus,
    default_club_reference = excluded.default_club_reference
  where public.practice_units.owner_id = auth.uid();

  delete from public.practice_unit_instructions
  where practice_unit_id = p_unit_id;

  insert into public.practice_unit_instructions
    (id, practice_unit_id, sort_order, text, ball_count)
  select
    gen_random_uuid(),
    p_unit_id,
    (inst->>'order')::int,
    inst->>'text',
    case when inst->>'ball_count' is null then null
         else (inst->>'ball_count')::int end
  from jsonb_array_elements(p_instructions) inst;
end;
$$;

grant execute on function public.save_practice_unit(uuid, text, text, text, text, jsonb)
  to authenticated;


create or replace function public.save_practice_session(
  p_session_id uuid,
  p_name text,
  p_notes text,
  p_items jsonb
) returns void
language plpgsql
set search_path = ''
as $$
begin
  insert into public.practice_sessions (id, owner_id, name, notes)
  values (p_session_id, auth.uid(), p_name, p_notes)
  on conflict (id) do update set
    name  = excluded.name,
    notes = excluded.notes
  where public.practice_sessions.owner_id = auth.uid();

  delete from public.practice_session_items
  where practice_session_id = p_session_id;

  insert into public.practice_session_items
    (id, practice_session_id, practice_unit_id, sort_order,
     repeat_count, club_reference, notes, focus_cue)
  select
    gen_random_uuid(),
    p_session_id,
    (item->>'practice_unit_id')::uuid,
    (item->>'order')::int,
    (item->>'repeat_count')::int,
    item->>'club_reference',
    item->>'notes',
    item->>'focus_cue'
  from jsonb_array_elements(p_items) item;
end;
$$;

grant execute on function public.save_practice_session(uuid, text, text, jsonb)
  to authenticated;
