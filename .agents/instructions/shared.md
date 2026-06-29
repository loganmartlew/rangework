# Shared module instructions

`apps/mobile/shared` is the Kotlin Multiplatform core. It owns domain models, draft models, validation, use cases, repository interfaces, auth/data foundations, and Supabase-backed repository adapters.

## File map

- `apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/auth/` — auth state, repository interface, and foundation code.
- `apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/library/` — `PracticeLibrary` interface, `PracticeLibraryResult` sealed type, and `DefaultPracticeLibrary` implementation. Owns validation, normalization, duplicate assembly, and restore logic; composes the thinned `PracticeUnitRepository` / `PracticeSessionRepository` persistence seam.
- `apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/data/DataFoundation.kt` and `.../auth/AuthFoundation.kt` — assemble use cases from repositories. `DataFoundation` exposes `practiceLibrary: PracticeLibrary` (not the raw planning repos).
- `apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/repository/` — repository interfaces: `PracticeUnitRepository`, `PracticeSessionRepository` (thinned to `persist`/`get`/`list`/`delete`), `MeasurementPreferencesRepository`, `ClubRepository`.
- `apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/data/Supabase*Repository.kt` — Supabase-backed implementations for all repository interfaces plus `SupabaseAuthRepository`.
- `apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/data/InMemory*Repository.kt` — in-memory adapters for `PracticeUnitRepository` and `PracticeSessionRepository` (commonMain, shared by commonTest and androidApp tests).
- `apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/usecase/` — use cases for clubs, measurement preferences, auth (observe, restore, sign-in, sign-out), and app bootstrap messaging.
- `apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/model/` — serializable domain models, draft models, validation helpers.
- `apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/config/` — `AppEnvironment` and `GoogleAuthConfig`.
- `apps/mobile/shared/src/commonTest/` — unit tests for shared use cases and `PracticeLibrary` (primary regression coverage).

## Conventions

- Keep `commonMain` platform-agnostic. Android-specific integrations belong in `androidMain` or `androidApp`, not shared common code.
- Layering: models and validation helpers feed use cases → use cases normalize input and orchestrate repositories → repository adapters map to Supabase/PostgREST.
- Put trimming, normalization, ordering fixes, and validation in shared use cases or model helpers — not repeated in the Android UI layer.
- Shared models are serialized with `kotlinx.serialization`; keep wire/storage-facing changes compatible with repository row mappings.
- When changing repository behavior or model validation, add or update focused tests in `apps/mobile/shared/src/commonTest`.
