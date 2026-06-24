# Baseline plan: Rangework

## App identity

- **App name:** Rangework
- **Application ID:** `com.loganmartlew.rangework.android`
- **Shared package:** `com.loganmartlew.rangework.shared`

## Recommended baseline

Build the app as an **Android-first, mobile-first monorepo** with:

- **Kotlin Multiplatform** for shared domain, data, and auth logic
- **Native Jetpack Compose** for the Android UI
- **Supabase** as the initial backend platform
- **Supabase Postgres** as the primary database
- **Google sign-in** from day one
- **No full offline sync in the first coded phase, but leave room for a future local SQL layer**
- **No iOS app, no Firebase, and no custom backend service** in the first baseline

This is the best fit for the current product direction because the app is:

- mandatory sign-in and cloud-sync from day one
- remote-first in the first coded phase, with room for offline later
- private per-user data only
- planning-focused, not execution-tracking-focused
- more dependent on **relational data and reporting flexibility** than on push, analytics, or crash tooling

## Product decisions captured so far

- **Repo shape:** mobile-first monorepo
- **Backend platform:** Supabase remains the preferred baseline over Firebase
- **Auth model:** mandatory sign-in with cloud sync from day one
- **Auth provider:** Google sign-in only for launch
- **User scope:** private per-user data only in v1, with no additional roles currently planned
- **Device scope:** Android first, with tablet-specific layouts included in the baseline scope
- **Release target:** public-ready foundation rather than an internal-only prototype
- **Primary v1 scope:** practice planning and session composition; execution/result logging comes later
- **Practice unit structure:** units need a variable number of ordered instructions; each instruction may reference a club and include reps/ball count
- **Session model:** reusable templates only for now
- **Session composition rule:** session items should reference live units, and duplicate use of the same unit in one session is allowed
- **Session item fields:** per-item notes, rest timers, and focus cues are wanted
- **Historical logging rule:** when execution logging arrives later, logged practice records should preserve a snapshot of the unit as performed
- **Future metric model:** multiple granularities should be supported later
- **Future metric creation:** curated metrics plus optional custom fields
- **Future analytics:** cross-unit analytics should be anticipated, including individual-ball club/ball data points
- **Future metric inputs:** typed inputs and validation rules will be needed
- **Future metric units:** metrics should not hard-code display units; the app should use settings-driven unit preferences instead
- **Future media:** photos and videos are expected later
- **Offline direction:** not required immediately, but the baseline should stay remote-first while leaving repository boundaries ready for future offline entry and sync

## Backend recommendation: Supabase over Firebase

### Reassessment for future custom unit metrics

Future per-unit custom metrics make the backend decision more nuanced, but they do **not** overturn the current recommendation.

They create a **hybrid data problem**:

- the app still has a strongly relational core
- each practice unit may define its own metric schema
- future result records may carry partly unstructured metric values

That mix is still a strong fit for **Supabase/Postgres**, as long as the design intentionally uses both relational tables and `jsonb`.

### Supabase / Postgres pros

1. **The core app is still relational.** Users, practice units, sessions, session items, future session runs, and ownership rules all map cleanly to tables and joins.
2. **Custom metrics do not require abandoning SQL.** Postgres can store dynamic fields in `jsonb` while keeping the rest of the model structured.
3. **Analytics are much stronger.** If you later want trends, filters, aggregates, leaderboards, per-club analysis, or “show me all wedge drills where make rate improved”, SQL remains a major advantage.
4. **Security is cleaner for private user data.** Row Level Security fits a per-user app very well.
5. **You can evolve from flexible to structured over time.** If certain metrics become important, you can promote them from `jsonb` into first-class columns, views, or materialized views without changing platforms.
6. **Schema, constraints, and migrations are first-class.** That matters once the data model starts to matter for reporting quality.

### Supabase / Postgres cons

1. **It needs more design up front.** A dynamic metric system requires deliberate modeling rather than dropping arbitrary objects into documents.
2. **`jsonb` is flexible but less ergonomic than documents.** Queries, indexes, and validation are more explicit.
3. **Dynamic form validation may live partly in app logic.** You may also add database-side checks later, but it is not as “schema-free” as Firestore.
4. **Offline sync is not a built-in strength.** If offline logging becomes important later, Supabase will need more client-side architecture than Firebase.
5. **You should avoid overusing `jsonb` for everything.** If the whole model turns into arbitrary blobs, you lose the main benefits of Postgres.

### Firebase / Firestore pros

1. **Custom metric payloads feel natural.** A unit can store a nested metric schema object, and each logged result can store a nested values object with minimal ceremony.
2. **Schema evolution is very fast early on.** You can add new fields or change structures without migrations.
3. **Offline support is much stronger out of the box.** If mobile logging in patchy connectivity becomes central later, Firebase gains real weight.
4. **The client SDKs are optimized for app-first document workflows.** For flexible nested data capture, Firestore is operationally simple.

### Firebase / Firestore cons

1. **The rest of your domain is still relational.** Sessions composed from reusable units are more awkward in a document database and usually push you toward denormalization.
2. **Analytics become harder faster.** Cross-document aggregates, richer reporting, and ad hoc historical analysis are much weaker than SQL and often push data into BigQuery.
3. **Schema drift is easy.** The same “flexibility” that helps early can become inconsistent payloads later.
4. **Security and consistency across related entities can get more complex.** Firestore rules are powerful, but relational ownership patterns are less natural there than RLS in Postgres.
5. **Read-heavy reporting can become costlier.** Document reads and denormalized access patterns are often less efficient for analysis-style workloads.

### Decision

**Supabase is still the better primary backend** if custom metrics are a **future extension to a relational training app**.

Firebase becomes more attractive only if the product shifts toward:

- document-first data everywhere
- highly variable nested payloads as the dominant model
- lightweight reporting needs
- strong offline sync as a core product requirement

That is not the current shape of this app.

### What this means

- Use **Supabase Auth** for identity
- Use **Supabase Postgres + PostgREST** for app data
- Design future custom metrics as a **hybrid relational + `jsonb` model**
- Use **Row Level Security** to restrict every app table to the authenticated user
- **Do not build a custom API service yet**
- **Do not mix in Firebase yet**

### When to revisit Firebase later

Revisit Firebase only if the product later prioritizes:

- push notifications
- analytics and crash reporting
- device-side offline sync as a major requirement
- a broader Google-native mobile services bundle
- document-first custom data with only light reporting needs

If that happens, a **hybrid** setup is still possible later: keep Supabase for relational data and add selected Firebase services separately.

## Primary frameworks, libraries, and technologies

| Area | Recommendation | Notes |
| --- | --- | --- |
| UI | **Jetpack Compose** | Android-native UI only; do not use Compose Multiplatform UI yet |
| Design system | **Material 3** | Standard Android baseline for theming and components |
| Navigation | **Navigation Compose** | Keep Android navigation local to the Android app module |
| Shared logic | **Kotlin Multiplatform** | Shared models, repositories, auth coordination, use cases |
| Concurrency | **Kotlin Coroutines + Flow** | Shared async and reactive state streams |
| Serialization | **kotlinx.serialization** | Shared DTO and payload mapping |
| Date/time | **kotlinx-datetime** | Shared date and time handling |
| DI | **Koin** | Simpler KMP-friendly DI than Hilt for a shared-logic setup |
| Backend platform | **Supabase** | Auth, Postgres, PostgREST, RLS |
| Auth | **Supabase Auth + Google sign-in** | Android obtains Google credentials, Supabase owns app auth/session state |
| Android sign-in libs | **Credential Manager** + **Google ID** | Modern Android Google sign-in path |
| Remote data client | **supabase-kt** | Use `auth-kt` and `postgrest-kt` first |
| HTTP engine | **Ktor client engine for Android** | Use an Android/JVM engine such as OkHttp in `androidMain` |
| Local persistence | **No full offline sync initially** | Keep room for a future SQLDelight-backed local data layer if offline becomes near-term |
| Lightweight local storage | **Secure auth/session persistence and app settings** | Keep local storage minimal now, but persist user settings such as unit preferences |
| Config/secrets | **Gradle-managed env config + shared config wrapper** | Keep Supabase URL and anon key out of source literals |
| Testing | **kotlin.test + JUnit + Turbine** | Shared logic tests and Flow testing |
| CI/CD | **GitHub Actions** | Add baseline build and test workflow once implementation starts |

## Android app architecture

### Recommended app architecture

Use a **lean layered architecture**:

- **Android app module** owns UI, navigation, ViewModels, and Android platform integrations
- **Shared KMP module** owns:
  - domain models
  - repositories
  - auth/session coordination
  - use cases
  - DTO mapping
  - Supabase integration

### Presentation pattern

Use:

- **Jetpack Compose**
- **AndroidX ViewModel**
- **unidirectional state flow**

Keep ViewModels in the Android app module. Keep business rules in the shared KMP module so the future iOS app can reuse them.

### Why not Hilt

Hilt is strong for Android-only apps, but this project already wants KMP shared logic. **Koin** is the more practical baseline because it works cleanly across shared and Android code without creating an Android-only dependency pattern in core logic.

## Backend architecture

### Baseline backend shape

Do **not** create a separate backend service initially.

Instead:

- the Android app talks to **Supabase Auth**
- the shared data layer talks to **Supabase PostgREST**
- database security is enforced with **Row Level Security**
- backend schema changes live as **Supabase SQL migrations** in the repo

### Security model

- The mobile app may contain the **Supabase project URL** and **anon key**
- The mobile app must **never** contain the **service role key**
- All user data access must be protected with **RLS policies**
- Every user-owned table should include a `user_id` column tied to `auth.uid()`

### Edge Functions

Do **not** include Supabase Edge Functions in the baseline unless a real server-side rule appears.

Examples of features that would justify them later:

- AI-generated session suggestions
- protected integrations with third-party APIs
- privileged server-side jobs
- scheduled reporting or notifications

## Database recommendation

### Primary database

Use **Supabase Postgres**.

### Why

- relational model fits practice units and session composition
- joins and aggregates are straightforward
- future reporting is much easier than a document-first model
- schema migrations and constraints are first-class

### How unstructured metric data can work with Postgres

Custom per-unit metrics are not an all-or-nothing problem. Postgres supports several patterns.

#### Pattern 1: JSONB-only

Store:

- a metric schema on the practice unit, such as `metric_schema jsonb`
- captured metric values on future result rows, such as `metric_values jsonb`

This is the fastest way to support flexible inputs.

**Pros**

- fastest to ship
- easiest to support widely different unit types
- very natural for dynamic forms

**Cons**

- weaker validation
- queries are more verbose
- cross-unit analytics get harder as custom fields diverge

#### Pattern 2: fully normalized metric tables

Store metric definitions as rows and metric values as rows.

Example future tables:

- `practice_unit_metric_definitions`
- `practice_unit_result_metric_values`

This is closer to an entity-attribute-value model.

**Pros**

- stronger validation
- better indexing and reporting
- easier to attach metadata like units, input type, ordering, constraints, and analytics category

**Cons**

- more complex schema
- more joins
- more app mapping code

#### Pattern 3: recommended hybrid model

For this app, the best future direction is a **hybrid**:

1. keep the **relational core** for users, units, sessions, session items, and future result records
2. store **metric definitions** in a relational table
3. allow flexible metric configuration via `jsonb`
4. store captured result payloads in `jsonb` initially
5. promote high-value metrics into typed columns, views, or reporting tables later when analytics demand it

This gives you flexibility **without** giving up SQL where it matters.

### Recommended future metric model in Postgres

When execution logging is eventually added, aim for something like:

#### `practice_unit_metric_definitions`

One row per metric field configured for a unit:

- `practice_unit_id`
- `key`
- `label`
- `input_type` such as integer, decimal, boolean, text, select, duration
- `required`
- `sort_order`
- `config_jsonb` for options like min/max, select options, display unit, help text
- optional `analytics_key` if different units should roll up into a shared reporting concept

#### `practice_unit_results`

One row per logged execution of a unit:

- `practice_unit_id`
- `user_id`
- `session_run_id` or equivalent
- `recorded_at`
- `notes`
- `unit_snapshot_jsonb`
- `metric_values_jsonb`

### Why this hybrid is strong

- the app can render dynamic inputs from metric definitions
- the result payload can stay flexible
- common relational queries stay easy
- heavy-reporting metrics can be indexed with expression indexes or extracted into views later
- if a metric becomes important enough, it can be promoted without redesigning the whole backend

### Important caveat

If you eventually need **deep analytics across arbitrary custom fields**, neither Supabase nor Firebase is magically simple. The real question becomes whether those custom metrics stay:

- mostly **unit-specific and local**, or
- broadly **normalized across the whole product**

If they stay unit-specific, `jsonb` is fine.

If they become cross-product KPIs, define shared metric semantics early and model those more explicitly.

### Metric semantics and unit handling

Because cross-unit analytics and settings-driven units are expected later, the future metric model should follow these rules:

1. define an app-level catalog of canonical metric semantics for common metrics, including ball- and club-oriented data points
2. allow optional custom metrics on top of that catalog
3. keep metric semantics separate from display units
4. store values in canonical forms where relevant, then convert for display based on user settings
5. model dimensions such as distance, speed, angle, count, percentage, duration, and boolean separately from user-facing unit labels

That means a metric definition should prefer concepts like:

- `semantic_key`
- `value_type`
- `value_dimension`
- validation and presentation config

...instead of hard-coding a display unit like `m` or `mph` into the core metric identity.

### Initial data model direction

Start with these core entities:

#### `profiles`

- one row per authenticated user
- display metadata only

#### `practice_units`

Reusable building blocks such as:

- title
- description/instructions
- default ball count
- optional club/focus/tag metadata
- created/updated timestamps

#### `practice_sessions`

Reusable saved session templates such as:

- name
- description/notes
- created/updated timestamps

#### `practice_session_items`

Join records that compile units into a session, such as:

- `practice_session_id`
- `practice_unit_id`
- `sort_order`
- optional overrides like ball count or notes

### Important product decision still open

Before implementation starts, decide whether a session item should:

1. **Reference a live practice unit** and optionally override some fields, or
2. **Snapshot the unit contents** into the session at the time it is added

That decision affects the schema details, but it does **not** change the recommended baseline stack.

## Local storage strategy

### Do not implement full offline sync in the first coded phase

Because the current direction is still:

- sign-in and cloud-sync first
- planning-focused rather than field logging
- baseline delivery before sync complexity

...the first coded phase should stay lean and avoid:

- a full sync engine
- conflict resolution logic
- an offline-first repository implementation

### What local storage planning is still needed

You still need minimal local persistence for:

- auth/session continuity
- small app settings if they appear later

### Recommendation

- Let auth/session persistence stay **minimal and secure**
- Do not implement full offline sync yet
- Keep repository boundaries and domain models clean enough that a local SQL layer can be introduced later
- If offline entry or richer caching becomes near-term, prefer **SQLDelight** over Room because it aligns better with a future KMP expansion

## Google sign-in plan

For Android, use the modern Google sign-in stack:

- **AndroidX Credential Manager**
- **Google ID library**
- **Supabase Auth** as the app's identity/session authority

High-level flow:

1. User taps Google sign-in
2. Android gets a Google credential
3. The app exchanges that identity with Supabase Auth
4. Supabase returns and manages the app session

This keeps identity aligned with the chosen backend instead of splitting auth across vendors.

## Repo structure to target

Because the repo is currently empty, set it up intentionally around the chosen architecture.

```text
/
  baseline-plan.md
  settings.gradle.kts
  build.gradle.kts
  gradle.properties
  gradle/
    libs.versions.toml
  androidApp/
    build.gradle.kts
    src/
      main/
      test/
      androidTest/
  shared/
    build.gradle.kts
    src/
      commonMain/
      commonTest/
      androidMain/
      androidUnitTest/
  supabase/
    config.toml
    migrations/
    seed.sql
    functions/        # only if needed later
  .github/
    workflows/
```

## Module responsibilities

### `androidApp`

Owns:

- Compose screens
- navigation
- Android ViewModels
- Android sign-in UI flow
- theming
- app entry points

### `shared`

Owns:

- domain models
- use cases
- repository interfaces and implementations
- Supabase client setup
- DTO mapping
- auth/session logic
- shared validation and business rules

### `supabase`

Owns:

- database schema migrations
- RLS policies
- seed data
- optional edge functions later

## Shared module structure recommendation

Keep the first shared setup **simple**: start with **one KMP shared module**, not several.

Use package-level separation inside `shared`:

- `auth`
- `model`
- `data`
- `repository`
- `usecase`
- `config`

Split into multiple shared Gradle modules only after the app grows.

## What not to include in the first baseline

Do **not** include these yet:

- iOS app module
- Compose Multiplatform UI
- Firebase
- Room
- SQLDelight
- push notifications
- analytics
- crash reporting
- media upload/storage
- execution/result logging
- custom backend service
- Supabase Realtime unless a real live-update need appears

## Requirements gathering workflow

Before any code is generated, use this workflow:

1. **Round 1 questionnaire**
   - ask the user for a broad set of answers in one batch
   - cover product behavior, auth, data model, metrics, platforms, and rollout scope
2. **Round 2 follow-up questions**
   - ask only the targeted questions created by gaps, conflicts, or design implications from round 1
3. **Decision consolidation**
   - update this plan with all confirmed decisions
   - freeze the implementation baseline for the first coding phase
4. **Code generation**
   - implement one documented phase at a time only after the user explicitly prompts for that phase

## Round 1 questionnaire for the user

Reply with numbered answers so the decisions can be copied back into this plan.

1. What should a **practice unit** contain in v1 besides title and ball count: instructions, club, target distance, notes, tags, difficulty, duration target, or something else?
2. Should a **practice session** act as a reusable template only, or do you also want the concept of a dated real-world practice session log in the early roadmap?
3. When a unit is added to a session, should the session item be a **live reference** to the unit, a **snapshot copy**, or a hybrid with selected overrides?
4. Should the user be allowed to add the **same unit multiple times** to one session with different overrides?
5. Should session items support **freeform notes**, **rest timers**, **focus cues**, or other per-item fields in v1?
6. Is **Google sign-in only** acceptable at launch, or do you want email/password, magic link, or Apple sign-in planned as later fallbacks?
7. Do you expect any roles beyond a normal golfer account, such as **coach**, **admin**, or **shared team access**?
8. Is the app expected to stay **Android-phone-first**, or should the baseline already consider tablets, Wear OS, web, or a coach/admin web surface?
9. What is the likely **minimum Android level of polish** for the first build: internal prototype, private beta, or public-ready foundation?
10. When custom metric logging arrives, will metrics usually be recorded **per unit completion**, **per set**, **per ball/attempt**, or do you need to support multiple granularities?
11. For future custom metrics, do you want users to create **completely custom fields**, or should they choose from a **curated metric catalog** plus optional custom fields?
12. Do you expect cross-unit analytics later, such as comparing similar metrics across many drills, or will most reporting stay local to a specific unit type?
13. Should custom metric fields support typed inputs like **number**, **decimal**, **boolean**, **text**, **single select**, **multi select**, and **duration**?
14. Will metric fields need validation rules such as **required**, **min/max**, **target range**, or **unit labels** like meters, percent, or makes?
15. Do you expect media attachments later, such as **photos**, **videos**, or **voice notes**, on units or logged sessions?
16. Is **online-only** still acceptable for the first coded version, or do you now want the architecture to leave more room for near-term offline entry and sync?

## Round 1 answers captured

1. Practice units need a variable number of instructions, and each instruction can reference a club.
2. Practice sessions are reusable templates only for now.
3. Session items should use live references.
4. The same unit can appear multiple times in one session with different overrides.
5. Session items should support notes, rest timers, and focus cues.
6. Launch auth should be Google only.
7. No extra roles are currently needed.
8. Tablet support should be considered in the baseline.
9. The baseline should target a public-ready foundation.
10. Future metric logging should support multiple granularities.
11. Future metrics should use a curated catalog plus optional custom fields.
12. Cross-unit analytics should be anticipated.
13. Future metric fields should support typed inputs.
14. Future metric fields should support validation rules and units.
15. Photos and videos are expected later.
16. The architecture should leave room for offline entry and sync.

## Round 2 answers captured

1. Practice-unit instructions should include reps/ball count.
2. Future logged practice records should preserve a snapshot of the unit as performed.
3. The baseline should stay remote-first and only keep repository boundaries ready for SQLDelight later.
4. Tablet-specific layouts should be part of the baseline scope.
5. The future curated metric catalog should include canonical shared semantics, including key club/ball data points for individual-ball granularity, and metric identity should not hard-code units.

## Code generation phases

Implementation should proceed in the following phases once requirements are locked.

### Phase 1: Repository and build skeleton

Scope:

- Gradle Kotlin DSL root setup
- version catalog
- `androidApp` module
- `shared` KMP module
- base package structure
- baseline tooling and CI scaffolding

Exit condition:

- the project builds as an empty app shell with shared module wiring in place

### Phase 2: Backend and auth foundation

Scope:

- Supabase project/config structure in repo
- initial SQL migrations
- RLS policies
- Google auth configuration plan wired into app config
- shared auth client setup

Exit condition:

- sign-in and authenticated session restoration paths are scaffolded end to end

### Phase 3: Shared domain and data foundation

Scope:

- core models
- variable instruction modeling for practice units
- settings and preferences model for unit-system choices
- repository contracts
- Supabase-backed repository implementations
- use cases
- shared validation and serialization

Exit condition:

- shared module exposes stable APIs for units, sessions, and auth state

### Phase 4: Android app shell and navigation

Scope:

- Compose app shell
- navigation structure
- Material 3 theme
- ViewModel wiring
- authenticated and unauthenticated entry flows
- tablet-specific layout foundations in addition to phone layouts

Exit condition:

- Android UI shell is usable and wired to shared state

### Phase 5: Practice units and session composition features

Scope:

- create, edit, delete, and list practice units
- manage ordered unit instructions with club references
- create, edit, delete, and list practice sessions
- add, order, remove, and override units inside sessions
- support per-session-item notes, rest timers, and focus cues

Exit condition:

- the baseline v1 planning workflow works end to end

### Phase 6: Hardening, tests, and release readiness

Scope:

- shared tests
- Android unit tests
- basic UI verification where appropriate
- CI workflow completion
- configuration cleanup and documentation
- public-ready polish for the agreed baseline scope

Exit condition:

- the baseline app is ready for iterative feature expansion

## Final recommendation

The cleanest baseline is:

- **Android app with native Jetpack Compose**
- **one shared KMP module for logic and data**
- **Supabase Auth + Postgres + RLS**
- **a future hybrid relational + `jsonb` model for custom metrics**
- **Google sign-in via Credential Manager**
- **remote-first data access with future room for SQLDelight, but no offline sync in the first coded phase**
- **no Firebase in the initial setup**

That gives you the leanest setup that still matches the app's relational model, live-template session model, tablet-aware UI scope, future reporting needs, and likely path toward custom per-unit metric capture with settings-driven unit handling.
