-- Inline Units — ownership foundation (Stage 4).
--
-- Lands inline-unit ownership end to end in the data layer: the
-- scoped_to_session_id owning reference on practice_units (with its cascade
-- FK), save_practice_session v4 (mints inline units from embedded definitions
-- atomically + reaps orphans), a server-side duplicate_practice_session copy
-- RPC (deep-copies inline units on duplication), and a before-delete ordering
-- trigger so the inline-unit cascade can't trip the protective item→unit
-- restrict FK. No app UI (Stage 5) and no MCP surface (Stage 6) ship here:
-- scoped_to_session_id is null on every existing row and no client mints an
-- inline unit yet, so every current flow behaves bit-identically. See
-- design-docs/mcp-data-clutter/ (epic + stage-04-inline-units-foundation plan,
-- D1–D5) and docs/adr/0007-inline-unit-ownership-and-cascade.md.

-- ============================================================
-- 1. practice_units.scoped_to_session_id (nullable uuid, cascade FK)
--
-- Null = library citizen (every existing row); non-null = an Inline Unit
-- owned by that session. `on delete cascade` ties the inline unit's life to
-- its owning session (design §6/§9). No RLS change: scoping is an owner-scoped
-- column already covered by the "manage their own practice units" policy.
-- ============================================================

alter table public.practice_units
  add column if not exists scoped_to_session_id uuid
    references public.practice_sessions (id) on delete cascade;

-- ============================================================
-- 2. Partial index for the deep-copy scan and orphan GC
--
-- Only inline units carry a non-null scope, so a partial index keeps the
-- library's hot path (all-null) untouched while serving the by-owner scans
-- duplication and GC perform.
-- ============================================================

create index if not exists practice_units_scoped_session_idx
  on public.practice_units (scoped_to_session_id)
  where scoped_to_session_id is not null;

-- ============================================================
-- 3. save_practice_session v4 — inline mint + orphan GC (D1)
--
-- Signature unchanged (inline rides inside each p_items element as an optional
-- `inline_unit` object), so CREATE OR REPLACE. Each item carries either
-- `practice_unit_id` (reference an existing unit — every existing call) or an
-- `inline_unit` object (mint a new unit scoped to this session). A plain
-- reference-only payload produces byte-identical rows to v3.
-- ============================================================

create or replace function public.save_practice_session(
  p_session_id uuid,
  p_name text,
  p_notes text,
  p_items jsonb,
  p_tag_ids uuid[] default '{}'::uuid[]
) returns void
language plpgsql
set search_path = ''
as $$
declare
  v_item jsonb;
  v_inline jsonb;
  v_unit_id uuid;
  v_resolved_unit_ids uuid[] := '{}'::uuid[];
begin
  -- 1. Upsert the session row (archived_at preserved per Stage 1 — untouched).
  insert into public.practice_sessions (id, owner_id, name, notes)
  values (p_session_id, auth.uid(), p_name, p_notes)
  on conflict (id) do update set
    name  = excluded.name,
    notes = excluded.notes
  where public.practice_sessions.owner_id = auth.uid();

  -- 2. Clear existing items.
  delete from public.practice_session_items
  where practice_session_id = p_session_id;

  -- 3. Mint inline units in item order, building the resolved unit-id array.
  --    An item with an `inline_unit` object mints a new owner-scoped unit; an
  --    item without one references its existing unit by `practice_unit_id`.
  for v_item in select * from jsonb_array_elements(p_items)
  loop
    v_inline := v_item->'inline_unit';
    if v_inline is not null and jsonb_typeof(v_inline) = 'object' then
      v_unit_id := gen_random_uuid();

      insert into public.practice_units
        (id, owner_id, title, notes, focus, default_club_code,
         success_criterion, scoped_to_session_id)
      values
        (v_unit_id, auth.uid(),
         v_inline->>'title',
         v_inline->>'notes',
         v_inline->>'focus',
         v_inline->>'default_club_code',
         nullif(trim(v_inline->>'success_criterion'), ''),
         p_session_id);

      insert into public.practice_unit_instructions
        (id, practice_unit_id, sort_order, text, ball_count, club_code)
      select
        gen_random_uuid(),
        v_unit_id,
        (inst->>'order')::int,
        inst->>'text',
        case when inst->>'ball_count' is null then null
             else (inst->>'ball_count')::int end,
        inst->>'club_code'
      from jsonb_array_elements(coalesce(v_inline->'instructions', '[]'::jsonb)) inst;

      insert into public.practice_unit_tags (practice_unit_id, tag_id)
      select distinct v_unit_id, tid::uuid
      from jsonb_array_elements_text(coalesce(v_inline->'tag_ids', '[]'::jsonb)) tid;
    else
      v_unit_id := (v_item->>'practice_unit_id')::uuid;
    end if;

    v_resolved_unit_ids := v_resolved_unit_ids || v_unit_id;
  end loop;

  -- 4. Insert items from the resolved array (the rest of the projection —
  --    order, repeat, overrides, observation_types — unchanged from v3).
  insert into public.practice_session_items
    (id, practice_session_id, practice_unit_id, sort_order,
     repeat_count, club_code, notes, focus_cue, observation_types)
  select
    gen_random_uuid(),
    p_session_id,
    v_resolved_unit_ids[ordinality],
    (item->>'order')::int,
    (item->>'repeat_count')::int,
    item->>'club_code',
    item->>'notes',
    item->>'focus_cue',
    coalesce(
      (select array_agg(distinct value)
       from jsonb_array_elements_text(item->'observation_types') as value),
      '{}'::text[]
    )
  from jsonb_array_elements(p_items) with ordinality as t(item, ordinality);

  -- 5. Success-requires-criterion (atomic boundary) — now also covers minted
  --    inline units, since they exist by this point.
  if exists (
    select 1
    from public.practice_session_items psi
    join public.practice_units pu on pu.id = psi.practice_unit_id
    where psi.practice_session_id = p_session_id
      and 'success' = any(psi.observation_types)
      and pu.success_criterion is null
  ) then
    raise exception
      'Cannot enable the success observation on a unit without a success criterion';
  end if;

  -- 6. Orphan GC: reap inline units this session owns that the save dropped or
  --    replaced. Promoted units are safe (their scope is already null, so they
  --    fall outside this predicate); still-referenced inline units are in the
  --    resolved array; only genuinely orphaned scoped rows are removed.
  delete from public.practice_units
  where scoped_to_session_id = p_session_id
    and id <> all(v_resolved_unit_ids);

  -- 7. Tags (unchanged).
  delete from public.practice_session_tags
  where practice_session_id = p_session_id;

  insert into public.practice_session_tags (practice_session_id, tag_id)
  select distinct p_session_id, tid
  from unnest(coalesce(p_tag_ids, '{}'::uuid[])) as tid;
end;
$$;

grant execute on function public.save_practice_session(uuid, text, text, jsonb, uuid[])
  to authenticated;

-- ============================================================
-- 4. duplicate_practice_session — server-side deep copy (D2)
--
-- Deep-copies a session's inline units atomically with the duplicate: a new
-- session row (archived_at left null — a duplicate is always unarchived), a
-- fresh copy of each owned inline unit repointed onto the new session, items
-- copied verbatim with inline references remapped through the old→new unit id
-- mapping (library references untouched), and session tags copied. RLS
-- enforces the caller owns the source (the source selects return nothing
-- otherwise) and the copy defaults owner_id to the caller.
-- ============================================================

create or replace function public.duplicate_practice_session(
  p_source_id uuid,
  p_new_id uuid
) returns void
language plpgsql
security invoker
set search_path = ''
as $$
declare
  v_source record;
  v_unit record;
  v_new_unit_id uuid;
  v_mapping jsonb := '{}'::jsonb;
begin
  select * into v_source
  from public.practice_sessions
  where id = p_source_id;

  if not found then
    raise exception 'Practice session not found or not accessible';
  end if;

  -- New session row (archived_at intentionally left null).
  insert into public.practice_sessions (id, owner_id, name, notes)
  values (p_new_id, auth.uid(), v_source.name, v_source.notes);

  -- Deep-copy the source's inline units; record old→new unit id mapping.
  for v_unit in
    select *
    from public.practice_units
    where scoped_to_session_id = p_source_id
  loop
    v_new_unit_id := gen_random_uuid();
    v_mapping := v_mapping || jsonb_build_object(v_unit.id::text, v_new_unit_id::text);

    insert into public.practice_units
      (id, owner_id, title, notes, focus, default_club_code,
       success_criterion, scoped_to_session_id)
    values
      (v_new_unit_id, auth.uid(), v_unit.title, v_unit.notes, v_unit.focus,
       v_unit.default_club_code, v_unit.success_criterion, p_new_id);

    insert into public.practice_unit_instructions
      (id, practice_unit_id, sort_order, text, ball_count, club_code)
    select gen_random_uuid(), v_new_unit_id, sort_order, text, ball_count, club_code
    from public.practice_unit_instructions
    where practice_unit_id = v_unit.id;

    insert into public.practice_unit_tags (practice_unit_id, tag_id)
    select v_new_unit_id, tag_id
    from public.practice_unit_tags
    where practice_unit_id = v_unit.id;
  end loop;

  -- Copy items verbatim, repointing inline references through the mapping;
  -- library references (not in the mapping) are left untouched.
  insert into public.practice_session_items
    (id, practice_session_id, practice_unit_id, sort_order,
     repeat_count, club_code, notes, focus_cue, observation_types)
  select
    gen_random_uuid(),
    p_new_id,
    coalesce((v_mapping->>(practice_unit_id::text))::uuid, practice_unit_id),
    sort_order,
    repeat_count,
    club_code,
    notes,
    focus_cue,
    observation_types
  from public.practice_session_items
  where practice_session_id = p_source_id;

  -- Copy session tags.
  insert into public.practice_session_tags (practice_session_id, tag_id)
  select p_new_id, tag_id
  from public.practice_session_tags
  where practice_session_id = p_source_id;
end;
$$;

grant execute on function public.duplicate_practice_session(uuid, uuid)
  to authenticated;

-- ============================================================
-- 5. Delete-ordering trigger (D3)
--
-- practice_session_items.practice_unit_id is `on delete restrict` (it protects
-- library units from being deleted out from under a referencing session).
-- Deleting a session fires two cascades: its items (cascade) and its inline
-- units (the new scope cascade). If the unit cascade runs before the item
-- cascade, the still-present item's restrict FK blocks the delete. Removing the
-- items first, in a before-delete trigger, makes the two cascades order-safe by
-- construction — the inline-unit cascade then references nothing. We ship this
-- rather than rely on Postgres constraint evaluation order. Do NOT relax the
-- protective item→unit FK: it deliberately guards shared library units.
-- ============================================================

create or replace function public.delete_practice_session_items_before_delete()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  delete from public.practice_session_items
  where practice_session_id = old.id;
  return old;
end;
$$;

drop trigger if exists before_delete_practice_session_items on public.practice_sessions;
create trigger before_delete_practice_session_items
before delete on public.practice_sessions
for each row
execute function public.delete_practice_session_items_before_delete();
