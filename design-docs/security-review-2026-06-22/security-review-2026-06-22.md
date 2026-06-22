# Rangework Security Review — 2026-06-22

**Scope:** Full application security audit of the Rangework monorepo — Supabase backend
(schema, RLS, RPCs, edge function), the Android/KMP mobile app (`apps/mobile`), the static
marketing/legal site (`apps/site`), build configuration, CI/CD, and dependency surface.

**Method:** Static review of all SQL migrations, Kotlin shared/Android code, Gradle build
config, GitHub Actions workflows, the Deno edge function, and a dependency audit. No dynamic
testing or live-environment probing was performed.

**Reviewer:** Claude (Opus 4.8), automated assisted review.

---

## Executive summary

**Overall posture: strong.** The application is built on a sound security foundation. Row-Level
Security is enabled and correctly owner-scoped on every user table, the public Supabase anon key
is backed by real RLS (not relied on as a secret), server-side functions pin `search_path` and
correctly separate `security invoker` from `security definer`, account deletion validates the
caller server-side, and no secrets are committed to source control. Cleartext traffic is blocked
and the Android attack surface is minimal (single exported launcher activity, INTERNET permission
only, `allowBackup=false`).

**No Critical or High severity issues were found.** The findings below are hardening and
privacy/hygiene improvements. The two most actionable items are adding a **nonce** to the Google
ID-token sign-in flow and closing a **PII-retention gap** in the audit-log scrub on account
deletion.

### Findings at a glance

| # | Severity | Finding | Area |
|---|----------|---------|------|
| 1 | Medium | Google ID-token sign-in performs no nonce binding (replay protection) | Android auth |
| 2 | Low | Account-deletion audit scrub leaves PII (email/names) in cascade-delete events | Backend / privacy |
| 3 | Low | Session tokens stored in unencrypted SharedPreferences (default session manager) | Android storage |
| 4 | Low | WebView has JavaScript enabled with no navigation restriction / file-access hardening | Android WebView |
| 5 | Low | `supabase/.temp/` committed — exposes project ref, pooler host, Postgres version | Repo hygiene |
| 6 | Low | Sentry: 100% trace sampling and no PII scrubbing in production | Observability / privacy |
| 7 | Low | Transitive dev-only `esbuild` advisory in `apps/site` (GHSA-g7r4-m6w7-qqqr) | Dependencies |
| 8 | Info | GitHub Actions pinned to mutable tags rather than commit SHAs | CI/CD supply chain |
| 9 | Info | Audit log stores full-row PII snapshots (well-access-controlled) | Backend / privacy |

---

## Strengths (verified, no action required)

- **RLS everywhere.** `profiles`, `user_preferences`, `practice_units`,
  `practice_unit_instructions`, `practice_sessions`, `practice_session_items`,
  `user_enabled_clubs`, `range_sessions`, and `range_session_time_entries` all have RLS enabled
  with owner-scoped policies (`auth.uid() = owner_id`/`user_id`). Child tables verify parent
  ownership in **both** `using` and `with check` clauses, and `practice_session_items`
  additionally validates that the referenced `practice_unit` is owned by the caller — preventing
  cross-user references (IDOR).
- **Least-privilege grants.** The `anon` role receives **no** table grants; only `authenticated`
  can reach data tables. The `clubs` catalog is read-only reference data. This means
  unauthenticated PostgREST requests cannot touch user data even before RLS is consulted.
- **Correct function security model.** Save/start RPCs (`save_practice_unit`,
  `save_practice_session`, `start_range_session`) are **`security invoker`**, so RLS applies to
  every statement inside them; the explicit `owner_id = auth.uid()` predicates are
  belt-and-suspenders. `security definer` functions (`audit.log_change`,
  `scrub_user_audit_log`, `sync_profile_from_auth_user`, `seed_default_clubs_for_profile`) all
  pin `set search_path` and fully-qualify object names, closing the search-path hijack vector.
- **Audit schema isolation.** `audit.events` lives outside `public` (not exposed by PostgREST),
  has RLS enabled with **no policies**, and no client role holds table grants — only
  `service_role` can read it.
- **Account deletion is server-validated.** The `delete-account` edge function validates the JWT
  via `auth.getUser()` (server round-trip, not a local decode) and derives the target user id
  from the validated token — never from request input — eliminating IDOR. The privileged
  `scrub_user_audit_log` is `revoke`d from all client roles and granted only to `service_role`.
- **No hardcoded secrets.** Supabase URL/anon key, Google web client ID, and signing credentials
  are sourced from Gradle properties / environment / `.env` (git-ignored) and injected via
  `BuildConfig` or GitHub Secrets. A full-repo scan for JWTs, Google API keys, private keys, and
  passwords found nothing committed.
- **Transport security.** `network_security_config.xml` sets `cleartextTrafficPermitted=false`
  with system trust anchors; the manifest references it. All backend endpoints are HTTPS.
- **Minimal Android surface.** Only `MainActivity` is exported (MAIN/LAUNCHER only — no deep-link
  intent filter), there are no exported services/receivers/content-providers, the only
  permission is `INTERNET`, `allowBackup=false`, and release builds enable R8
  minify + resource shrink.
- **Safe query construction.** All PostgREST access uses the type-safe `filter { eq(...) }` DSL
  with `@Serializable` DTOs — parameterized and URL-encoded. No string-concatenated filters or
  raw SQL on the client. No SQL/filter-injection vector found.
- **Hardened CI.** The build workflow uses `permissions: contents: read`, the safe
  `pull_request` trigger (not `pull_request_target`), and `--frozen-lockfile`. The release
  workflow is `workflow_dispatch`-only, pulls all signing/config secrets from GitHub Secrets,
  and deletes the decoded keystore with `if: always()`.
- **No sensitive logging.** No `Log.*`, `println`, or `printStackTrace` calls exist in app code;
  tokens are never written to logs or UI state.

---

## Detailed findings

### 1. Google ID-token sign-in performs no nonce binding — **Medium**

**Location:**
[`AndroidGoogleIdTokenProvider.kt`](apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/auth/AndroidGoogleIdTokenProvider.kt:39),
[`SupabaseAuthRepository.kt:26`](apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/auth/SupabaseAuthRepository.kt:26)

`GetSignInWithGoogleOption.Builder(webClientId).build()` is constructed without `.setNonce(...)`,
and `client.auth.signInWith(IDToken) { … }` sets no `nonce`. The Google ID token is therefore
not cryptographically bound to this specific sign-in request.

**Impact:** Without a nonce, a previously issued, still-valid Google ID token with the same
audience (`aud` = this app's web client ID) could be replayed/injected to authenticate. Supabase
does validate the token's signature, issuer, expiry, and `aud` server-side, which substantially
narrows the practical attack window (an attacker cannot mint a token, and tokens for other OAuth
clients are rejected). The residual risk is token replay within validity — the exact class of
attack the nonce is designed to defeat, and which Supabase's own Android guidance recommends
mitigating.

**Recommendation:** Generate a random nonce per sign-in; pass the **SHA-256 hash** of it to
`GetSignInWithGoogleOption.Builder(...).setNonce(hashedNonce)`, and pass the **raw** nonce to
`signInWith(IDToken) { nonce = rawNonce }`. Thread the value through `requestIdToken()` →
`GoogleIdTokenRequestResult.Success` → `signInWithGoogleIdToken(...)`.

```kotlin
val rawNonce = java.util.UUID.randomUUID().toString()
val hashedNonce = java.security.MessageDigest.getInstance("SHA-256")
    .digest(rawNonce.toByteArray()).joinToString("") { "%02x".format(it) }
// hashedNonce -> GetSignInWithGoogleOption; rawNonce -> signInWith(IDToken){ nonce = rawNonce }
```

---

### 2. Account-deletion audit scrub leaves PII in cascade-delete events — **Low**

**Location:**
[`scrub_user_audit_log`](supabase/migrations/20260621200000_delete_account_helpers.sql:13),
[`delete-account/index.ts:36`](supabase/functions/delete-account/index.ts:36),
[audit triggers](supabase/migrations/20260621100100_audit_log_triggers.sql)

The edge function calls `scrub_user_audit_log(user.id)` (which deletes
`audit.events where actor_id = p_user_id`) **before** `auth.admin.deleteUser`. When `deleteUser`
runs, the cascade `DELETE` of `public.profiles` (and child rows) fires the `audit.log_change`
triggers in the **service-role connection**, where `auth.uid()` is `null`. Those audit rows are
therefore written with `actor_id = null` and survive the scrub. The `profiles` delete event's
`old_values` contains the user's `email`, `first_name`, and `last_name`.

**Impact:** After a user deletes their account, PII (notably email and names) remains in
`audit.events`. The audit table is service-role-only, so this is not externally exposed, but it
defeats the stated "scrub PII before deletion" intent and is relevant to data-erasure/GDPR
obligations.

**Recommendation:** Make the scrub cover deletion-generated rows. Options, simplest first:
- Scrub by ownership as well as actor, e.g. also delete events for the user's own rows
  (`where table_name = 'profiles' and row_id = p_user_id`, plus owned units/sessions), **or**
- Temporarily suppress audit triggers for the cascade by running the user delete with
  `set local session_replication_role = replica;` in a `security definer` helper, **or**
- Run the PII scrub **after** deletion with a match broad enough to catch `actor_id is null`
  events referencing the deleted user's rows.

---

### 3. Session tokens stored in unencrypted SharedPreferences — **Low**

**Location:**
[`RangeworkSupabaseClientFactory.kt:18`](apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/data/RangeworkSupabaseClientFactory.kt:18)

`install(Auth)` uses supabase-kt's default `SettingsSessionManager`, which persists the access
and refresh tokens in plaintext SharedPreferences. `allowBackup=false` already prevents
`adb backup` extraction.

**Impact:** On a rooted or otherwise compromised device, or via malware with access to the app's
data directory, the refresh token could be read and used to impersonate the user until it is
revoked. This is the default behaviour for supabase-kt on Android and a common posture, but it is
hardenable.

**Recommendation:** Provide a custom `SessionManager` that stores tokens via
`EncryptedSharedPreferences` / a key wrapped by the Android Keystore. Pass it to
`install(Auth) { sessionManager = … }`.

---

### 4. WebView: JavaScript enabled, no navigation restriction or file-access hardening — **Low**

**Location:**
[`WebViewScreen.kt:32`](apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/WebViewScreen.kt:32)

The legal-pages WebView sets `settings.javaScriptEnabled = true` and does not override
`shouldOverrideUrlLoading`, nor explicitly disable `allowFileAccess` / `allowContentAccess`.

**Mitigating factors (verified):** The target URL is resolved through a fixed allowlist of three
first-party HTTPS URLs (`rangework.app/...`), with any unknown key falling back to the app's own
homepage — so the loaded URL is **not** attacker-controllable. There is **no**
`addJavascriptInterface` bridge (no JS→native RCE path), and the hosting route has no external
deep link. Cleartext is blocked globally.

**Impact:** Limited. The main residual risk is that a link tapped inside a legal page (or a
defacement of the first-party site) navigates the JS-enabled WebView to off-domain content within
the app.

**Recommendation:** Override `shouldOverrideUrlLoading` to keep navigation on `rangework.app` and
hand off-domain links to the system browser; explicitly set `allowFileAccess = false` and
`allowContentAccess = false`; leave `domStorageEnabled` off unless required. Consider disabling
JavaScript if the legal pages render without it.

---

### 5. `supabase/.temp/` committed to source control — **Low**

**Location:** `supabase/.temp/*` (tracked), [`.gitignore`](.gitignore)

The Supabase CLI scratch directory is checked in, exposing `linked-project.json` (project ref
`fdtbtgfuxeawkyvcrlau`, organization slug), the **direct pooler connection string**
(`postgresql://postgres.fdtbtgfuxeawkyvcrlau@aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres`),
and the exact Postgres version (`17.6.1.127`).

**Impact:** No credential (password) is exposed, and the project ref is already public via the API
URL baked into the app. However, the direct DB host/username, pooler endpoint, and precise
Postgres version are infrastructure details that aid targeted attacks (direct-connection
attempts, version-specific exploit selection). This is primarily a hygiene/information-disclosure
issue.

**Recommendation:** Add `supabase/.temp/` (and `.branches`) to `.gitignore` and remove from
history of the working tree: `git rm -r --cached supabase/.temp`. These are regenerated locally
by the CLI.

---

### 6. Sentry: 100% trace sampling and no PII scrubbing in production — **Low**

**Location:**
[`RangeworkApplication.kt:9`](apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/RangeworkApplication.kt:9)

`SentryAndroid.init` sets `tracesSampleRate = 1.0` and defines no `beforeSend`/PII-scrubbing.
(The DSN is hardcoded, but a Sentry DSN is client-distributable by design and is **not** a
secret — flagged only for awareness.)

**Impact:** Sampling every transaction is a cost/quota concern at scale. More importantly, without
a `beforeSend` filter, exception messages and breadcrumbs captured in production could include
user PII (emails, practice content). Sentry does not send IP/PII by default (`sendDefaultPii` is
off), which limits — but does not eliminate — the exposure.

**Recommendation:** Lower `tracesSampleRate` for release builds (e.g. 0.1–0.2); add a `beforeSend`
that strips/redacts PII; explicitly set `isSendDefaultPii = false`; optionally gate Sentry init to
release builds only.

---

### 7. Dev-only `esbuild` advisory in `apps/site` — **Low**

**Location:** `apps/site` dependency tree (transitive via `astro` / `@tailwindcss/vite` → `vite`
→ `esbuild`); advisory GHSA-g7r4-m6w7-qqqr.

`pnpm audit` reports `esbuild >=0.27.3 <0.28.1` (arbitrary file read via the dev server on
Windows). This affects only the local dev server, not the static production build, and `apps/site`
ships as static HTML.

**Recommendation:** Update to `esbuild >= 0.28.1` (refresh `astro`/`vite`, or add a pnpm
`overrides` entry). Low urgency given dev-only impact.

---

### 8. GitHub Actions pinned to mutable tags — **Informational**

**Location:** [`android.yml`](.github/workflows/android.yml),
[`release.yml`](.github/workflows/release.yml)

Third-party actions (`pnpm/action-setup@v4`, `actions/setup-*@v4`,
`android-actions/setup-android@v3`, `gradle/actions/setup-gradle@v4`,
`softprops/action-gh-release@v2`) are pinned to mutable major tags. A compromised or retagged
action could execute in CI — and the release workflow has access to signing and backend secrets.

**Recommendation:** Pin third-party actions to full commit SHAs (Dependabot can keep them
updated). First-party `actions/*` are lower risk but pinning is still best practice.

---

### 9. Audit log stores full-row PII snapshots — **Informational**

**Location:** [`audit.log_change`](supabase/migrations/20260621100000_audit_log_schema.sql:38)

`audit.events` retains complete `old_values`/`new_values` JSON snapshots, including `profiles`
rows (email, names). Access is correctly restricted to `service_role` (see Strengths), so this is
not an exposure today — noted for data-minimization/retention awareness. Consider a column
allowlist/denylist for sensitive fields and a retention/TTL policy on the audit table. (Closing
finding #2 also reduces lingering PII here.)

---

## Prioritized remediation plan

1. **#1 — Add nonce to Google sign-in** (Medium; small, localized code change).
2. **#2 — Close the audit-scrub PII gap** on account deletion (Low; correctness + compliance).
3. **#5 — Untrack `supabase/.temp/`** and update `.gitignore` (Low; one-time hygiene).
4. **#3 — Encrypt session storage** via a custom `SessionManager` (Low; defense-in-depth).
5. **#4 / #6 — WebView and Sentry hardening** (Low; quick config changes).
6. **#7 / #8 — Bump `esbuild`; pin Actions to SHAs** (Low/Info; supply-chain hygiene).

---

## Appendix — key dependency versions reviewed

| Component | Version |
|-----------|---------|
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.0.21 |
| compileSdk / targetSdk / minSdk | 35 / 35 / 26 |
| supabase-kt (auth/postgrest/functions) | 3.0.0 |
| Ktor | 3.1.3 |
| androidx.credentials | 1.6.0 |
| googleid | 1.1.1 |
| Sentry Android Gradle plugin | 4.14.0 |
| Astro (`apps/site`) | ^6.4.7 |
| turbo | ^2.5.4 |

No known Critical/High CVEs were identified for the Kotlin/Android dependency versions during this
review. An offline static review cannot substitute for continuous scanning — enable **Dependabot**
(or OWASP Dependency-Check / `gradle dependencyCheckAnalyze`) on both the Gradle and pnpm trees so
new advisories surface automatically.

---

*This review covered code and configuration as of commit `a94c571` on `main`. It is a static
assessment; a penetration test against a live environment (auth flows, rate limiting, RLS under
real tokens) is recommended before a production launch.*
