-- Tags: a single shared vocabulary attached to Practice Units and Practice Sessions.
--
-- One owner-nullable `tags` table holds both the app-defined Default Tags
-- (owner_id IS NULL, curated stable codes) and per-user Custom Tags
-- (owner_id = the user). Two join tables attach tags to units and sessions by
-- the tag's UUID identity, so a Custom Tag can be renamed without breaking
-- attachments. See apps/mobile/CONTEXT.md (Tag, Default Tag, Custom Tag, Tag Code)
-- and apps/mobile/docs/adr/0001-tags-are-planning-only.md.

-- ============================================================
-- 1. tags table
-- ============================================================

create table if not exists public.tags (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid references public.profiles (id) on delete cascade,
  code text not null,
  display_name text not null,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now()),
  constraint tags_code_check
    check (char_length(trim(code)) > 0),
  constraint tags_display_name_check
    check (char_length(trim(display_name)) > 0)
);

-- A Default Tag's code is globally unique; a Custom Tag's code is unique per owner.
create unique index if not exists tags_default_code_unique
  on public.tags (code)
  where owner_id is null;

create unique index if not exists tags_custom_owner_code_unique
  on public.tags (owner_id, code)
  where owner_id is not null;

create index if not exists tags_owner_id_idx
  on public.tags (owner_id);

drop trigger if exists set_tags_updated_at on public.tags;
create trigger set_tags_updated_at
before update on public.tags
for each row
execute function public.set_updated_at();

alter table public.tags enable row level security;

-- A user may read Default Tags (shared catalog) and their own Custom Tags,
-- but may only write their own.
drop policy if exists "Users can view default and own tags" on public.tags;
create policy "Users can view default and own tags"
on public.tags
for select
using (owner_id is null or owner_id = auth.uid());

drop policy if exists "Users can insert own tags" on public.tags;
create policy "Users can insert own tags"
on public.tags
for insert
with check (owner_id = auth.uid());

drop policy if exists "Users can update own tags" on public.tags;
create policy "Users can update own tags"
on public.tags
for update
using (owner_id = auth.uid())
with check (owner_id = auth.uid());

drop policy if exists "Users can delete own tags" on public.tags;
create policy "Users can delete own tags"
on public.tags
for delete
using (owner_id = auth.uid());

grant select, insert, update, delete on table public.tags to authenticated;

-- ============================================================
-- 2. Join tables
-- ============================================================

create table if not exists public.practice_unit_tags (
  practice_unit_id uuid not null references public.practice_units (id) on delete cascade,
  tag_id uuid not null references public.tags (id) on delete cascade,
  created_at timestamptz not null default timezone('utc', now()),
  primary key (practice_unit_id, tag_id)
);

create index if not exists practice_unit_tags_tag_id_idx
  on public.practice_unit_tags (tag_id);

create table if not exists public.practice_session_tags (
  practice_session_id uuid not null references public.practice_sessions (id) on delete cascade,
  tag_id uuid not null references public.tags (id) on delete cascade,
  created_at timestamptz not null default timezone('utc', now()),
  primary key (practice_session_id, tag_id)
);

create index if not exists practice_session_tags_tag_id_idx
  on public.practice_session_tags (tag_id);

alter table public.practice_unit_tags enable row level security;
alter table public.practice_session_tags enable row level security;

-- A user may attach/read/detach tags only on units they own, and may only
-- attach tags visible to them (Default or their own Custom).
drop policy if exists "Users manage tags on own units" on public.practice_unit_tags;
create policy "Users manage tags on own units"
on public.practice_unit_tags
for all
using (
  exists (
    select 1 from public.practice_units pu
    where pu.id = practice_unit_id and pu.owner_id = auth.uid()
  )
)
with check (
  exists (
    select 1 from public.practice_units pu
    where pu.id = practice_unit_id and pu.owner_id = auth.uid()
  )
  and exists (
    select 1 from public.tags t
    where t.id = tag_id and (t.owner_id is null or t.owner_id = auth.uid())
  )
);

drop policy if exists "Users manage tags on own sessions" on public.practice_session_tags;
create policy "Users manage tags on own sessions"
on public.practice_session_tags
for all
using (
  exists (
    select 1 from public.practice_sessions ps
    where ps.id = practice_session_id and ps.owner_id = auth.uid()
  )
)
with check (
  exists (
    select 1 from public.practice_sessions ps
    where ps.id = practice_session_id and ps.owner_id = auth.uid()
  )
  and exists (
    select 1 from public.tags t
    where t.id = tag_id and (t.owner_id is null or t.owner_id = auth.uid())
  )
);

grant select, insert, delete on table public.practice_unit_tags to authenticated;
grant select, insert, delete on table public.practice_session_tags to authenticated;

-- ============================================================
-- 3. Slug generation (the canonical Tag Code rule)
--
-- Lowercase, then collapse every run of non-alphanumeric characters into a
-- single underscore, then strip leading/trailing underscores. Returns NULL for
-- input that has no alphanumeric content. This is mirrored — and unit-tested —
-- as a pure function in shared Kotlin and MCP TypeScript; keep all three in sync.
-- ============================================================

create or replace function public.slugify_tag(raw text)
returns text
language sql
immutable
as $$
  select nullif(
    trim(both '_' from regexp_replace(lower(trim(coalesce(raw, ''))), '[^a-z0-9]+', '_', 'g')),
    ''
  );
$$;

-- ============================================================
-- 4. Resolution rule: slug → Default Tag → own Custom Tag → insert new Custom Tag
--
-- The single source of truth for tag dedup/collision, called by both the Kotlin
-- repository and the MCP `create_tag` tool so the rule is never reimplemented.
-- p_code is slugified again here (idempotent) so the dedup key is authoritative.
-- ============================================================

create or replace function public.create_or_get_tag(p_code text, p_name text)
returns uuid
language plpgsql
security invoker
set search_path = ''
as $$
declare
  v_code text;
  v_name text;
  v_tag_id uuid;
begin
  v_code := public.slugify_tag(coalesce(nullif(trim(coalesce(p_code, '')), ''), p_name));
  v_name := trim(coalesce(p_name, ''));

  if v_code is null then
    raise exception 'Tag name must contain at least one alphanumeric character';
  end if;
  if v_name = '' then
    v_name := v_code;
  end if;

  -- 1. Default Tag with this code (shared vocabulary wins).
  select id into v_tag_id
  from public.tags
  where owner_id is null and code = v_code;
  if found then
    return v_tag_id;
  end if;

  -- 2. The caller's own Custom Tag with this code.
  select id into v_tag_id
  from public.tags
  where owner_id = auth.uid() and code = v_code;
  if found then
    return v_tag_id;
  end if;

  -- 3. Mint a new Custom Tag owned by the caller.
  insert into public.tags (owner_id, code, display_name)
  values (auth.uid(), v_code, v_name)
  returning id into v_tag_id;

  return v_tag_id;
end;
$$;

grant execute on function public.create_or_get_tag(text, text) to authenticated;

-- ============================================================
-- 5. Attachment counts (for the delete-confirmation prompt)
-- ============================================================

create or replace function public.count_tag_attachments(p_tag_id uuid)
returns jsonb
language sql
security invoker
set search_path = ''
as $$
  select jsonb_build_object(
    'unit_count', (select count(*) from public.practice_unit_tags where tag_id = p_tag_id),
    'session_count', (select count(*) from public.practice_session_tags where tag_id = p_tag_id)
  );
$$;

grant execute on function public.count_tag_attachments(uuid) to authenticated;

-- ============================================================
-- 6. Seed the 14 Default Tags (idempotent)
-- ============================================================

insert into public.tags (owner_id, code, display_name)
select null, v.code, v.display_name
from (values
  ('putting',          'Putting'),
  ('chipping',         'Chipping'),
  ('pitching',         'Pitching'),
  ('bunker',           'Bunker'),
  ('short_game',       'Short Game'),
  ('wedges',           'Wedges'),
  ('approach',         'Approach'),
  ('driving',          'Driving'),
  ('full_swing',       'Full Swing'),
  ('distance_control', 'Distance Control'),
  ('accuracy',         'Accuracy'),
  ('shot_shaping',     'Shot Shaping'),
  ('tempo',            'Tempo'),
  ('mental',           'Mental')
) as v(code, display_name)
where not exists (
  select 1 from public.tags t
  where t.owner_id is null and t.code = v.code
);

-- ============================================================
-- 7. Add tag-id array parameters to the atomic save RPCs
--
-- The signature changes (a new argument), so the old functions are dropped
-- before recreating. Tag attachments are replaced wholesale alongside the
-- instructions/items, mirroring the existing delete-then-insert pattern. RLS on
-- the join tables rejects tag ids the caller cannot see and units/sessions they
-- do not own.
-- ============================================================

drop function if exists public.save_practice_unit(uuid, text, text, text, text, jsonb);

create or replace function public.save_practice_unit(
  p_unit_id uuid,
  p_title text,
  p_notes text,
  p_focus text,
  p_default_club_code text,
  p_instructions jsonb,
  p_tag_ids uuid[] default '{}'::uuid[]
) returns void
language plpgsql
set search_path = ''
as $$
begin
  insert into public.practice_units (id, owner_id, title, notes, focus, default_club_code)
  values (p_unit_id, auth.uid(), p_title, p_notes, p_focus, p_default_club_code)
  on conflict (id) do update set
    title             = excluded.title,
    notes             = excluded.notes,
    focus             = excluded.focus,
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

  delete from public.practice_unit_tags
  where practice_unit_id = p_unit_id;

  insert into public.practice_unit_tags (practice_unit_id, tag_id)
  select distinct p_unit_id, tid
  from unnest(coalesce(p_tag_ids, '{}'::uuid[])) as tid;
end;
$$;

grant execute on function public.save_practice_unit(uuid, text, text, text, text, jsonb, uuid[])
  to authenticated;


drop function if exists public.save_practice_session(uuid, text, text, jsonb);

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

  delete from public.practice_session_tags
  where practice_session_id = p_session_id;

  insert into public.practice_session_tags (practice_session_id, tag_id)
  select distinct p_session_id, tid
  from unnest(coalesce(p_tag_ids, '{}'::uuid[])) as tid;
end;
$$;

grant execute on function public.save_practice_session(uuid, text, text, jsonb, uuid[])
  to authenticated;
