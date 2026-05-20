<script lang="ts">
  import { page } from '$app/stores';
  let { children } = $props();
  const tabs = [
    { href: '/', label: 'Designer' },
    { href: '/motors', label: 'Motors' },
    { href: '/simulate', label: 'Simulate' },
    { href: '/export', label: 'Export' }
  ];
</script>

<div class="app">
  <header>
    <h1>OpenRocketX</h1>
    <nav>
      {#each tabs as t}
        <a href={t.href} class:active={$page.url.pathname === t.href}>{t.label}</a>
      {/each}
    </nav>
  </header>
  <main>
    {@render children()}
  </main>
</div>

<style>
  :global(:root) {
    --bg: #0e1116;
    --panel: #161b22;
    --text: #c7d1de;
    --muted: #8b949e;
    --accent: #4a90e2;
    --accent-text: #ffffff;
    --border: #30363d;
  }
  :global(body) {
    margin: 0;
    background: var(--bg);
    color: var(--text);
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif;
    font-size: 14px;
  }
  .app { display: flex; flex-direction: column; height: 100vh; }
  header {
    display: flex; align-items: center; gap: 24px; padding: 12px 20px;
    background: var(--panel); border-bottom: 1px solid var(--border);
  }
  h1 { font-size: 16px; margin: 0; font-weight: 600; letter-spacing: 0.4px; }
  nav { display: flex; gap: 4px; }
  nav a {
    color: var(--muted); text-decoration: none; padding: 6px 12px;
    border-radius: 6px; font-size: 13px;
  }
  nav a:hover { color: var(--text); background: rgba(74, 144, 226, 0.1); }
  nav a.active { color: var(--accent-text); background: var(--accent); }
  main { flex: 1; overflow: auto; }
</style>
