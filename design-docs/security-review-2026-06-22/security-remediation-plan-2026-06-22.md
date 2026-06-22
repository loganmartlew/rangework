# Rangework Security Remediation Plan — Findings 1–6

**Companion to:** [`security-review-2026-06-22.md`](security-review-2026-06-22.md)
**Date:** 2026-06-22
**Scope:** Implementation plan for findings **1–6** from the security review (the one Medium and
five Low items). Findings 7–9 (esbuild bump, Action SHA pinning, audit retention) are out of scope
here.

> **How to read this document.** Each fix is self-contained: goal, exact files, step-by-step
> changes with code sketches, tests, verification, and rollback. Code sketches are illustrative —
> match surrounding style and confirm the few **⚠️ verify** points against the installed library
> versions before relying on them. Apply fixes on a branch; do not commit to `main` directly.

## Suggested sequencing

| Order | Finding | Effort | Risk | Why this order |
|-------|---------|--------|------|----------------|
| 1 | #5 — Untrack `supabase/.temp/` | ~5 min | None | Trivial, isolated, no code |
| 2 | #6 — Sentry hardening | ~30 min | Low | Single file, no API surface change |
| 3 | #4 — WebView hardening | ~30 min | Low | Single file, isolated |
| 4 | #2 — Audit scrub PII gap | ~1–2 h | Med | DB migration + edge fn; test in a branch project |
| 5 | #1 — Google sign-in nonce | ~2–3 h | Med | Multi-layer threading + live auth test |
| 6 | #3 — Encrypted session storage | ~3–5 h | Med | KMP plumbing; most involved |

Findings #1 and #3 both require a **live device + real Supabase project** to verify (auth round
trips). Do them when you can run the app end-to-end, not just unit tests.

**Validation after each code change** (from `CLAUDE.md`):

```powershell
Set-Location apps/mobile
.\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest `
  :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest :androidApp:assembleDebug
.\gradlew.bat :shared:lintDebug :androidApp:lintDebug
```

---

## Finding #5 — Untrack `supabase/.temp/` (Low)

**Goal:** Stop tracking the Supabase CLI scratch directory so the pooler host, project ref, and
Postgres version stop being published in new commits.

### Changes

1. **Edit [`.gitignore`](.gitignore)** — add under the existing entries:

   ```gitignore
   # Supabase CLI scratch / local state
   supabase/.temp/
   supabase/.branches/
   ```

2. **Remove from the index** (keeps the local files, drops them from git tracking):

   ```bash
   git rm -r --cached supabase/.temp
   git commit -m "chore: stop tracking supabase/.temp CLI scratch dir"
   ```

### Verification

- `git status` shows `supabase/.temp/` deletions staged and, after commit, the directory no longer
  appears in `git ls-files`.
- `git check-ignore supabase/.temp/project-ref` prints the path (confirms it is now ignored).

### Notes / rollback

- **History is not rewritten.** The values remain in past commits. They are *not* secret
  credentials (no password; the project ref is already in the app's API URL), so a history rewrite
  is optional. If you want them purged from history, that is a separate `git filter-repo` task to
  schedule deliberately (it rewrites SHAs and forces a re-clone for collaborators).
- Rollback: `git revert` the commit; the files were never deleted from disk.

---

## Finding #6 — Sentry: sampling + PII scrubbing (Low)

**Goal:** Stop sampling 100% of transactions in production and prevent user PII from leaving the
device in crash/trace payloads.

**File:** [`RangeworkApplication.kt`](apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/RangeworkApplication.kt)

### Changes

Replace the `SentryAndroid.init` body:

```kotlin
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid

class RangeworkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SentryAndroid.init(this) { options ->
            options.dsn = BuildConfig.SENTRY_DSN          // see optional step below
            options.isDebug = BuildConfig.DEBUG

            // Sample everything in debug, a small fraction in release.
            options.tracesSampleRate = if (BuildConfig.DEBUG) 1.0 else 0.2

            // Never attach IP / user identifiers automatically.
            options.isSendDefaultPii = false

            // Defensive scrub: drop user object and obvious PII before send.
            options.beforeSend = SentryOptions.BeforeSendCallback { event, _ ->
                event.user = null
                event.request = null
                event
            }
        }
    }
}
```

- `tracesSampleRate = 0.2` is a starting point — tune for quota.
- The `beforeSend` above is the minimum (drops the user object and request data). If you later add
  breadcrumbs that could contain notes/emails, extend it to redact message bodies.

### Optional: move the DSN to `BuildConfig`

The DSN is not a secret, but moving it matches the existing config pattern and lets you disable
Sentry per-build. In [`androidApp/build.gradle.kts`](apps/mobile/androidApp/build.gradle.kts),
alongside the other `buildConfigField` calls (~line 100):

```kotlin
val sentryDsn = providers.optionalBuildConfigValue(
    gradlePropertyName = "rangeworkSentryDsn",
    environmentVariableName = "RANGEWORK_SENTRY_DSN",
).ifEmpty { "https://c25ebd3aaf998399f242f58e3a7d5639@o4511601454284800.ingest.de.sentry.io/4511601459527760" }
buildConfigField("String", "SENTRY_DSN", sentryDsn.asBuildConfigString())
```

If you skip this, keep the DSN string literal inline in `RangeworkApplication.kt`.

### Verification

- `:androidApp:assembleDebug` compiles; app launches.
- Force a test event (`Sentry.captureMessage("test")`) and confirm in the Sentry dashboard that no
  `user`/IP is attached.
- Confirm release builds report `tracesSampleRate` 0.2 (visible in event payload / SDK debug log).

### Rollback

Single-file revert; no migration or persisted state involved.

---

## Finding #4 — WebView hardening (Low)

**Goal:** Keep navigation on first-party `rangework.app`, hand off-domain links to the system
browser, and disable file/content access.

**File:** [`WebViewScreen.kt`](apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/WebViewScreen.kt)

### Changes

```kotlin
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
// ...existing imports...

private const val ALLOWED_HOST_SUFFIX = "rangework.app"

// inside AndroidView factory = { context -> WebView(context).apply { ... } }
settings.javaScriptEnabled = true       // retained: legal pages use the theme toggle
settings.allowFileAccess = false
settings.allowContentAccess = false
settings.domStorageEnabled = false      // leave off unless a page needs it

webViewClient = object : WebViewClient() {
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean {
        val host = request.url.host ?: return true        // block hostless schemes
        val onDomain = host == ALLOWED_HOST_SUFFIX || host.endsWith(".$ALLOWED_HOST_SUFFIX")
        if (onDomain) return false                          // load in-app
        return try {                                        // off-domain -> system browser
            context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
            true
        } catch (_: ActivityNotFoundException) {
            true                                            // swallow; do not load in-app
        }
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) { isLoading = true }
    override fun onPageFinished(view: WebView, url: String?) { isLoading = false }
}
loadUrl(url)
```

- `url` is already restricted to the first-party allowlist (`legalPageUrls`), so this hardens the
  *secondary* navigation (links tapped inside a page) and removes file/content access.
- **JS decision:** the `ThemeToggle` on the site relies on JavaScript, so JS stays enabled. If you
  confirm the legal pages render acceptably without it, setting `javaScriptEnabled = false` is the
  stronger option — test all three pages (privacy-policy, terms-of-use, cookie-policy) first.

### Verification

- Open Settings → each legal page; confirm it loads in-app.
- Tap an external link within a page (e.g. an email/social link in the footer) → opens in the
  system browser, not inside the app WebView.
- `:androidApp:assembleDebug` + `:androidApp:lintDebug` clean.

### Rollback

Single-file revert.

---

## Finding #2 — Account-deletion audit scrub PII gap (Low)

**Goal:** Ensure that after account deletion, **no** audit rows containing the user's PII
(email/names, and ideally all their content) remain in `audit.events`.

**Root cause recap:** `scrub_user_audit_log` deletes `where actor_id = p_user_id` and runs
*before* `deleteUser`. The cascade `DELETE`s during `deleteUser` run in the service-role
connection where `auth.uid()` is `null`, so those events get `actor_id = null` and survive the
scrub. The `profiles` delete event's `old_values` carries email + names.

**Recommended approach:** (a) make `audit.log_change()` fall back to the row's **owner** when
`auth.uid()` is null, and (b) move the scrub to **after** `deleteUser`. Then a single owner-keyed
scrub removes both the user's activity events and the deletion-cascade events. This keeps the
current edge-function/admin-API architecture and needs no elevated `auth.users` delete privilege.

### Step 2a — New migration: owner-aware actor fallback

Create `supabase/migrations/20260622120000_audit_actor_owner_fallback.sql`:

```sql
-- Make the audit actor fall back to the row owner when auth.uid() is null
-- (e.g. service-role cascade deletes during account deletion). This lets the
-- account-deletion scrub remove deletion-generated events by the user's id.
create or replace function audit.log_change()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_pk_keys    text[];
  v_pk_key     text;
  v_old_row    jsonb;
  v_new_row    jsonb;
  v_source_row jsonb;
  v_row_pk     jsonb := '{}'::jsonb;
  v_row_id     uuid;
  v_new_diff   jsonb;
  v_old_diff   jsonb;
  v_actor      uuid;        -- NEW
  v_owner_txt  text;        -- NEW
begin
  if TG_NARGS > 0 then
    v_pk_keys := string_to_array(TG_ARGV[0], ',');
  else
    v_pk_keys := array['id'];
  end if;

  if TG_OP = 'DELETE' then
    v_old_row := to_jsonb(OLD);
    v_source_row := v_old_row;
  else
    v_new_row := to_jsonb(NEW);
    v_source_row := v_new_row;
    if TG_OP = 'UPDATE' then
      v_old_row := to_jsonb(OLD);
    end if;
  end if;

  -- NEW: resolve actor, falling back to the row's owner when there is no JWT actor.
  v_owner_txt := coalesce(
    v_source_row ->> 'owner_id',
    v_source_row ->> 'user_id',
    case when TG_TABLE_NAME = 'profiles' then v_source_row ->> 'id' end
  );
  begin
    v_actor := coalesce(auth.uid(), v_owner_txt::uuid);
  exception when others then
    v_actor := auth.uid();
  end;

  foreach v_pk_key in array v_pk_keys loop
    v_row_pk := v_row_pk || jsonb_build_object(v_pk_key, v_source_row -> v_pk_key);
  end loop;

  if array_length(v_pk_keys, 1) = 1 then
    begin
      v_row_id := (v_source_row ->> v_pk_keys[1])::uuid;
    exception when others then
      v_row_id := null;
    end;
  end if;

  if TG_OP = 'INSERT' then
    insert into audit.events (actor_id, table_name, action, row_id, row_pk, new_values)
    values (v_actor, TG_TABLE_NAME, 'INSERT', v_row_id, v_row_pk, v_new_row);
    return NEW;

  elsif TG_OP = 'DELETE' then
    insert into audit.events (actor_id, table_name, action, row_id, row_pk, old_values)
    values (v_actor, TG_TABLE_NAME, 'DELETE', v_row_id, v_row_pk, v_old_row);
    return OLD;

  else
    select coalesce(jsonb_object_agg(k, v_new_row -> k), '{}'::jsonb)
      into v_new_diff
    from jsonb_object_keys(v_new_row) as k
    where k not in ('updated_at', 'created_at')
      and (v_new_row -> k) is distinct from (v_old_row -> k);

    if v_new_diff = '{}'::jsonb then
      return NEW;
    end if;

    select coalesce(jsonb_object_agg(k, v_old_row -> k), '{}'::jsonb)
      into v_old_diff
    from jsonb_object_keys(v_new_diff) as k;

    insert into audit.events (actor_id, table_name, action, row_id, row_pk, old_values, new_values)
    values (v_actor, TG_TABLE_NAME, 'UPDATE', v_row_id, v_row_pk, v_old_diff, v_new_diff);

    return NEW;
  end if;
end;
$$;
```

> **Semantic note:** `actor_id` now means "who acted, or the data owner if no actor." For an audit
> log this is generally *more* useful (you always know whose data changed) and only changes the
> value in the previously-`null` case. If you prefer to keep `actor_id` strictly "the actor,"
> the alternative is to add a separate `owner_id` column to `audit.events` and scrub on that —
> more invasive (schema change + backfill).

### Step 2b — Move the scrub after deletion

**File:** [`delete-account/index.ts`](supabase/functions/delete-account/index.ts). Reorder so the
scrub runs after `deleteUser` (the `user.id` is already captured in a local before deletion):

```ts
// 1) delete the auth user (cascades to profiles + all owned data; audit triggers
//    now tag those events with the owner id via the fallback)
const { error: deleteError } = await adminClient.auth.admin.deleteUser(user.id);
if (deleteError) {
  console.error("user delete failed:", deleteError);
  return new Response(JSON.stringify({ error: "Account deletion failed" }), {
    status: 500, headers: { "Content-Type": "application/json" },
  });
}

// 2) scrub all audit rows for this user (their activity + the deletion-cascade rows)
const { error: scrubError } = await adminClient.rpc("scrub_user_audit_log", {
  p_user_id: user.id,
});
if (scrubError) {
  // User is already deleted; surface for retry/cleanup but do not fail silently.
  console.error("audit scrub failed (PII may remain in audit.events):", scrubError);
  return new Response(JSON.stringify({ error: "Account deletion partially failed" }), {
    status: 500, headers: { "Content-Type": "application/json" },
  });
}

return new Response(null, { status: 204 });
```

`scrub_user_audit_log` itself is unchanged (it already deletes `where actor_id = p_user_id`).

### Residual edge case + mitigation

If `deleteUser` succeeds but the scrub then fails, the user is gone but their audit PII lingers
until a retry. Because the token is now invalid, the client cannot simply retry the function.
Mitigation options (pick one):

- **Accept + monitor:** the 500 above logs to the function logs / Sentry; add a periodic sweep
  (scheduled SQL) that deletes `audit.events` rows whose `actor_id` no longer exists in
  `auth.users`. This is also a good belt-and-suspenders cleanup in general.
- **Fully atomic (alternative):** replace the GoTrue admin delete with a `security definer` RPC
  that deletes `auth.users` and scrubs `audit.events` in one transaction. ⚠️ **Verify** that the
  function owner can `delete from auth.users` in your Supabase project before choosing this — the
  admin API is the sanctioned path and direct deletes are version-sensitive.

### Verification

Test against a **branch/staging** Supabase project, not production:

1. Sign in as a throwaway user; create a unit + session (generates audit rows).
2. As a privileged DB user: `select count(*), actor_id from audit.events group by actor_id;` —
   note the user's rows.
3. Delete the account via the app (Settings → Delete account).
4. Re-query `audit.events`: **zero** rows for that user id, and **no** `profiles` row in
   `old_values` containing the email anywhere (`select * from audit.events where old_values::text
   ilike '%theemail@example.com%';` returns nothing).

### Rollback

Migrations are forward-only; ship a follow-up migration that restores the previous
`audit.log_change()` body (it is preserved in
[`20260621100000_audit_log_schema.sql`](supabase/migrations/20260621100000_audit_log_schema.sql)).
Revert the edge-function reorder with `git revert` and `supabase functions deploy delete-account`.

---

## Finding #1 — Google ID-token sign-in nonce (Medium)

**Goal:** Bind the Google ID token to this sign-in request with a nonce, defeating token replay.

**Flow:** generate a random `rawNonce`; pass `sha256hex(rawNonce)` to Google (it becomes the
`nonce` claim in the issued ID token); pass `rawNonce` to Supabase, which re-hashes and compares.
The value must thread provider → result → ViewModel → use case → repository.

> ⚠️ **Verify two things against supabase-kt 3.0.0 before/while implementing:**
> 1. The `IDToken` sign-in config exposes a `nonce` property (it has historically).
> 2. GoTrue expects the **raw** nonce and performs the SHA-256 comparison server-side (so the
>    hash you send Google must be SHA-256 hex of the same raw value).

### Step 1a — Generate + hash in the provider

**File:** [`AndroidGoogleIdTokenProvider.kt`](apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/auth/AndroidGoogleIdTokenProvider.kt)

```kotlin
import java.security.MessageDigest
import java.util.UUID

// add nonce to the success result
sealed interface GoogleIdTokenRequestResult {
    data class Success(val idToken: String, val nonce: String) : GoogleIdTokenRequestResult
    data class Cancelled(val message: String) : GoogleIdTokenRequestResult
    data class Failure(val message: String) : GoogleIdTokenRequestResult
}

// inside requestIdToken(), before building the option:
val rawNonce = UUID.randomUUID().toString()
val hashedNonce = MessageDigest.getInstance("SHA-256")
    .digest(rawNonce.toByteArray())
    .joinToString("") { "%02x".format(it) }

val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId)
    .setNonce(hashedNonce)
    .build()

// on success:
GoogleIdTokenRequestResult.Success(idToken = googleCredential.idToken, nonce = rawNonce)
```

### Step 1b — Thread through the shared layers

1. **[`AuthRepository`](apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/auth/AuthRepository.kt)** — add `nonce`:

   ```kotlin
   suspend fun signInWithGoogleIdToken(
       idToken: String,
       nonce: String? = null,
       accessToken: String? = null,
   ): AuthState
   ```

2. **[`SupabaseAuthRepository`](apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/auth/SupabaseAuthRepository.kt)** — pass it to the builder:

   ```kotlin
   override suspend fun signInWithGoogleIdToken(
       idToken: String,
       nonce: String?,
       accessToken: String?,
   ): AuthState {
       client.auth.signInWith(IDToken) {
           provider = Google
           this.idToken = idToken
           this.nonce = nonce                       // ⚠️ verify property name
           if (accessToken != null) this.accessToken = accessToken
       }
       return toAuthState(client.auth.sessionStatus.value)
   }
   ```

3. **[`SignInWithGoogleIdTokenUseCase`](apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/usecase/SignInWithGoogleIdTokenUseCase.kt)** — add `nonce`:

   ```kotlin
   suspend operator fun invoke(
       idToken: String,
       nonce: String? = null,
       accessToken: String? = null,
   ): AuthState = authRepository.signInWithGoogleIdToken(idToken, nonce, accessToken)
   ```

### Step 1c — Pass the nonce from the ViewModel

**File:** [`AuthViewModel.kt`](apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/AuthViewModel.kt) (~line 88)

```kotlin
is GoogleIdTokenRequestResult.Success -> {
    try {
        authStateMessage(
            foundation.signInWithGoogleIdTokenUseCase(
                idToken = tokenResult.idToken,
                nonce = tokenResult.nonce,
            ),
        )
    } catch (exception: IllegalStateException) { /* unchanged */ }
    // ...
}
```

### Tests

- Update any **fake `AuthRepository`** in `shared/src/commonTest` and
  `androidApp/src/test` to match the new signature (search:
  `grep -rn "signInWithGoogleIdToken" shared/src/commonTest androidApp/src/test`).
- Extend the auth use-case test to assert the nonce is forwarded to the repository.
- [`AuthViewModelTest`](apps/mobile/androidApp/src/test/java/com/loganmartlew/rangework/android/ui/AuthViewModelTest.kt):
  have the fake `GoogleIdTokenProvider` return `Success(idToken, nonce)` and assert it reaches the
  fake repository.

### Verification (requires a live build)

- **End-to-end sign-in must still succeed** on a real device against the real Supabase project — a
  nonce mismatch will *reject* sign-in, so this is the critical gate. Test: fresh install → Google
  sign-in → lands authenticated.
- Optionally decode the returned Google ID token (jwt.io) and confirm its `nonce` claim equals the
  SHA-256 hex you generated.

### Rollback

Revert the chain; the `nonce` params are nullable with defaults, so partial reverts compile, but
revert all layers together to avoid sending a hash to Google without telling Supabase (which would
break sign-in).

---

## Finding #3 — Encrypted session storage (Low)

**Goal:** Stop persisting access/refresh tokens in plaintext SharedPreferences; encrypt them at
rest with a key held in the Android Keystore.

**Architecture constraint (confirmed):** the Supabase client is built in the single factory
[`createRangeworkSupabaseClient`](apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/data/RangeworkSupabaseClientFactory.kt)
(`commonMain`), and `androidApp` does **not** see supabase-kt types (`shared` exposes them via
`implementation`, not `api`). Therefore the custom `SessionManager` **must live in
`shared/src/androidMain`**, and the factory selects it via `expect`/`actual`.

> ⚠️ **Verify** the supabase-kt 3.0.0 `SessionManager` interface shape before coding — method
> names/signatures vary by version. Expected (3.x):
> `suspend fun saveSession(session: UserSession)`, `suspend fun loadSession(): UserSession?`,
> `suspend fun deleteSession()`.

### Step 3a — Capture an app context

`shared/androidMain` needs a `Context` to reach the Keystore/SharedPreferences. Add a tiny holder
in **`shared/src/androidMain/kotlin/com/loganmartlew/rangework/shared/platform/RangeworkSessionContext.kt`**:

```kotlin
package com.loganmartlew.rangework.shared.platform

import android.content.Context

object RangeworkSessionContext {
    @Volatile var appContext: Context? = null
}
```

Set it once in
[`RangeworkApplication.onCreate`](apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/RangeworkApplication.kt)
(before the Sentry init is fine):

```kotlin
RangeworkSessionContext.appContext = applicationContext
```

`Application.onCreate` runs before any composable creates the client, so the holder is always set
in time.

### Step 3b — `expect`/`actual` session-manager provider

**commonMain** — new file
`shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/data/PlatformSessionManager.kt`:

```kotlin
package com.loganmartlew.rangework.shared.data

import io.github.jan.supabase.auth.SessionManager

/** Returns a platform-encrypted session manager, or null to use the supabase-kt default. */
internal expect fun rangeworkPlatformSessionManager(): SessionManager?
```

**androidMain** — new file
`shared/src/androidMain/kotlin/com/loganmartlew/rangework/shared/data/PlatformSessionManager.android.kt`:

```kotlin
package com.loganmartlew.rangework.shared.data

import com.loganmartlew.rangework.shared.platform.RangeworkSessionContext
import io.github.jan.supabase.auth.SessionManager

internal actual fun rangeworkPlatformSessionManager(): SessionManager? {
    val ctx = RangeworkSessionContext.appContext ?: return null   // fall back to default
    return EncryptedSessionManager(ctx)
}
```

### Step 3c — The encrypted `SessionManager`

Two options. **Option A** is faster; **Option B** avoids a deprecated dependency.

**Option A — `EncryptedSharedPreferences` (quickest).** Add to
[`shared/build.gradle.kts`](apps/mobile/shared/build.gradle.kts) `androidMain.dependencies`:

```kotlin
implementation("androidx.security:security-crypto:1.1.0-alpha06")
```

```kotlin
// shared/src/androidMain/.../data/EncryptedSessionManager.kt
class EncryptedSessionManager(context: Context) : SessionManager {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "rangework_secure_session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
    override suspend fun saveSession(session: UserSession) =
        prefs.edit().putString("session", Json.encodeToString(session)).apply()
    override suspend fun loadSession(): UserSession? =
        prefs.getString("session", null)?.let { Json.decodeFromString(it) }
    override suspend fun deleteSession() = prefs.edit().remove("session").apply()
}
```

> Note: `androidx.security:security-crypto` is in maintenance/deprecated status but still
> functional and widely used. Acceptable for a Low-severity fix; revisit if Google removes it.

**Option B — Android Keystore AES/GCM (durable, no deprecated dep).** No extra dependency. Generate
a `KeyGenParameterSpec` AES/GCM key in the `AndroidKeyStore`, encrypt the serialized `UserSession`
JSON, and store ciphertext + IV (base64) in a normal `SharedPreferences`. ~60 lines; standard
Keystore boilerplate. Prefer this if you want to avoid the deprecation.

Either way, `UserSession` is `@Serializable`, so `kotlinx.serialization` handles (de)serialization.

### Step 3d — Install it in the factory

[`RangeworkSupabaseClientFactory.kt`](apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/data/RangeworkSupabaseClientFactory.kt):

```kotlin
import io.github.jan.supabase.auth.Auth

return createSupabaseClient(supabaseUrl = config.projectUrl, supabaseKey = config.anonKey) {
    install(Auth) {
        rangeworkPlatformSessionManager()?.let { sessionManager = it }
    }
    install(Postgrest)
    install(Functions)
}
```

Because every client routes through this factory, the encrypted manager applies to all of them
(the app shares one client via `createRangeworkFoundation`, plus any created by `AuthViewModel`'s
default).

### Migration of existing sessions

Existing installs have a plaintext session under supabase-kt's default key; the new manager uses a
new store, so **current users will be signed out once** on upgrade (they re-auth via Google — low
friction). If you want to avoid that, do a one-time read of the old default store and re-save into
the encrypted one on first run. Given Low severity, a single forced re-login is acceptable; note it
in the release notes.

### Tests / verification

- `commonTest` stays green (the `expect` returns `null` off-Android, so tests use the default
  manager — no Android deps leak into common tests).
- On device: sign in, kill + relaunch the app → session restores (proves encrypt/decrypt round
  trip). Inspect `adb shell run-as <pkg>` shared_prefs → the session value is ciphertext, not a
  readable JWT.
- Run the full Gradle validation + lint for both `:shared` and `:androidApp`.

### Rollback

Remove the `install(Auth){ sessionManager = ... }` line (factory falls back to default), or have
`rangeworkPlatformSessionManager()` return `null`. Users are signed out once more on rollback.

---

## Cross-cutting: testing & rollout checklist

- [ ] Each fix on its own branch/commit; run the full Gradle test + lint matrix per `CLAUDE.md`.
- [ ] DB/edge changes (#2) tested against a **non-production** Supabase project first; apply via
      `supabase db push` and `supabase functions deploy delete-account`.
- [ ] Live-device pass for **#1** (sign-in still works) and **#3** (session restores; storage is
      ciphertext) — these can't be proven by unit tests alone.
- [ ] Update fakes/tests touched by the `AuthRepository` signature change (#1).
- [ ] Confirm no new secrets/PII introduced; `beforeSend` scrub verified in a real Sentry event.
- [ ] Update `README.md` only if you added the optional `RANGEWORK_SENTRY_DSN` build input (#6).

## Out of scope (tracked separately)

Findings **7** (bump `esbuild` ≥ 0.28.1 in `apps/site`), **8** (pin GitHub Actions to commit SHAs),
and **9** (audit-log retention/field minimization) are not covered here — see the review doc. Note
that fix **#2** already reduces lingering PII relevant to #9.
