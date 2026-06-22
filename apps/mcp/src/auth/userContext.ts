import { createClient, type SupabaseClient } from '@supabase/supabase-js';

export interface UserContext {
  userId: string;
  supabaseClient: SupabaseClient;
}

export function createUserContext(
  userId: string,
  userToken: string,
  supabaseUrl: string,
  supabaseAnonKey: string,
): UserContext {
  const supabaseClient = createClient(supabaseUrl, supabaseAnonKey, {
    global: {
      headers: {
        Authorization: `Bearer ${userToken}`,
      },
    },
    auth: {
      persistSession: false,
      autoRefreshToken: false,
      detectSessionInUrl: false,
    },
  });

  return { userId, supabaseClient };
}
