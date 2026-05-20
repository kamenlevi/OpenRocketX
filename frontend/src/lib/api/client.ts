import type { MotorList, PreviewMesh, RocketSpec, SimResult, SimSample } from './types.js';

/**
 * Base URL of the local Java engine. In a Tauri build this is read from a
 * sidecar handshake (the engine prints "PORT=NNNNN" on startup) and pushed
 * into window.__OPENROCKETX_BASE__. In dev mode against vite, we fall back
 * to OPENROCKETX_BASE env or http://127.0.0.1:8080.
 */
function baseUrl(): string {
  if (typeof window !== 'undefined') {
    const w = window as unknown as { __OPENROCKETX_BASE__?: string };
    if (w.__OPENROCKETX_BASE__) return w.__OPENROCKETX_BASE__;
  }
  return import.meta.env.VITE_OPENROCKETX_BASE ?? 'http://127.0.0.1:8080';
}

async function jsonPost<T>(path: string, body: unknown): Promise<T> {
  const r = await fetch(baseUrl() + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  if (!r.ok) throw new Error(`${path}: HTTP ${r.status} ${await r.text()}`);
  return r.json() as Promise<T>;
}

async function jsonGet<T>(path: string): Promise<T> {
  const r = await fetch(baseUrl() + path);
  if (!r.ok) throw new Error(`${path}: HTTP ${r.status} ${await r.text()}`);
  return r.json() as Promise<T>;
}

export const api = {
  health(): Promise<string> {
    return fetch(baseUrl() + '/healthz').then((r) => r.text());
  },

  preview(spec: RocketSpec): Promise<PreviewMesh> {
    return jsonPost<PreviewMesh>('/api/designs/preview', spec);
  },

  validate(spec: RocketSpec) {
    return jsonPost<{
      name: string;
      stageCount: number;
      vertexCount: number;
      triangleCount: number;
      manifold: boolean;
    }>('/api/designs/validate', spec);
  },

  motors(filter?: { class?: string; mfg?: string }): Promise<MotorList> {
    const q = new URLSearchParams();
    if (filter?.class) q.set('class', filter.class);
    if (filter?.mfg) q.set('mfg', filter.mfg);
    return jsonGet<MotorList>('/api/motors' + (q.toString() ? '?' + q : ''));
  },

  async exportStl(spec: RocketSpec): Promise<Blob> {
    const r = await fetch(baseUrl() + '/api/export/stl', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(spec)
    });
    if (!r.ok) throw new Error(`stl export: HTTP ${r.status}`);
    return r.blob();
  },

  async exportGltf(spec: RocketSpec): Promise<Blob> {
    const r = await fetch(baseUrl() + '/api/export/gltf', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(spec)
    });
    if (!r.ok) throw new Error(`gltf export: HTTP ${r.status}`);
    return r.blob();
  },

  simulate(spec: RocketSpec): Promise<SimResult> {
    return jsonPost<SimResult>('/api/simulate', spec);
  },

  /**
   * Open the live simulation WebSocket. The handler is called once per
   * step; the returned closer stops the stream.
   */
  simulateStream(onSample: (s: SimSample | { done: true }) => void, onError?: (e: Event) => void): () => void {
    const wsUrl = baseUrl().replace(/^http/, 'ws') + '/api/simulate/stream';
    const ws = new WebSocket(wsUrl);
    ws.onmessage = (ev) => onSample(JSON.parse(ev.data));
    if (onError) ws.onerror = onError;
    return () => ws.close();
  }
};
