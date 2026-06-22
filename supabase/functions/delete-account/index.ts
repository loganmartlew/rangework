import { createClient } from 'jsr:@supabase/supabase-js@2';

Deno.serve(async (req: Request) => {
  if (req.method !== 'POST') {
    return new Response(null, { status: 405 });
  }

  const authHeader = req.headers.get('Authorization');
  if (!authHeader) {
    return new Response(JSON.stringify({ error: 'Unauthorized' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json' },
    });
  }

  const userClient = createClient(
    Deno.env.get('SUPABASE_URL')!,
    Deno.env.get('SUPABASE_ANON_KEY')!,
    { global: { headers: { Authorization: authHeader } } },
  );

  const {
    data: { user },
    error: userError,
  } = await userClient.auth.getUser();
  if (userError || !user) {
    return new Response(JSON.stringify({ error: 'Unauthorized' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json' },
    });
  }

  const adminClient = createClient(
    Deno.env.get('SUPABASE_URL')!,
    Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!,
    { auth: { autoRefreshToken: false, persistSession: false } },
  );

  // 1) delete the auth user (cascades to profiles + all owned data; audit triggers
  //    now tag those events with the owner id via the fallback)
  const { error: deleteError } = await adminClient.auth.admin.deleteUser(
    user.id,
  );
  if (deleteError) {
    console.error('user delete failed:', deleteError);
    return new Response(JSON.stringify({ error: 'Account deletion failed' }), {
      status: 500,
      headers: { 'Content-Type': 'application/json' },
    });
  }

  // 2) scrub all audit rows for this user (their activity + the deletion-cascade rows)
  const { error: scrubError } = await adminClient.rpc('scrub_user_audit_log', {
    p_user_id: user.id,
  });
  if (scrubError) {
    // User is already deleted; surface for retry/cleanup but do not fail silently.
    console.error(
      'audit scrub failed (PII may remain in audit.events):',
      scrubError,
    );
    return new Response(
      JSON.stringify({ error: 'Account deletion partially failed' }),
      {
        status: 500,
        headers: { 'Content-Type': 'application/json' },
      },
    );
  }

  return new Response(null, { status: 204 });
});
