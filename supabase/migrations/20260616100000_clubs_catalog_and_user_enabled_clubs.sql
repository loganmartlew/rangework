-- Club catalog: seeded reference table + per-user enabled set.
-- Clubs replace free-text club_reference fields with stable codes that FK here.

-- ============================================================
-- 1. Club catalog (public reference data; no client writes)
-- ============================================================

create table if not exists public.clubs (
  code text primary key,
  display_name text not null,
  category text not null,
  sort_order integer not null,
  default_enabled boolean not null default false,
  constraint clubs_category_check
    check (category in ('WOOD', 'HYBRID', 'IRON', 'WEDGE', 'PUTTER'))
);

-- Seed the full club catalog (idempotent)
insert into public.clubs (code, display_name, category, sort_order, default_enabled) values
  ('driver',         'Driver',                  'WOOD',   1,  true),
  ('two_wood',       '2-Wood',                  'WOOD',   2,  false),
  ('three_wood',     '3-Wood',                  'WOOD',   3,  true),
  ('four_wood',      '4-Wood',                  'WOOD',   4,  false),
  ('five_wood',      '5-Wood',                  'WOOD',   5,  true),
  ('seven_wood',     '7-Wood',                  'WOOD',   6,  false),
  ('nine_wood',      '9-Wood',                  'WOOD',   7,  false),
  ('eleven_wood',    '11-Wood',                 'WOOD',   8,  false),
  ('two_hybrid',     '2-Hybrid',                'HYBRID', 9,  false),
  ('three_hybrid',   '3-Hybrid',                'HYBRID', 10, false),
  ('four_hybrid',    '4-Hybrid',                'HYBRID', 11, false),
  ('five_hybrid',    '5-Hybrid',                'HYBRID', 12, false),
  ('six_hybrid',     '6-Hybrid',                'HYBRID', 13, false),
  ('seven_hybrid',   '7-Hybrid',                'HYBRID', 14, false),
  ('one_iron',       '1-Iron',                  'IRON',   15, false),
  ('two_iron',       '2-Iron',                  'IRON',   16, false),
  ('three_iron',     '3-Iron',                  'IRON',   17, false),
  ('four_iron',      '4-Iron',                  'IRON',   18, true),
  ('five_iron',      '5-Iron',                  'IRON',   19, true),
  ('six_iron',       '6-Iron',                  'IRON',   20, true),
  ('seven_iron',     '7-Iron',                  'IRON',   21, true),
  ('eight_iron',     '8-Iron',                  'IRON',   22, true),
  ('nine_iron',      '9-Iron',                  'IRON',   23, true),
  ('pitching_wedge', 'Pitching Wedge (PW)',      'WEDGE',  24, true),
  ('approach_wedge', 'Approach Wedge (AW)',      'WEDGE',  25, false),
  ('gap_wedge',      'Gap Wedge (GW)',           'WEDGE',  26, true),
  ('sand_wedge',     'Sand Wedge (SW)',          'WEDGE',  27, true),
  ('lob_wedge',      'Lob Wedge (LW)',           'WEDGE',  28, true),
  ('ultra_lob_wedge','Ultra Lob Wedge (ULW)',    'WEDGE',  29, false),
  ('putter',         'Putter',                   'PUTTER', 30, true)
on conflict (code) do update
  set display_name    = excluded.display_name,
      category        = excluded.category,
      sort_order      = excluded.sort_order,
      default_enabled = excluded.default_enabled;

alter table public.clubs enable row level security;

drop policy if exists "Anyone can view clubs" on public.clubs;
create policy "Anyone can view clubs"
on public.clubs
for select
using (true);

grant select on table public.clubs to authenticated;

-- ============================================================
-- 2. Per-user enabled clubs
-- ============================================================

create table if not exists public.user_enabled_clubs (
  user_id uuid not null default auth.uid() references public.profiles (id) on delete cascade,
  club_code text not null references public.clubs (code) on delete cascade,
  created_at timestamptz not null default timezone('utc', now()),
  primary key (user_id, club_code)
);

alter table public.user_enabled_clubs enable row level security;

drop policy if exists "Users can manage their own enabled clubs" on public.user_enabled_clubs;
create policy "Users can manage their own enabled clubs"
on public.user_enabled_clubs
for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

grant select, insert, delete on table public.user_enabled_clubs to authenticated;

-- ============================================================
-- 3. Seed default clubs when a new profile is created
-- ============================================================

create or replace function public.seed_default_clubs_for_profile()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.user_enabled_clubs (user_id, club_code)
  select new.id, code
  from public.clubs
  where default_enabled = true
  on conflict do nothing;
  return new;
end;
$$;

drop trigger if exists seed_default_clubs_on_profile_insert on public.profiles;
create trigger seed_default_clubs_on_profile_insert
after insert on public.profiles
for each row
execute function public.seed_default_clubs_for_profile();

-- ============================================================
-- 4. Backfill existing users with default clubs
-- ============================================================

insert into public.user_enabled_clubs (user_id, club_code)
select p.id, c.code
from public.profiles p
cross join public.clubs c
where c.default_enabled = true
on conflict do nothing;

-- ============================================================
-- 5. Migrate existing free-text club values to catalog codes
--
-- Normalization strategy: lower-case, strip non-alphanumeric, then CASE match.
-- Common patterns seen in practice data (e.g. "58*", "7i", "sw", "driver"):
--   driver / d / 1w             → driver
--   3w / 3wood                  → three_wood
--   5w / 5wood                  → five_wood
--   7w / 7wood                  → seven_wood
--   #h / #hybrid (2-7)          → #_hybrid
--   #i / #iron / # (1-9)        → #_iron
--   pw / pitching               → pitching_wedge
--   aw / approach               → approach_wedge
--   gw / gap                    → gap_wedge
--   sw / sand / 54/55/56        → sand_wedge
--   lw / lob / 58/59/60/61/62   → lob_wedge
--   ulw / ultralob / 64/65      → ultra_lob_wedge
--   p / putt / putter           → putter
--   anything else               → null
-- ============================================================

create or replace function public.normalize_club_code(raw text)
returns text
language plpgsql
immutable
as $$
declare
  n text;
begin
  if raw is null then return null; end if;
  n := regexp_replace(lower(trim(raw)), '[^a-z0-9]', '', 'g');
  return case
    when n in ('driver','d','1w') then 'driver'
    when n in ('2w','2wood') then 'two_wood'
    when n in ('3w','3wood') then 'three_wood'
    when n in ('4w','4wood') then 'four_wood'
    when n in ('5w','5wood') then 'five_wood'
    when n in ('7w','7wood') then 'seven_wood'
    when n in ('9w','9wood') then 'nine_wood'
    when n in ('11w','11wood') then 'eleven_wood'
    when n in ('2h','2hybrid') then 'two_hybrid'
    when n in ('3h','3hybrid') then 'three_hybrid'
    when n in ('4h','4hybrid') then 'four_hybrid'
    when n in ('5h','5hybrid') then 'five_hybrid'
    when n in ('6h','6hybrid') then 'six_hybrid'
    when n in ('7h','7hybrid') then 'seven_hybrid'
    when n in ('1i','1iron','1') then 'one_iron'
    when n in ('2i','2iron','2') then 'two_iron'
    when n in ('3i','3iron','3') then 'three_iron'
    when n in ('4i','4iron','4') then 'four_iron'
    when n in ('5i','5iron','5') then 'five_iron'
    when n in ('6i','6iron','6') then 'six_iron'
    when n in ('7i','7iron','7') then 'seven_iron'
    when n in ('8i','8iron','8') then 'eight_iron'
    when n in ('9i','9iron','9') then 'nine_iron'
    when n in ('pw','pitching','pitchingwedge') then 'pitching_wedge'
    when n in ('aw','approach','approachwedge') then 'approach_wedge'
    when n in ('gw','gap','gapwedge') then 'gap_wedge'
    when n in ('sw','sand','sandwedge','54','55','56') then 'sand_wedge'
    when n in ('lw','lob','lobwedge','58','59','60','61','62') then 'lob_wedge'
    when n in ('ulw','ultralob','ultralobwedge','63','64','65') then 'ultra_lob_wedge'
    when n in ('p','putt','putter') then 'putter'
    else null
  end;
end;
$$;

update public.practice_units
set default_club_reference = public.normalize_club_code(default_club_reference)
where default_club_reference is not null;

update public.practice_session_items
set club_reference = public.normalize_club_code(club_reference)
where club_reference is not null;

drop function if exists public.normalize_club_code(text);

-- ============================================================
-- 6. Add FK constraints now that all values are valid codes or null
-- ============================================================

alter table public.practice_units
  add constraint practice_units_default_club_reference_fk
  foreign key (default_club_reference) references public.clubs (code) on delete set null;

alter table public.practice_session_items
  add constraint practice_session_items_club_reference_fk
  foreign key (club_reference) references public.clubs (code) on delete set null;
