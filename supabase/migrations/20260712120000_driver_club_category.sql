-- Add DRIVER club category, renumber the woods, and seed mini_driver.
-- Runs after 20260616100000_clubs_catalog_and_user_enabled_clubs.sql.
-- Idempotent (upsert), matching the existing seed.

-- ============================================================
-- 1. Drop and recreate the category check constraint
-- ============================================================

alter table public.clubs drop constraint if exists clubs_category_check;

alter table public.clubs
  add constraint clubs_category_check
    check (category in ('DRIVER', 'WOOD', 'HYBRID', 'IRON', 'WEDGE', 'PUTTER'));

-- ============================================================
-- 2. Shift existing wood/hybrid sort_order up to make room
--    for the Driver family at the front (driver=1, mini_driver=2).
-- ============================================================

update public.clubs set sort_order = sort_order + 1 where sort_order >= 2;

-- ============================================================
-- 3. Move driver from WOOD to DRIVER, and seed mini_driver
-- ============================================================

update public.clubs set category = 'DRIVER', sort_order = 1 where code = 'driver';

insert into public.clubs (code, display_name, category, sort_order, default_enabled) values
  ('mini_driver', 'Mini Driver', 'DRIVER', 2, false)
on conflict (code) do update
  set display_name    = excluded.display_name,
      category        = excluded.category,
      sort_order      = excluded.sort_order,
      default_enabled = excluded.default_enabled;
