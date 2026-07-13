-- Practice Session archiving — schema foundation (Stage 1).
--
-- Lands the archived lifecycle state end to end in the data layer: the
-- archived_at column, a partial index for the default (unarchived) listing's
-- hot path, and a belt-and-braces guard on start_range_session. No UI
-- (Stage 2) and no MCP surface (Stage 3) ship here — archived_at is null on
-- every row, so every existing flow behaves bit-identically. See
-- design-docs/mcp-data-clutter/ (epic + stage-01-archiving-foundation plan,
-- D1/D2).

-- ============================================================
-- 1. practice_sessions.archived_at (nullable timestamptz)
--
-- Timestamp, not boolean — "when" comes free. Null = unarchived (every
-- existing row). No RLS change: archiving is a same-owner column write
-- already covered by the "manage their own practice sessions" policy.
-- ============================================================

alter table public.practice_sessions
  add column if not exists archived_at timestamptz;

-- ============================================================
-- 2. Partial index for the default (unarchived) list's hot path
--
-- Matches the existing (owner_id, updated_at desc) ordering, scoped to the
-- unarchived population that the default listing reads.
-- ============================================================

create index if not exists practice_sessions_active_owner_idx
  on public.practice_sessions (owner_id, updated_at desc)
  where archived_at is null;

-- ============================================================
-- 3. start_range_session guard (D2) — reject starting from an archived
--    session. Belt-and-braces behind Stage 2's UI gating; the RPC already
--    reads v_session via `select * into` so v_session.archived_at is in
--    hand. Pure additive guard clause right after the existing "not found"
--    check — the rest of the v3 body (migration 20260709120000) is copied
--    verbatim, no snapshot-shape change, no version bump.
-- ============================================================

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
  v_expanded_count integer;
  v_step_ball_count integer;
  v_ball integer;
  v_success_criterion text;
  v_observation_types text[];
  v_result jsonb;
begin
  -- Read practice session (RLS enforces ownership)
  select * into v_session
  from public.practice_sessions
  where id = p_session_id;

  if not found then
    raise exception 'Practice session not found or not accessible';
  end if;

  if v_session.archived_at is not null then
    raise exception 'Cannot start a range session from an archived practice session';
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

    -- Success criterion in force at start; observation types with `success`
    -- filtered out when the criterion has since been removed from the unit.
    v_success_criterion := v_unit.success_criterion;
    v_observation_types := coalesce(v_item.observation_types, '{}'::text[]);
    if v_success_criterion is null then
      v_observation_types := array_remove(v_observation_types, 'success');
    end if;

    -- Build the instructions array for the snapshot unit entry, resolving each
    -- instruction's club via Club Resolution precedence.
    v_instructions := '[]'::jsonb;

    for v_instr in
      select pui.*
      from public.practice_unit_instructions pui
      where pui.practice_unit_id = v_unit.id
      order by pui.sort_order
    loop
      v_club_code := coalesce(v_item.club_code, v_instr.club_code, v_unit.default_club_code);
      v_club_display_name := null;

      if v_club_code is not null then
        select c.display_name into v_club_display_name
        from public.clubs c
        where c.code = v_club_code;
      end if;

      v_instructions := v_instructions || jsonb_build_object(
        'text', v_instr.text,
        'ballCount', v_instr.ball_count,
        'club', v_club_code,
        'clubDisplayName', v_club_display_name
      );
    end loop;

    -- Expand steps: rep outer, instruction inner, ball innermost.
    -- This produces: rep1-instr1-ball1, rep1-instr1-ball2, …, rep1-instr2-…,
    -- rep2-instr1-…, matching how a pass is executed on the range.
    for v_rep in 1..v_item.repeat_count loop
      v_instruction_index := 0;

      for v_instr in
        select pui.*
        from public.practice_unit_instructions pui
        where pui.practice_unit_id = v_unit.id
        order by pui.sort_order
      loop
        v_club_code := coalesce(v_item.club_code, v_instr.club_code, v_unit.default_club_code);
        v_club_display_name := null;

        if v_club_code is not null then
          select c.display_name into v_club_display_name
          from public.clubs c
          where c.code = v_club_code;
        end if;

        -- ADR 0004 expansion rule: N > 0 → N Ball Steps of one ball each;
        -- 0 and null (Uncounted) → a single Action Step keeping its value.
        if v_instr.ball_count is not null and v_instr.ball_count > 0 then
          v_expanded_count := v_instr.ball_count;
          v_step_ball_count := 1;
        else
          v_expanded_count := 1;
          v_step_ball_count := v_instr.ball_count;
        end if;

        for v_ball in 1..v_expanded_count loop
          v_steps := v_steps || jsonb_build_object(
            'unitIndex', v_unit_index,
            'instructionIndex', v_instruction_index,
            'repNumber', v_rep,
            'totalReps', v_item.repeat_count,
            'instructionText', v_instr.text,
            'ballCount', v_step_ball_count,
            'club', v_club_code,
            'clubDisplayName', v_club_display_name,
            'unitTitle', v_unit.title,
            'notes', v_item.notes,
            'focusCue', v_item.focus_cue
          );
        end loop;

        v_instruction_index := v_instruction_index + 1;
      end loop;
    end loop;

    -- Add unit to snapshot. The unit object carries no club of its own
    -- (per-instruction clubs live on each entry in the instructions array).
    -- v3 adds successCriterion + observationTypes.
    v_units := v_units || jsonb_build_object(
      'unitTitle', v_unit.title,
      'unitNotes', v_unit.notes,
      'unitFocus', v_unit.focus,
      'itemNotes', v_item.notes,
      'itemFocusCue', v_item.focus_cue,
      'repeatCount', v_item.repeat_count,
      'instructions', v_instructions,
      'successCriterion', v_success_criterion,
      'observationTypes', to_jsonb(v_observation_types)
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

  -- Insert range_sessions row (snapshot_version 3 = data-recording shape)
  insert into public.range_sessions (
    id, owner_id, source_session_id, session_name, snapshot, snapshot_version
  ) values (
    p_range_session_id, auth.uid(), p_session_id, v_session.name, v_snapshot, 3
  );

  -- Return the full inserted row as JSONB
  select to_jsonb(rs.*) into v_result
  from public.range_sessions rs
  where rs.id = p_range_session_id;

  return v_result;
end;
$$;
