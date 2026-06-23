import { supabase } from '../../lib/supabase-client.js';
import type { Session } from '@supabase/supabase-js';
import type { ErrorKind } from './types.js';

export type ConsentState =
  | { kind: 'loading' }
  | { kind: 'consent'; clientName: string; authorizationId: string }
  | { kind: 'redirect'; url: string }
  | { kind: 'error'; error: ErrorKind };

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

    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((event, session) => {
      if (event === 'SIGNED_IN') {
        settle(session);
      } else if (event === 'INITIAL_SESSION') {
        if (session || !new URLSearchParams(window.location.search).has('code')) {
          settle(session);
        }
      }
    });

    setTimeout(() => settle(null), 10_000);
  });
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function getOAuthClient(): any {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return (supabase.auth as any).oauth;
}

export async function resolveConsentState(): Promise<ConsentState> {
  const params = new URLSearchParams(window.location.search);
  const authorizationId = params.get('authorization_id');

  if (!authorizationId) {
    return { kind: 'error', error: 'invalid' };
  }

  const session = await resolveInitialSession();

  // Strip the PKCE `code` param from the URL after exchange
  if (params.has('code')) {
    params.delete('code');
    const cleanUrl = `${window.location.pathname}?${params.toString()}`;
    window.history.replaceState(null, '', cleanUrl);
  }

  if (!session) {
    const redirectTo = `${window.location.origin}/oauth/consent?authorization_id=${encodeURIComponent(authorizationId)}`;
    const { error } = await supabase.auth.signInWithOAuth({
      provider: 'google',
      options: { redirectTo },
    });
    if (error) {
      return { kind: 'error', error: 'auth-failed' };
    }
    return { kind: 'loading' };
  }

  try {
    const oauthClient = getOAuthClient();
    const { data, error } = await oauthClient.getAuthorizationDetails(authorizationId);

    if (error) {
      const msg = error?.message?.toLowerCase() ?? '';
      const isExpired = msg.includes('expir') || msg.includes('not found') || msg.includes('session');
      return { kind: 'error', error: isExpired ? 'expired' : 'network' };
    }

    // If the response contains only redirect_url, consent was already granted
    // previously — skip the UI and redirect directly.
    if (data?.redirect_url && !data?.clientName && !data?.client_name) {
      return { kind: 'redirect', url: data.redirect_url };
    }

    const clientName = data?.clientName ?? data?.client_name ?? 'Unknown app';
    return { kind: 'consent', clientName, authorizationId };
  } catch {
    return { kind: 'error', error: 'network' };
  }
}

export async function submitDecision(
  authorizationId: string,
  decision: 'approve' | 'deny',
): Promise<{ redirect_url: string } | { error: ErrorKind }> {
  try {
    const oauthClient = getOAuthClient();
    const { data, error } =
      decision === 'approve'
        ? await oauthClient.approveAuthorization(authorizationId, { skipBrowserRedirect: true })
        : await oauthClient.denyAuthorization(authorizationId, { skipBrowserRedirect: true });

    if (error || !data?.redirect_url) {
      const msg = error?.message?.toLowerCase() ?? '';
      if (msg.includes('no longer pending') || msg.includes('expir')) {
        return { error: 'expired' };
      }
      return { error: 'network' };
    }

    return { redirect_url: data.redirect_url };
  } catch {
    return { error: 'network' };
  }
}

export function getErrorMessage(kind: ErrorKind): string {
  switch (kind) {
    case 'invalid':
      return 'This link is invalid. Please try connecting again from your app.';
    case 'expired':
      return 'This authorization request has expired. Please reconnect from your app.';
    case 'network':
      return 'Something went wrong. Please try again.';
    case 'auth-failed':
      return 'Could not sign in. Please try again.';
  }
}
