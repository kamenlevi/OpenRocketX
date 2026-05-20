# Architecture

OpenRocketX is two processes that talk to each other over local HTTP:

```
┌─────────────────────────────────────────────────────────┐
│ Tauri shell (Rust)                                      │
│  ┌────────────────────────────────────────────────────┐ │
│  │ SvelteKit + Three.js (chromium webview)            │ │
│  │  - Designer, Motors, Simulate, Export pages        │ │
│  │  - lib/api/client.ts → fetch / WebSocket           │ │
│  └───────────────────────────┬────────────────────────┘ │
│                              │ http://127.0.0.1:NNNNN   │
│                              ▼                          │
│  ┌────────────────────────────────────────────────────┐ │
│  │ Java engine (sidecar process)                      │ │
│  │  x.api      Javalin HTTP/WebSocket                 │ │
│  │  x.geom     TriangleMesh, Revolve, FinExtrude      │ │
│  │  x.export   StlWriter, GltfWriter                  │ │
│  │  x.model    Parametric DTOs                        │ │
│  │  x.bridge   OpenRocket adapter (gated on :core)    │ │
│  │  info.openrocket.core.*  vendored upstream         │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

## Why two processes

OpenRocket's simulation engine is Java, lives in one of the most complete
open-source motor/component databases, and has a hand-tuned 6-DOF flight
simulator. Reimplementing any of that would be a 5-year project. So:
**keep the engine in Java, build the UI in something modern**. The two
processes talk over `127.0.0.1`, isolated by a CSP that blocks any other
host.

## Port discovery

The Java engine binds to an OS-chosen port (`OPENROCKETX_PORT` env var
can pin it) and writes `PORT=NNNNN` as the **first line of stdout** before
serving any traffic. The Tauri sidecar code in `frontend/src-tauri/src/main.rs`
reads that line, stashes the base URL in tauri State, and emits an
`engine-ready` event the webview listens for. The webview reads
`window.__OPENROCKETX_BASE__` (set by the Tauri JS bridge) for production
or `VITE_OPENROCKETX_BASE` for dev.

## Two parallel models

There are two component models in this codebase:

1. `info.openrocket.core.rocketcomponent.*` — the upstream tree of
   `RocketComponent` subclasses with `getChildren()`, listeners,
   flight-configuration overrides, the full object model OpenRocket
   needs internally.
2. `x.model.*` — small records (`NoseConeSpec`, `BodyTubeSpec`,
   `FinSetSpec`, `Stage`, `RocketSpec`) that map 1:1 to the JSON we
   ship to the frontend and consume in the exporter.

`x.bridge.OrkAdapter` walks model (1) and emits model (2). Keeping them
separate means:

- **The exporter is testable in isolation.** It depends only on
  `x.model` and `x.geom`, so the geometry tests don't need the upstream
  Gradle build (which needs ~30 Maven deps and a git submodule for the
  component DB).
- **The wire format is small.** A `RocketSpec` is a few hundred bytes
  of JSON; the upstream tree is rich with state we don't need on the
  client.
- **TypeScript types stay simple.** `frontend/src/lib/api/types.ts`
  mirrors the records directly.

## The export pipeline (the headline)

Upstream OpenRocket has an OBJ export feature that, empirically, only
emitted fins — body tubes and nose cones were missing. The pipeline
here replaces that:

```
RocketSpec ──► RocketMesh.build ──► TriangleMesh ──► StlWriter / GltfWriter
                  │                       │
                  ├─ Revolve              ├─ vertex weld
                  │   (nose, body,        │   (1e-7 m tolerance)
                  │    transition)        │
                  ├─ FinExtrude           └─ edge-manifold check
                  │   (any 2D polygon
                  │    → prism × N copies)
                  └─ Profiles
                      (Haack, ogive,
                       parabolic, etc.)
```

Each component generates a sub-mesh; sub-meshes are merged via
`TriangleMesh.merge()` which dedupes shared vertices on the fly so the
output is a single watertight solid where component surfaces meet
exactly. The `isEdgeManifold()` check is the necessary condition for a
slicer-friendly mesh; the export endpoint sets an
`X-OpenRocketX-Warning: non-manifold-mesh` response header if it fails.

## Tessellation defaults

```java
new Tesselation(
    radialSegments:    64,   // around the body axis
    noseAxial:         32,   // along the nose cone
    bodyAxial:          4,   // along a body tube (it's straight)
    transitionAxial:   24    // along a transition
);
```

These are tuned for visual smoothness at 1× scale (10 cm body tube
radius) without ballooning STL size. A 50 cm body tube + 12 cm ogive
nose + 3 fins → ~3,600 triangles, 180 KB binary STL. For higher detail
or coarser previews, plumb a `Tesselation` override through the API
(not done yet).

## Simulation streaming

The eventual integration sketch:

```
POST /api/simulate {RocketSpec, conditions} ─►
  x.bridge.SimBridge.run(...)
    ├─ build info.openrocket.core.rocketcomponent.Rocket from RocketSpec
    ├─ pick a FlightConfiguration / SimulationOptions
    ├─ register a SimulationListener that emits per-step samples
    │   {t, altitude, velocity[3], orientation[4], thrust, drag, ...}
    └─ run SimulationEngine

WS /api/simulate/stream  ◄─ same listener, but pushes each sample as
                            JSON to the client instead of buffering.
```

Today the SimController returns a kinematic toy; the listener wiring is
the integration point in `x.bridge`.
