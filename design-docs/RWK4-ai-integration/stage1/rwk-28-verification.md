# RWK-28 — Supabase OAuth 2.1 Configuration — Verification Checklist

> **Epic:** [RWK-4 — AI Session Creation](https://loganmartlew.atlassian.net/browse/RWK-4)
> **Ticket:** [RWK-28 — Configure Supabase OAuth 2.1 Server](https://loganmartlew.atlassian.net/browse/RWK-28)
> **Status:** Ready for user execution

---

## Overview

RWK-28 is **user-owned dashboard work**. The agent does not touch the Supabase dashboard. This checklist contains the dashboard configuration instructions and verification steps the user executes in the Supabase project dashboard (Authentication → OAuth Server).

**Prerequisite:** The consent stub page at `apps/site/src/pages/oauth/consent.astro` must be deployed to `https://rangework.app/oauth/consent` before applying the authorization-path setting. Verify the stub is live first.

---

## Step 1: Deploy the consent stub page

Before configuring the Supabase dashboard, ensure the consent stub page is deployed:

```powershell
pnpm --filter @rangework/site build
# Deploy to your hosting provider (e.g. Cloudflare Pages, Vercel, Netlify)
```

Verify the page is reachable:

```powershell
curl -I https://rangework.app/oauth/consent
```

Expected: HTTP 200.

---

## Step 2: Configure Supabase OAuth 2.1 server

Navigate to the Supabase project dashboard → **Authentication** → **OAuth Server** (or **OAuth 2.1 Server** if listed separately).

Apply the following settings:

| Setting                     | Value                                 | Notes                                                                                                          |
| --------------------------- | ------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| OAuth 2.1 server            | **Enabled**                           | Beta feature — accept known risk (flag F11)                                                                    |
| JWT signing algorithm       | **ES256**                             | Supabase's default asymmetric algorithm                                                                        |
| Dynamic client registration | **Enabled (fully open)**              | No allowlist — lowest friction for RWK-34 testing; acceptable security posture with no active production users |
| Authorization path          | `https://rangework.app/oauth/consent` | Points at the stub page shipped in Step 1; RWK-33 replaces it with the full consent page                       |

Save the configuration.

---

## Step 3: Verify the discovery endpoint

The discovery endpoint advertises the OAuth 2.1 server's capabilities.

```powershell
curl https://<project-ref>.supabase.co/.well-known/oauth-authorization-server/auth/v1
```

Replace `<project-ref>` with your Supabase project reference (found in the dashboard URL or project settings).

**Expected response:** Valid JSON with the following fields present:

- `authorization_endpoint`
- `token_endpoint`
- `jwks_uri`
- `registration_endpoint`

**Record the full response** for reference by RWK-30 (token validation implementation).

---

## Step 4: Verify the JWKS endpoint

The JWKS endpoint returns the public keys used to verify JWT signatures.

Extract the `jwks_uri` from the discovery response (Step 3), then fetch it:

```powershell
curl <jwks_uri>
```

**Expected response:** Valid JWKS JSON with a `keys` array containing at least one ES256 key.

Example:

```json
{
  "keys": [
    {
      "kty": "EC",
      "alg": "ES256",
      "crv": "P-256",
      "kid": "...",
      "use": "sig",
      "x": "...",
      "y": "..."
    }
  ]
}
```

---

## Step 5: Verify dynamic client registration

The registration endpoint allows OAuth clients to register dynamically.

Extract the `registration_endpoint` from the discovery response (Step 3), then POST a client metadata payload:

```powershell
curl -X POST <registration_endpoint> \
  -H "Content-Type: application/json" \
  -d '{
    "client_name": "RWK-28 Test Client",
    "redirect_uris": ["https://example.com/callback"],
    "grant_types": ["authorization_code"],
    "response_types": ["code"],
    "token_endpoint_auth_method": "none"
  }'
```

**Expected response:** HTTP 201 Created with a JSON body containing a `client_id`.

Example:

```json
{
  "client_id": "abc123...",
  "client_name": "RWK-28 Test Client",
  ...
}
```

**Note:** This is a test registration. The client is not used in Stage 1. RWK-34 will cover end-to-end OAuth flows.

---

## Step 6: Verify Android sign-in (email)

If the Android app can be built and run during Stage 1:

1. Build and install the Android app.
2. Sign in with email/password.
3. Verify sign-in succeeds and the app loads practice data.

**If the Android app cannot be built** (e.g. missing Gradle properties or Supabase credentials), document which steps were skipped and note that RWK-34 will cover full end-to-end verification.

---

## Step 7: Verify Android sign-in (Google)

If the Android app can be built and run during Stage 1:

1. Sign in with Google OAuth.
2. Verify sign-in succeeds and the app loads practice data.

**If the Android app cannot be built**, document which steps were skipped and note that RWK-34 will cover full end-to-end verification.

---

## Step 8: Verify the consent URL is reachable

Confirm the consent stub page is live:

```powershell
curl -I https://rangework.app/oauth/consent
```

**Expected:** HTTP 200.

---

## Step 9: Record the discovery response

Save the full discovery endpoint response (Step 3) to a file for reference by RWK-30:

```powershell
curl https://<project-ref>.supabase.co/.well-known/oauth-authorization-server/auth/v1 > discovery-response.json
```

Attach `discovery-response.json` to the RWK-28 ticket or store it in `design-docs/RWK4-ai-integration/stage1/` for RWK-30 implementation.

---

## Acceptance criteria

- [ ] Consent stub page deployed and reachablE at `https://rangework.app/oauth/consent` (HTTP 200).
- [ ] Supabase OAuth 2.1 server enabled with RS256 and open DCR.
- [ ] Authorization path set to `https://rangework.app/oauth/consent`.
- [ ] Discovery endpoint returns valid JSON with `authorization_endpoint`, `token_endpoint`, `jwks_uri`, and `registration_endpoint`.
- [ ] JWKS endpoint returns ES256 public keys.
- [ ] Dynamic client registration accepts a test payload and returns a `client_id`.
- [ ] Android sign-in (email) verified **OR** explicitly noted as deferred to RWK-34.
- [ ] Android sign-in (Google) verified **OR** explicitly noted as deferred to RWK-34.
- [ ] Discovery response recorded and saved for RWK-30 reference.

---

## Notes

- **ES256 key rotation:** Supabase manages token issuance and rotation at the dashboard level. Existing tokens are expected to continue working through Supabase's key rotation mechanism. If Android sign-in fails post-algorithm-switch, revert the dashboard setting and document the issue.
- **Open DCR on production:** Acceptable with no active users. Revisit before production scale.
- **Supabase OAuth 2.1 server is beta (F11):** Pin behavior during RWK-28; monitor Supabase changelog for API changes that could affect RWK-30/33.
- **Android app cannot be built:** Document skipped steps in the checklist; defer to RWK-34.

---

## Sign-off

**User:** ****\*\*\*\*****\_\_\_****\*\*\*\*****  
**Date:** ****\*\*\*\*****\_\_\_****\*\*\*\*****

**Verification completed:** [ ] Yes [ ] No (partial — see notes above)

**Notes:**

<!-- Add any observations, issues, or deferred items here -->
