<script lang="ts">
  import { onMount } from 'svelte';
  import { api } from '$lib/api/client.js';
  import type { Motor } from '$lib/api/types.js';

  let motors = $state<Motor[]>([]);
  let stubNote = $state<string | null>(null);
  let error = $state<string | null>(null);
  let impulseClass = $state('');

  async function load() {
    try {
      const r = await api.motors(impulseClass ? { class: impulseClass } : undefined);
      motors = r.motors;
      stubNote = r.stub ? r.note ?? null : null;
    } catch (e) {
      error = e instanceof Error ? e.message : String(e);
    }
  }

  onMount(load);
</script>

<div class="motors">
  <header>
    <h2>Motors</h2>
    <select bind:value={impulseClass} onchange={load}>
      <option value="">All impulse classes</option>
      {#each ['A','B','C','D','E','F','G','H','I','J','K','L','M','N','O'] as c}
        <option value={c}>{c}</option>
      {/each}
    </select>
  </header>

  {#if stubNote}
    <div class="warn">⚠ Stub data — {stubNote}</div>
  {/if}
  {#if error}
    <div class="error">{error}</div>
  {/if}

  <table>
    <thead>
      <tr>
        <th>ID</th><th>Mfg</th><th>Designation</th><th>Class</th>
        <th>Total Impulse (Ns)</th><th>Avg Thrust (N)</th><th>Burn (s)</th><th>Ø (mm)</th>
      </tr>
    </thead>
    <tbody>
      {#each motors as m}
        <tr>
          <td>{m.id}</td>
          <td>{m.manufacturer}</td>
          <td>{m.designation}</td>
          <td>{m.impulseClass}</td>
          <td>{m.totalImpulseNs.toFixed(1)}</td>
          <td>{m.averageThrustN.toFixed(1)}</td>
          <td>{m.burnTimeS.toFixed(2)}</td>
          <td>{(m.diameter * 1000).toFixed(0)}</td>
        </tr>
      {/each}
    </tbody>
  </table>
</div>

<style>
  .motors { padding: 20px; }
  header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
  h2 { font-size: 16px; margin: 0; }
  select { background: var(--panel); color: var(--text); border: 1px solid var(--border); padding: 6px 10px; border-radius: 4px; }
  .warn { color: #f0c674; background: rgba(240, 198, 116, 0.1); padding: 8px 12px; border-radius: 4px; margin-bottom: 12px; font-size: 13px; }
  .error { color: #ff8866; font-size: 12px; margin-bottom: 12px; }
  table { width: 100%; border-collapse: collapse; font-size: 13px; }
  th, td { padding: 8px 12px; text-align: left; border-bottom: 1px solid var(--border); }
  th { color: var(--muted); font-weight: 500; text-transform: uppercase; font-size: 11px; letter-spacing: 0.4px; }
  tr:hover td { background: rgba(74, 144, 226, 0.05); }
</style>
