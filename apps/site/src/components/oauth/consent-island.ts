import { supabase } from '../../lib/supabase-client.js';
import type { Session } from '@supabase/supabase-js';
import type { AuthorizationDetails, ErrorKind } from './types.js';

// Type shim for the beta supabase.auth.oauth server methods
interface OAuthServerClient {
  getAuthorizationDetails: (authorizationId: string) => Promise<{
    data: { clientName: string; scopes: string } | null;
    error: { message: string; status?: number } | null;
  }>;
  approveAuthorization: (authorizationId: string) => Promise<{
    data: { redirect_url: string } | null;
    error: { message: string } | null;
  }>;
  denyAuthorization: (authorizationId: string) => Promise<{
    data: { redirect_url: string } | null;
    error: { message: string } | null;
  }>;
}

function getOAuthServerClient(): OAuthServerClient {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return (supabase.auth as any).oauth as OAuthServerClient;
}

function setState(state: 'loading' | 'consent' | 'error') {
  for (const s of ['loading', 'consent', 'error']) {
    const el = document.getElementById(`state-${s}`);
    if (el) {
      el.hidden = s !== state;
      if (s === 'loading') {
        (el as HTMLElement).setAttribute('aria-busy', s === state ? 'true' : 'false');
      }
    }
  }
}

function setError(kind: ErrorKind) {
  const msgEl = document.getElementById('error-message');
  if (msgEl) {
    switch (kind) {
      case 'invalid':
        msgEl.textContent = 'This link is invalid. Please try connecting again from your app.';
        break;
      case 'expired':
        msgEl.textContent =
          'This authorization request has expired. Please reconnect from your app.';
        break;
      case 'network':
        msgEl.textContent = 'Something went wrong. Please try again.';
        break;
      case 'auth-failed':
        msgEl.textContent = 'Could not sign in. Please try again.';
        break;
    }
  }

  const retryEl = document.getElementById('btn-retry');
  if (retryEl) {
    (retryEl as HTMLButtonElement).hidden = kind !== 'network' && kind !== 'auth-failed';
  }

  setState('error');
}

/**
 * Resolves the current Supabase session, waiting for any in-flight PKCE code
 * exchange to complete before returning.
 *
 * After a Google OAuth redirect, the Supabase client must make an async network
 * call to exchange the `?code=` parameter for tokens. Calling `getSession()`
 * synchronously before that exchange completes returns null, which would
 * mistakenly trigger another OAuth redirect and create an infinite loop.
 *
 * Using `onAuthStateChange` ensures we wait for either:
 *   - `INITIAL_SESSION`: an existing session was found in storage
 *   - `SIGNED_IN`: a new session was established (e.g. after PKCE exchange)
 *
 * A safety timeout of 10s resolves with null if no auth event fires.
 */
function resolveInitialSession(): Promise<Session | null> {
  return new Promise((resolve) => {
    let settled = false;

    const settle = (session: Session | null) => {
      if (settled) return;
      settled = true;
      subscription.unsubscribe();
      resolve(session);
    };

    const { data: { subscription } } = supabase.auth.onAuthStateChange((event, session) => {
      if (event === 'SIGNED_IN') {
        // Always settle on an explicit sign-in (covers PKCE code exchange completing).
        settle(session);
      } else if (event === 'INITIAL_SESSION') {
        // Settle immediately only if a session already exists. If session is null
        // and there is a PKCE code in the URL, the exchange is still in-flight —
        // wait for the subsequent SIGNED_IN event instead of racing ahead.
        if (session || !new URLSearchParams(window.location.search).has('code')) {
          settle(session);
        }
      }
    });

    setTimeout(() => settle(null), 10_000);
  });
}

async function mountConsentIsland() {
  setState('loading');

  const authorizationId = new URLSearchParams(window.location.search).get('authorization_id');

  if (!authorizationId) {
    setError('invalid');
    return;
  }

  // Wait for the initial auth state — this handles the PKCE code-exchange race
  // where getSession() would return null while the exchange is still in flight.
  const session = await resolveInitialSession();

  if (!session) {
    // No session — redirect to Google sign-in, preserving authorization_id for return
    const redirectTo = `${window.location.origin}/oauth/consent?authorization_id=${encodeURIComponent(authorizationId)}`;
    const { error } = await supabase.auth.signInWithOAuth({
      provider: 'google',
      options: { redirectTo },
    });
    if (error) {
      setError('auth-failed');
    }
    // If sign-in initiated successfully, the redirect is in progress — nothing more to do
    return;
  }

  // Session exists — fetch authorization details
  try {
    const oauthClient = getOAuthServerClient();
    const { data, error } = await oauthClient.getAuthorizationDetails(authorizationId);

    if (error || !data) {
      const msg = error?.message?.toLowerCase() ?? '';
      const isExpired =
        msg.includes('expir') || error?.status === 410 || error?.status === 404;
      setError(isExpired ? 'expired' : 'network');
      return;
    }

    showConsent(data, authorizationId);
  } catch {
    setError('network');
  }
}

function showConsent(details: AuthorizationDetails, authorizationId: string) {
  const clientNameEl = document.getElementById('client-name');
  if (clientNameEl) clientNameEl.textContent = details.clientName;

  const clientNameDescEl = document.getElementById('client-name-desc');
  if (clientNameDescEl) clientNameDescEl.textContent = details.clientName;

  setState('consent');

  document.getElementById('btn-approve')?.addEventListener('click', () => handleApprove(authorizationId));
  document.getElementById('btn-deny')?.addEventListener('click', () => handleDeny(authorizationId));
}

async function handleApprove(authorizationId: string) {
  setConsentButtonsDisabled(true);
  try {
    const { data, error } = await getOAuthServerClient().approveAuthorization(authorizationId);
    if (error || !data?.redirect_url) {
      setConsentButtonsDisabled(false);
      setError('network');
      return;
    }
    window.location.replace(data.redirect_url);
  } catch {
    setConsentButtonsDisabled(false);
    setError('network');
  }
}

async function handleDeny(authorizationId: string) {
  setConsentButtonsDisabled(true);
  try {
    const { data, error } = await getOAuthServerClient().denyAuthorization(authorizationId);
    if (error || !data?.redirect_url) {
      setConsentButtonsDisabled(false);
      setError('network');
      return;
    }
    window.location.replace(data.redirect_url);
  } catch {
    setConsentButtonsDisabled(false);
    setError('network');
  }
}

function setConsentButtonsDisabled(disabled: boolean) {
  document.querySelectorAll<HTMLButtonElement>('#state-consent button').forEach((btn) => {
    btn.disabled = disabled;
  });
}

document.addEventListener('DOMContentLoaded', () => {
  mountConsentIsland();

  document.getElementById('btn-retry')?.addEventListener('click', () => {
    mountConsentIsland();
  });
});
