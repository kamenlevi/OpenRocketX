# OpenRocket integration plan

How to turn the stubs in `x.bridge` and `MotorController` / `SimController`
into real engine calls. The standalone build (without `:core` on the
classpath) is what's working today; this is the path to a build with
the upstream engine fully wired.

## Step 1 — get the vendored core/ to compile

The vendored OpenRocket core (under `core/`) is the upstream snapshot,
unmodified. It builds against Maven Central + a git submodule for the
component DB (`openrocket-database`). The submodule path is hard-coded
in `core/build.gradle`:

```
def externalResourceComponents = './resources-src/datafiles/openrocket-database'
```

Options to satisfy it:

1. Add the submodule. From the repo root:
   ```
   git submodule add https://github.com/openrocket/openrocket-database.git \
                    core/resources-src/datafiles/openrocket-database
   git submodule update --init --recursive
   ```
   The upstream `externalComponentsValidate` task then passes.
2. Stub it. Touch the path with an empty `orc/` directory and a
   placeholder `LICENSE` + `README.md`; the upstream `externalComponentsCopy`
   task does a `from(...) include '**/*'` so an empty tree compiles, you
   just get no preset components. Useful for CI builds that don't need
   the preset database.

`core/build.gradle` also calls `serializeEngines` which fetches the
ThrustCurve motor data and writes a SQLite DB. The pre-shipped
`core/src/main/resources/datafiles/thrustcurves/thrustcurves.db` already
exists in the upstream snapshot we vendored, so this task is optional
unless you want to refresh.

Build with:

```sh
gradle :core:compileJava -PwithCore=true
```

Expect ~30 dependency downloads from Maven Central on the first run.

## Step 2 — enable x.bridge

Once `:core` builds, the bridge package compiles automatically. The
`api/build.gradle` snippet handles this:

```groovy
if (findProject(':core') == null) {
    sourceSets {
        main {
            java {
                exclude 'x/bridge/**'
            }
        }
    }
}
```

Smoke test:

```sh
gradle :api:test -PwithCore=true
# Then add an OrkAdapterTest that round-trips an upstream sample .ork:
#   core/src/main/resources/datafiles/examples/*.ork
```

## Step 3 — real motor list

Replace the stub in `MotorController` with a call into the upstream
ThrustCurve DB:

```java
// x.bridge.MotorBridge
import info.openrocket.core.thrustcurve.ThrustCurveMotor;
import info.openrocket.core.motor.MotorDatabase;

public List<Map<String, Object>> list(String impulseClass, String mfg) {
    MotorDatabase db = ...; // upstream provides Guice injection points
    return db.getMotorSets().stream()
        .filter(m -> impulseClass == null || m.getImpulseClass().equals(impulseClass))
        .filter(m -> mfg == null || m.getManufacturer().getDisplayName().equalsIgnoreCase(mfg))
        .map(MotorBridge::toWire)
        .toList();
}
```

The wire shape is already what the frontend expects (see
`MotorController.list` stub for the field list).

## Step 4 — real simulation

The shape we want:

```java
// x.bridge.SimBridge
import info.openrocket.core.simulation.SimulationEngine;
import info.openrocket.core.simulation.SimulationListener;

public void run(RocketSpec spec, Conditions cond, Consumer<Sample> onSample) {
    Rocket rocket = SpecToOrk.build(spec);  // inverse of OrkAdapter
    FlightConfiguration cfg = ...;
    SimulationOptions opts = mapConditions(cond);

    SimulationEngine engine = new BasicEventSimulationEngine();
    engine.addSimulationListener(new SimulationListener() {
        @Override public boolean handleStep(SimulationStatus status) {
            onSample.accept(new Sample(
                status.getSimulationTime(),
                status.getRocketPosition().z(),
                ...
            ));
            return true;
        }
    });
    engine.simulate(opts);
}
```

Wire it into `SimController.run` (synchronous) and `SimController.stream`
(WebSocket — replace the toy thread with `onSample` posting to
`ctx.send`).

## Step 5 — .ork file open

The upstream serializer is in
`info.openrocket.core.file.openrocket.OpenRocketLoader` and
`OpenRocketSaver`. Add endpoints:

```
POST   /api/files/open     multipart .ork → RocketSpec JSON
POST   /api/files/save     RocketSpec JSON → .ork bytes
```

In bridge:

```java
// x.bridge.OrkFile
public static RocketSpec open(InputStream ork) {
    OpenRocketDocument doc = new OpenRocketLoader().load(...);
    return OrkAdapter.fromOpenRocket(doc.getRocket());
}
```

## Step 6 — fold in shoulder geometry

The current exporter handles the main nose cone profile but ignores
shoulders (the cylindrical extension where a nose cone meets a body
tube). `NoseCone.getShoulderLength()` / `getShoulderRadius()` are
exposed; emit them as an additional body section in `OrkAdapter`. Same
for transitions (`getForeShoulderLength`, `getAftShoulderLength`).

## Risk register

- **Heavy dep graph in `:core`.** Maven Central fetches ~150 MB of jars
  on a clean build. CI cache is the workaround.
- **Component DB submodule.** Optional in upstream, required for preset
  components. Document both paths (with/without).
- **Free-form fin polygons can be non-convex.** Our `FinExtrude` uses a
  fan triangulation that assumes convex polygons. For free-form fins
  with concave outlines we need an ear-clipping triangulator before
  shipping; add as a follow-up.
- **Coordinate system.** OpenRocket uses +X aft along the body axis,
  +Y starboard, +Z up. Our exporter uses the same convention; the Three.js
  scene rotates the mesh ZX so the rocket stands vertical for display
  only — the underlying mesh keeps OpenRocket's axes intact for STL
  consumers.
