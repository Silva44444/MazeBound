// Mazebound Supabase browser configuration.
// The Project URL and Publishable key are intended for browser use.
// NEVER put a Supabase Secret key or service_role key in this file.

(() => {
  const SUPABASE_URL = "PASTE_YOUR_PROJECT_URL_HERE";
  const SUPABASE_PUBLISHABLE_KEY = "PASTE_YOUR_PUBLISHABLE_KEY_HERE";

  const isConfigured =
    SUPABASE_URL.startsWith("https://") &&
    !SUPABASE_URL.includes("PASTE_YOUR") &&
    !SUPABASE_PUBLISHABLE_KEY.includes("PASTE_YOUR");

  if (!isConfigured) {
    console.info("Mazebound: add your Supabase Project URL and Publishable key in supabase-config.js.");
    return;
  }

  if (!window.supabase?.createClient) {
    console.error("Mazebound: Supabase library did not load.");
    return;
  }

  window.mazeboundSupabase = window.supabase.createClient(
    SUPABASE_URL,
    SUPABASE_PUBLISHABLE_KEY
  );
})();
