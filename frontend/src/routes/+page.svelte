<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { api } from '$lib/api/client.js';
  import { RocketScene } from '$lib/three/RocketScene.js';
  import { exampleRocket } from '$lib/examples.js';
  import type { RocketSpec, ShapeType } from '$lib/api/types.js';

  let canvas: HTMLCanvasElement;
  let scene: RocketScene | null = null;
  let design = $state<RocketSpec>(exampleRocket());
  let stats = $state<{ vertices: number; triangles: number; manifold: boolean } | null>(null);
  let error = $state<string | null>(null);
  let showWire = $state(false);

  const shapes: ShapeType[] = ['OGIVE', 'CONICAL', 'ELLIPSOID', 'PARABOLIC', 'POWER', 'HAACK'];

  async function refresh() {
    error = null;
    try {
      const mesh = await api.preview(design);
      scene?.setMesh(mesh);
      stats = {
        vertices: mesh.positions.length / 3,
        triangles: mesh.indices.length / 3,
        manifold: mesh.manifold
      };
    } catch (e) {
      error = e instanceof Error ? e.message : String(e);
    }
  }

  onMount(() => {
    scene = new RocketScene(canvas);
    refresh();
  });

  onDestroy(() => scene?.dispose());

  $effect(() => {
    scene?.showWireframe(showWire);
  });

  function updateNose(field: 'length' | 'baseRadius' | 'shapeParam', v: number) {
    const s = design.stages[0].sections[0];
    if (s.kind === 'nose') {
      s.spec = { ...s.spec, [field]: v };
      design = { ...design };
      refresh();
    }
  }

  function updateNoseShape(shape: ShapeType) {
    const s = design.stages[0].sections[0];
    if (s.kind === 'nose') {
      s.spec = { ...s.spec, shape };
      design = { ...design };
      refresh();
    }
  }

  function updateBody(field: 'length' | 'outerRadius', v: number) {
    const s = design.stages[0].sections[1];
    if (s.kind === 'body') {
      s.spec = { ...s.spec, [field]: v };
      design = { ...design };
      refresh();
    }
  }

  function updateFinCount(v: number) {
    const f = design.stages[0].fins[0];
    f.spec = { ...f.spec, count: v };
    design = { ...design };
    refresh();
  }
</script>

<div class="designer">
  <aside class="panel">
    <h2>Nose cone</h2>
    {#if design.stages[0].sections[0].kind === 'nose'}
      {@const ns = design.stages[0].sections[0].spec}
      <label>
        Shape
        <select value={ns.shape} onchange={(e) => updateNoseShape((e.currentTarget as HTMLSelectElement).value as ShapeType)}>
          {#each shapes as s}<option value={s}>{s}</option>{/each}
        </select>
      </label>
      <label>
        Length (m): {ns.length.toFixed(3)}
        <input type="range" min="0.04" max="0.30" step="0.005" value={ns.length}
          oninput={(e) => updateNose('length', +(e.currentTarget as HTMLInputElement).value)} />
      </label>
      <label>
        Base radius (m): {ns.baseRadius.toFixed(3)}
        <input type="range" min="0.005" max="0.10" step="0.001" value={ns.baseRadius}
          oninput={(e) => updateNose('baseRadius', +(e.currentTarget as HTMLInputElement).value)} />
      </label>
    {/if}

    <h2>Body tube</h2>
    {#if design.stages[0].sections[1].kind === 'body'}
      {@const bs = design.stages[0].sections[1].spec}
      <label>
        Length (m): {bs.length.toFixed(3)}
        <input type="range" min="0.10" max="2.00" step="0.01" value={bs.length}
          oninput={(e) => updateBody('length', +(e.currentTarget as HTMLInputElement).value)} />
      </label>
      <label>
        Outer radius (m): {bs.outerRadius.toFixed(3)}
        <input type="range" min="0.005" max="0.10" step="0.001" value={bs.outerRadius}
          oninput={(e) => updateBody('outerRadius', +(e.currentTarget as HTMLInputElement).value)} />
      </label>
    {/if}

    <h2>Fin set</h2>
    <label>
      Count: {design.stages[0].fins[0].spec.count}
      <input type="range" min="2" max="8" step="1" value={design.stages[0].fins[0].spec.count}
        oninput={(e) => updateFinCount(+(e.currentTarget as HTMLInputElement).value)} />
    </label>

    <h2>Display</h2>
    <label class="row">
      <input type="checkbox" bind:checked={showWire} />
      Wireframe
    </label>

    {#if stats}
      <div class="stats">
        <div>Vertices: {stats.vertices.toLocaleString()}</div>
        <div>Triangles: {stats.triangles.toLocaleString()}</div>
        <div class:bad={!stats.manifold}>Manifold: {stats.manifold ? 'yes' : 'NO'}</div>
      </div>
    {/if}

    {#if error}
      <div class="error">{error}</div>
    {/if}
  </aside>

  <section class="viewer">
    <canvas bind:this={canvas}></canvas>
  </section>
</div>

<style>
  .designer { display: grid; grid-template-columns: 280px 1fr; height: 100%; }
  .panel {
    background: var(--panel); border-right: 1px solid var(--border);
    padding: 16px; overflow-y: auto;
  }
  .panel h2 {
    font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px;
    color: var(--muted); margin: 20px 0 8px;
  }
  .panel h2:first-child { margin-top: 0; }
  .panel label {
    display: block; margin-bottom: 12px; font-size: 13px; color: var(--text);
  }
  .panel label.row { display: flex; align-items: center; gap: 8px; }
  .panel input[type=range] { width: 100%; margin-top: 4px; }
  .panel select { width: 100%; padding: 6px; background: var(--bg); color: var(--text); border: 1px solid var(--border); border-radius: 4px; margin-top: 4px; }
  .stats { font-size: 12px; color: var(--muted); margin-top: 16px; padding-top: 12px; border-top: 1px solid var(--border); }
  .stats div { padding: 2px 0; }
  .stats .bad { color: #ff8866; }
  .error { color: #ff8866; font-size: 12px; margin-top: 16px; word-break: break-word; }
  .viewer { position: relative; background: #0e1116; }
  canvas { width: 100%; height: 100%; display: block; }
</style>
