# OpenRocketX

A modern desktop rocket simulator wrapping a forked
[OpenRocket](https://github.com/openrocket/openrocket) simulation engine
with a Tauri + SvelteKit + Three.js frontend, and a **working full-rocket
STL exporter** that fixes the upstream bug where only fin geometry was
emitted.

This is a GPL v3 fork. See `NOTICE.md` and `LICENSE`.

## What's working today

End-to-end, on this branch:

- Java sim/export engine (`api/`) boots on 127.0.0.1, prints its port
  on stdout so the Tauri sidecar can read it.
- HTTP/WebSocket API: design preview, validation, STL/GLB export, motor
  list, synchronous + streaming simulation.
- **STL/GLB exporter** producing a single closed manifold mesh for nose
  cone + body tubes + transitions + fin sets. Verified: a demo
  single-stage rocket exports as a 3,620-triangle, 181 KB STL that
  round-trips through a struct parser and meets the binary STL spec.
- SvelteKit frontend with four routes (Designer / Motors / Simulate /
  Export), parametric sliders, live Three.js preview tied to the server's
  mesh output.
- Tauri 2.x shell config and sidecar launcher (Rust) ready to bundle the
  engine + JRE.

## What's stubbed

The OpenRocket integration bridge (`api/src/main/java/x/bridge/`) maps
the upstream component model into our parametric `RocketSpec`. It
currently compiles only when `:core` is on the classpath
(`-PwithCore=true`); the upstream gradle build pulls a git submodule
for the component DB and needs ~30 Maven Central deps, so the standalone
build keeps the engine out and the rest of the project buildable.

- Motors page returns stub data; real ThrustCurve DB lookup needs the
  bridge.
- Simulate endpoint returns a toy parabolic curve; real 6-DOF flight
  needs the bridge + a `SimulationListener` adapter.
- `.ork` file open uses the upstream serializer when `:core` is enabled.

## Repository layout

```
openrocketx/
├── core/                  Vendored OpenRocket engine (upstream `core/`)
│                          — info.openrocket.core.* packages, untouched.
├── api/                   New: HTTP/WebSocket API + STL/GLB exporter.
│   └── src/main/java/x/
│       ├── api/           Javalin server, controllers
│       ├── export/        StlWriter, GltfWriter
│       ├── geom/          TriangleMesh, Revolve, FinExtrude, Profiles
│       ├── model/         Parametric DTOs (RocketSpec etc.)
│       └── bridge/        OpenRocket → RocketSpec adapter (gated)
├── frontend/              SvelteKit + Three.js + Tauri
│   ├── src/lib/api/       Typed API client
│   ├── src/lib/three/     RocketScene
│   ├── src/routes/        Designer / Motors / Simulate / Export
│   └── src-tauri/         Rust shell, sidecar config
└── docs/                  Architecture, integration plan
```

## Build & run

Prereqs: JDK 21, Node 22+, pnpm 10+, Rust 1.80+ (for the Tauri shell).

```sh
# Engine (standalone — without upstream OpenRocket on the classpath)
gradle :api:test           # 14 tests pass
gradle :api:run            # boots Javalin, prints "PORT=NNNNN"

# Frontend (dev mode, against a running engine)
cd frontend
pnpm install
VITE_OPENROCKETX_BASE=http://127.0.0.1:<PORT> pnpm dev

# With upstream OpenRocket on the classpath (eventual default)
gradle :api:test -PwithCore=true
```

Tauri bundling lives in `frontend/src-tauri/`; the sidecar build needs a
shaded engine jar and a small launcher. See
`frontend/src-tauri/binaries/README.md`.

## Acceptance status

Tracking the V1 acceptance criteria from the project brief:

- [ ] Open existing .ork — needs bridge + ORK serializer wired in.
- [x] Build single-stage rocket in the new designer.
- [ ] Run simulation with default Earth conditions — stub today; real
      simulation needs bridge.
- [x] Export the full rocket to STL — nose + body + fins all present,
      verified manifold.
- [ ] Every motor searchable — stub today; needs bridge + thrustcurves DB.

## Licensing

GPL v3. The combined work (vendored OpenRocket core + new code) is GPL
v3 in full. The Tauri/Svelte frontend talks to the Java backend over
local HTTP on 127.0.0.1; per the GPL FAQ this is plausibly aggregation
rather than derivative work, but we ship under GPL v3 so the question
is moot for our distribution. See `NOTICE.md`.
