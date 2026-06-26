# Shared module instructions

`apps/mobile/shared` is the Kotlin Multiplatform core. It owns domain models, draft models, validation, use cases, repository interfaces, auth/data foundations, and Supabase-backed repository adapters.

## File map

- `apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/auth/` — auth state, repository interface, and foundation code.
- `apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/data/DataFoundation.kt` and `.../auth/AuthFoundation.kt` — assemble use cases from repositories.
- `apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/repository/` — repository interfaces: `PracticeUnitRepository`, `PracticeSessionRepository`, `MeasurementPreferencesRepository`, `ClubRepository`, `TagRepository`.
- `apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/model/` also holds the tagging vocabulary: `Tag`, the pure `slugifyTag` (mirrored in SQL/MCP), the OR `tagFilterMatches`/`filteredByAnyTag` predicate, and the per-item cap (`MAX_TAGS_PER_ITEM`) enforced in `DraftValidation`.
- `apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/data/Supabase*Repository.kt` — Supabase-backed implementations for all four repository interfaces plus `SupabaseAuthRepository`.
- `apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/usecase/` — use cases for units, sessions, clubs, measurement preferences, auth (observe, restore, sign-in, sign-out), and app bootstrap messaging.
- `apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/model/` — serializable domain models, draft models, validation helpers.
- `apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/config/` — `AppEnvironment` and `GoogleAuthConfig`.
- `apps/mobile/shared/src/commonTest/` — unit tests for shared use cases (primary regression coverage).

## Conventions

- Keep `commonMain` platform-agnostic. Android-specific integrations belong in `androidMain` or `androidApp`, not shared common code.
- Layering: models and validation helpers feed use cases → use cases normalize input and orchestrate repositories → repository adapters map to Supabase/PostgREST.
- Put trimming, normalization, ordering fixes, and validation in shared use cases or model helpers — not repeated in the Android UI layer.
- Shared models are serialized with `kotlinx.serialization`; keep wire/storage-facing changes compatible with repository row mappings.
- When changing repository behavior or model validation, add or update focused tests in `apps/mobile/shared/src/commonTest`.
