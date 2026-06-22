import { supabase } from '../../lib/supabase-client.js';
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
    if (el) el.hidden = s !== state;
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

async function mountConsentIsland() {
  setState('loading');

  const authorizationId = new URLSearchParams(window.location.search).get('authorization_id');

  if (!authorizationId) {
    setError('invalid');
    return;
  }

  // Check for an existing Supabase session
  const { data: sessionData, error: sessionError } = await supabase.auth.getSession();

  if (sessionError || !sessionData.session) {
    // Redirect to Google sign-in, preserving authorization_id for the return trip
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
