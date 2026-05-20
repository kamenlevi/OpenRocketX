# Sidecar binaries

Tauri expects two artifacts here at bundle time:

- `openrocketx-launcher` (or `openrocketx-launcher.exe` on Windows): a small
  native launcher that locates the bundled JRE and exec's the engine jar.
  Build with: `./tools/build-launcher.sh` (TODO; for development use the
  shell script below).
- `openrocketx-engine.jar`: the shadow jar produced by `gradle :api:shadowJar`
  (TODO: add the Shadow plugin to `api/build.gradle`).

For local development without a launcher, run the engine directly via:

    gradle :api:run

and set `VITE_OPENROCKETX_BASE=http://127.0.0.1:NNNNN` to point the frontend
at it. The Tauri sidecar code in `src/main.rs` follows the production layout
above and parses `PORT=NNNNN` from the engine's stdout to discover the port.
