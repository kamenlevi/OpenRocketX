<script lang="ts">
  import { onMount } from 'svelte';
  import uPlot from 'uplot';
  import 'uplot/dist/uPlot.min.css';
  import { api } from '$lib/api/client.js';
  import { exampleRocket } from '$lib/examples.js';
  import type { SimSample } from '$lib/api/types.js';

  let chartEl: HTMLDivElement;
  let plot: uPlot | null = null;
  let ts: number[] = [];
  let alt: number[] = [];
  let spd: number[] = [];
  let acc: number[] = [];
  let stopStream: (() => void) | null = null;
  let running = $state(false);
  let summary = $state<{ apogeeM?: number; maxVelMs?: number; flightTimeS?: number } | null>(null);

  function makePlot() {
    plot = new uPlot(
      {
        width: chartEl.clientWidth,
        height: 360,
        scales: { x: { time: false } },
        series: [
          { label: 't (s)' },
          { label: 'Altitude (m)', stroke: '#4a90e2', width: 2 },
          { label: 'Speed (m/s)',  stroke: '#7bc97a', width: 2, scale: 'v' },
          { label: 'Accel (m/s²)', stroke: '#f0c674', width: 2, scale: 'a' }
        ],
        axes: [
          { label: 't (s)' },
          { scale: 'y', label: 'Altitude (m)' },
          { scale: 'v', label: 'Speed (m/s)', side: 1 },
          { scale: 'a', label: 'Accel', side: 1, grid: { show: false } }
        ]
      },
      [ts, alt, spd, acc],
      chartEl
    );
  }

  function pushSample(s: SimSample) {
    ts.push(s.t);
    alt.push(s.altitude);
    spd.push(s.speed);
    acc.push(s.acceleration);
    plot?.setData([ts, alt, spd, acc]);
  }

  async function start() {
    if (running) return;
    running = true;
    ts.length = 0; alt.length = 0; spd.length = 0; acc.length = 0;
    summary = null;
    plot?.setData([ts, alt, spd, acc]);

    stopStream = api.simulateStream(
      (m) => {
        if ('done' in m) {
          running = false;
          summary = {
            apogeeM: Math.max(...alt),
            maxVelMs: Math.max(...spd),
            flightTimeS: ts[ts.length - 1]
          };
        } else {
          pushSample(m);
        }
      },
      () => { running = false; }
    );
  }

  function stop() {
    stopStream?.();
    running = false;
  }

  async function runSync() {
    const r = await api.simulate(exampleRocket());
    ts.length = 0; alt.length = 0; spd.length = 0; acc.length = 0;
    for (const s of r.samples) pushSample(s);
    summary = r.summary;
  }

  onMount(() => {
    makePlot();
    runSync();
  });
</script>

<div class="simulate">
  <header>
    <h2>Flight simulation</h2>
    <div class="controls">
      <button onclick={start} disabled={running}>{running ? 'Streaming…' : 'Stream live'}</button>
      <button onclick={stop} disabled={!running}>Stop</button>
      <button onclick={runSync} disabled={running}>Run synchronous</button>
    </div>
  </header>

  {#if summary}
    <div class="summary">
      <div>Apogee: <strong>{summary.apogeeM?.toFixed(1)} m</strong></div>
      <div>Max velocity: <strong>{summary.maxVelMs?.toFixed(1)} m/s</strong></div>
      <div>Flight time: <strong>{summary.flightTimeS?.toFixed(1)} s</strong></div>
    </div>
  {/if}

  <div class="chart" bind:this={chartEl}></div>

  <p class="note">
    Curves are computed by the Java engine (stub model until the OpenRocket 6-DOF
    simulator is wired in via x.bridge). The streaming WebSocket pushes one sample
    per integration step.
  </p>
</div>

<style>
  .simulate { padding: 20px; }
  header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
  h2 { font-size: 16px; margin: 0; }
  .controls { display: flex; gap: 8px; }
  button {
    background: var(--accent); color: var(--accent-text); border: 0;
    padding: 8px 14px; border-radius: 4px; cursor: pointer; font-size: 13px;
  }
  button:hover:not(:disabled) { filter: brightness(1.1); }
  button:disabled { opacity: 0.5; cursor: not-allowed; }
  .summary {
    display: flex; gap: 24px; margin-bottom: 16px;
    color: var(--muted); font-size: 13px;
  }
  .summary strong { color: var(--text); }
  .chart { background: var(--panel); padding: 12px; border-radius: 6px; }
  .note { color: var(--muted); font-size: 12px; margin-top: 16px; max-width: 60ch; }
  :global(.uplot .u-legend) { color: var(--text) !important; }
</style>
