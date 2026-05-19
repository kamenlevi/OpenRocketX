# OpenRocketX — Attribution

OpenRocketX is a fork of [OpenRocket](https://github.com/openrocket/openrocket),
licensed under the GNU General Public License v3.0. See `LICENSE`.

## What's vendored from upstream

The directory `core/` contains the `core/` Gradle module from OpenRocket
(`unstable` branch snapshot, May 2026). This is the simulation engine —
component models, motors, atmospheric models, the 6-DOF simulator. We
have NOT modified upstream files; our new code lives in:

- `core/src/main/java/x/api/`     — Javalin HTTP/WebSocket server
- `core/src/main/java/x/export/`  — STL / GLTF exporter (replaces the
                                    broken upstream OBJ exporter, which
                                    only emitted fins)
- `core/src/main/java/x/geom/`    — Tessellation helpers (revolve,
                                    extrude, weld)
- `core/src/main/java/x/model/`   — DTOs for the wire protocol

The upstream Swing UI module (`swing/`) is intentionally not vendored.
The simulation engine in `core/` does not depend on it.

## Licensing implications

Because OpenRocketX links upstream OpenRocket source, the combined work
is GPL v3. The Tauri/Svelte frontend in `frontend/` communicates with
the Java backend over local HTTP on 127.0.0.1; per the GPL FAQ this can
plausibly be argued as aggregation rather than derivative work, but the
project ships under GPL v3 in full so the question is moot for our
distribution.

## How to contribute back

Bug fixes that affect upstream files (anything outside `x.*` packages
or the `frontend/` tree) should be submitted to the upstream OpenRocket
project, not held here.
