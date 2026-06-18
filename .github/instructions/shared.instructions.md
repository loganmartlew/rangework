---
applyTo: "apps/mobile/shared/**/*.kt,apps/mobile/shared/**/*.kts"
---

# Shared module instructions

- `apps/mobile/shared` is the Kotlin Multiplatform core. It owns domain models, draft models, validation, use cases, repository interfaces, auth/data foundations, and Supabase-backed repository adapters.
- Keep `commonMain` platform-agnostic. Android-specific integrations belong in `androidMain` or `androidApp`, not in shared common code.
- Preserve the current layering: models and validation helpers feed use cases, use cases normalize input and orchestrate repositories, and repository adapters map to Supabase/PostgREST.
- Prefer putting trimming, normalization, ordering fixes, and validation in shared use cases or model helpers instead of repeating them in the Android UI layer.
- Shared models are serialized with `kotlinx.serialization`; keep wire/storage-facing changes compatible with the repository row mappings.
- When changing repository behavior or model validation, add or update focused tests in `apps/mobile/shared/src/commonTest`.
