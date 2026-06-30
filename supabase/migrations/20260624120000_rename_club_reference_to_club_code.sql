-- Rename club_reference columns to club_code throughout the planning tables.
-- The term "Club Code" is the canonical domain name (see apps/mobile/CONTEXT.md).

ALTER TABLE public.practice_units
  RENAME COLUMN default_club_reference TO default_club_code;

ALTER TABLE public.practice_session_items
  RENAME COLUMN club_reference TO club_code;

-- Recreate save_practice_unit with renamed parameter and column references.
-- Drop first: Postgres cannot rename an input parameter via CREATE OR REPLACE
-- (p_default_club_reference -> p_default_club_code).
drop function if exists public.save_practice_unit(uuid, text, text, text, text, jsonb);

create or replace function public.save_practice_unit(
  p_unit_id uuid,
  p_title text,
  p_notes text,
  p_focus text,
  p_default_club_code text,
  p_instructions jsonb
) returns void
language plpgsql
set search_path = ''
as $$
begin
  insert into public.practice_units (id, owner_id, title, notes, focus, default_club_code)
  values (p_unit_id, auth.uid(), p_title, p_notes, p_focus, p_default_club_code)
  on conflict (id) do update set
    title            = excluded.title,
    notes            = excluded.notes,
    focus            = excluded.focus,
    default_club_code = excluded.default_club_code
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


-- Recreate save_practice_session with renamed column and JSONB key references.
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
     repeat_count, club_code, notes, focus_cue)
  select
    gen_random_uuid(),
    p_session_id,
    (item->>'practice_unit_id')::uuid,
    (item->>'order')::int,
    (item->>'repeat_count')::int,
    item->>'club_code',
    item->>'notes',
    item->>'focus_cue'
  from jsonb_array_elements(p_items) item;
end;
$$;

grant execute on function public.save_practice_session(uuid, text, text, jsonb)
  to authenticated;


-- Recreate start_range_session with renamed column references.
create or replace function public.start_range_session(
  p_range_session_id uuid,
  p_session_id uuid
) returns jsonb
language plpgsql
security invoker
set search_path = ''
as $$
declare
  v_session record;
  v_snapshot jsonb;
  v_units jsonb := '[]'::jsonb;
  v_steps jsonb := '[]'::jsonb;
  v_unit_index integer := 0;
  v_item record;
  v_unit record;
  v_instr record;
  v_club_code text;
  v_club_display_name text;
  v_instructions jsonb;
  v_instruction_index integer;
  v_rep integer;
  v_result jsonb;
begin
  -- Read practice session (RLS enforces ownership)
  select * into v_session
  from public.practice_sessions
  where id = p_session_id;

  if not found then
    raise exception 'Practice session not found or not accessible';
  end if;

  -- Iterate over session items in order
  for v_item in
    select psi.*
    from public.practice_session_items psi
    where psi.practice_session_id = p_session_id
    order by psi.sort_order
  loop
    -- Read the practice unit (RLS enforces ownership)
    select * into v_unit
    from public.practice_units pu
    where pu.id = v_item.practice_unit_id;

    if not found then
      continue;
    end if;

    -- Resolve club: item override takes priority, then unit default
    v_club_code := coalesce(v_item.club_code, v_unit.default_club_code);
    v_club_display_name := null;

    if v_club_code is not null then
      select c.display_name into v_club_display_name
      from public.clubs c
      where c.code = v_club_code;
    end if;

    -- Build the instructions array for the snapshot unit entry
    v_instructions := '[]'::jsonb;

    for v_instr in
      select pui.*
      from public.practice_unit_instructions pui
      where pui.practice_unit_id = v_unit.id
      order by pui.sort_order
    loop
      v_instructions := v_instructions || jsonb_build_object(
        'text', v_instr.text,
        'ballCount', v_instr.ball_count
      );
    end loop;

    -- Expand steps: rep outer, instruction inner.
    -- This produces: rep1-instr1, rep1-instr2, …, rep2-instr1, rep2-instr2, …
    for v_rep in 1..v_item.repeat_count loop
      v_instruction_index := 0;

      for v_instr in
        select pui.*
        from public.practice_unit_instructions pui
        where pui.practice_unit_id = v_unit.id
        order by pui.sort_order
      loop
        v_steps := v_steps || jsonb_build_object(
          'unitIndex', v_unit_index,
          'instructionIndex', v_instruction_index,
          'repNumber', v_rep,
          'totalReps', v_item.repeat_count,
          'instructionText', v_instr.text,
          'ballCount', v_instr.ball_count,
          'club', v_club_code,
          'clubDisplayName', v_club_display_name,
          'unitTitle', v_unit.title,
          'notes', v_item.notes,
          'focusCue', v_item.focus_cue
        );

        v_instruction_index := v_instruction_index + 1;
      end loop;
    end loop;

    -- Add unit to snapshot
    v_units := v_units || jsonb_build_object(
      'unitTitle', v_unit.title,
      'unitNotes', v_unit.notes,
      'unitFocus', v_unit.focus,
      'itemNotes', v_item.notes,
      'itemFocusCue', v_item.focus_cue,
      'club', v_club_code,
      'clubDisplayName', v_club_display_name,
      'repeatCount', v_item.repeat_count,
      'instructions', v_instructions
    );

    v_unit_index := v_unit_index + 1;
  end loop;

  -- Validate steps not empty
  if jsonb_array_length(v_steps) = 0 then
    raise exception 'Session has no instructions';
  end if;

  -- Assemble snapshot
  v_snapshot := jsonb_build_object(
    'sessionNotes', v_session.notes,
    'units', v_units,
    'steps', v_steps
  );

  -- Insert range_sessions row
  insert into public.range_sessions (
    id, owner_id, source_session_id, session_name, snapshot, snapshot_version
  ) values (
    p_range_session_id, auth.uid(), p_session_id, v_session.name, v_snapshot, 1
  );

  -- Return the full inserted row as JSONB
  select to_jsonb(rs.*) into v_result
  from public.range_sessions rs
  where rs.id = p_range_session_id;

  return v_result;
end;
$$;
