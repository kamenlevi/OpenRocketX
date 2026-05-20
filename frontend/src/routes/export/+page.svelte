<script lang="ts">
  import { api } from '$lib/api/client.js';
  import { exampleRocket } from '$lib/examples.js';

  let status = $state<string | null>(null);
  let validation = $state<{
    name: string;
    stageCount: number;
    vertexCount: number;
    triangleCount: number;
    manifold: boolean;
  } | null>(null);

  async function downloadStl() {
    status = 'Rendering STL…';
    try {
      const blob = await api.exportStl(exampleRocket());
      saveAs(blob, 'demo.stl');
      status = `Done — ${blob.size.toLocaleString()} bytes`;
    } catch (e) {
      status = 'Error: ' + (e instanceof Error ? e.message : String(e));
    }
  }

  async function downloadGltf() {
    status = 'Rendering GLB…';
    try {
      const blob = await api.exportGltf(exampleRocket());
      saveAs(blob, 'demo.glb');
      status = `Done — ${blob.size.toLocaleString()} bytes`;
    } catch (e) {
      status = 'Error: ' + (e instanceof Error ? e.message : String(e));
    }
  }

  async function validate() {
    try {
      validation = await api.validate(exampleRocket());
    } catch (e) {
      status = 'Error: ' + (e instanceof Error ? e.message : String(e));
    }
  }

  function saveAs(blob: Blob, filename: string) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }
</script>

<div class="export">
  <h2>Export</h2>

  <section>
    <h3>STL (binary)</h3>
    <p>Drop into FreeCAD, Cura, PrusaSlicer, or any CAD tool. Single manifold mesh of the full rocket.</p>
    <button onclick={downloadStl}>Download demo.stl</button>
  </section>

  <section>
    <h3>glTF 2.0 (GLB)</h3>
    <p>Modern web/AR-friendly mesh format. Same geometry as STL with normals included.</p>
    <button onclick={downloadGltf}>Download demo.glb</button>
  </section>

  <section>
    <h3>Validate</h3>
    <p>Run the export pipeline and report mesh stats and manifoldness without downloading.</p>
    <button onclick={validate}>Validate demo design</button>
    {#if validation}
      <table>
        <tbody>
          <tr><th>Name</th><td>{validation.name}</td></tr>
          <tr><th>Stages</th><td>{validation.stageCount}</td></tr>
          <tr><th>Vertices</th><td>{validation.vertexCount.toLocaleString()}</td></tr>
          <tr><th>Triangles</th><td>{validation.triangleCount.toLocaleString()}</td></tr>
          <tr><th>Manifold</th><td class:bad={!validation.manifold}>{validation.manifold ? 'yes' : 'NO'}</td></tr>
        </tbody>
      </table>
    {/if}
  </section>

  {#if status}<div class="status">{status}</div>{/if}

  <p class="note">
    The upstream OpenRocket OBJ exporter only emitted fin geometry — that's
    the bug this fork exists to fix. The STL produced here includes nose
    cone, every body section, transitions, and every fin set as a single
    welded mesh.
  </p>
</div>

<style>
  .export { padding: 20px; max-width: 720px; }
  h2 { font-size: 16px; margin: 0 0 24px; }
  section { margin-bottom: 28px; }
  h3 { font-size: 13px; text-transform: uppercase; letter-spacing: 0.5px; color: var(--muted); margin: 0 0 6px; }
  section p { color: var(--text); margin: 0 0 12px; font-size: 13px; }
  button {
    background: var(--accent); color: var(--accent-text); border: 0;
    padding: 8px 14px; border-radius: 4px; cursor: pointer; font-size: 13px;
  }
  table { margin-top: 12px; font-size: 13px; border-collapse: collapse; }
  th, td { text-align: left; padding: 4px 12px 4px 0; }
  th { color: var(--muted); font-weight: 400; }
  .bad { color: #ff8866; }
  .status {
    margin-top: 16px; padding: 8px 12px; background: var(--panel);
    border-radius: 4px; font-size: 12px; color: var(--muted);
  }
  .note { color: var(--muted); font-size: 12px; margin-top: 24px; }
</style>
