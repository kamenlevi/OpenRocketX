package x.bridge;

import info.openrocket.core.rocketcomponent.BodyTube;
import info.openrocket.core.rocketcomponent.FinSet;
import info.openrocket.core.rocketcomponent.NoseCone;
import info.openrocket.core.rocketcomponent.Rocket;
import info.openrocket.core.rocketcomponent.RocketComponent;
import info.openrocket.core.rocketcomponent.Transition;
import info.openrocket.core.util.CoordinateIF;
import x.model.BodyTubeSpec;
import x.model.FinSetSpec;
import x.model.NoseConeSpec;
import x.model.RocketSpec;
import x.model.ShapeType;
import x.model.Stage;
import x.model.TransitionSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Walks an OpenRocket {@link Rocket} and flattens it into the parametric
 * {@link RocketSpec} our exporter consumes. The two models live in parallel
 * on purpose: the exporter must remain buildable without the heavyweight
 * upstream core/ module, and the wire format we ship to the frontend stays
 * compact (small JSON, easy TypeScript types).
 *
 * Coverage in this stub:
 *   - NoseCone        → Stage.NoseConeSection
 *   - BodyTube        → Stage.BodyTubeSection
 *   - Transition      → Stage.TransitionSection
 *   - FinSet (any)    → Stage.MountedFinSet (via getFinPoints)
 *
 * TODO when wiring in:
 *   - Multi-stage: walk AxialStage and emit one Stage per upstream stage.
 *   - InnerTube, MassObject, Parachute: skip for STL (no external geometry).
 *   - Pods and component assemblies: recurse via ComponentAssembly.
 *   - Rail buttons, launch lugs: extrude as small cylinders.
 *   - Tube fins: cylindrical fin pieces, mountRadius offset and replicate.
 *
 * This file is excluded from compilation in the standalone build (see
 * api/build.gradle). Enable with `-PwithCore=true` once the upstream gradle
 * build succeeds end-to-end.
 */
public final class OrkAdapter {

    private OrkAdapter() {}

    public static RocketSpec fromOpenRocket(Rocket rocket) {
        List<Stage.BodySection> sections = new ArrayList<>();
        List<Stage.MountedFinSet> fins = new ArrayList<>();
        double xCursor = 0.0;

        for (RocketComponent child : flatten(rocket)) {
            if (child instanceof NoseCone nc) {
                sections.add(new Stage.NoseConeSection(new NoseConeSpec(
                        mapShape(nc.getShapeType()),
                        nc.getLength(),
                        nc.getAftRadius(),
                        nc.getShapeParameter(),
                        nc.getThickness()
                )));
                xCursor += nc.getLength();
            } else if (child instanceof BodyTube bt) {
                sections.add(new Stage.BodyTubeSection(new BodyTubeSpec(
                        bt.getLength(),
                        bt.getOuterRadius(),
                        bt.getInnerRadius()
                )));
                xCursor += bt.getLength();
            } else if (child instanceof Transition tr) {
                sections.add(new Stage.TransitionSection(new TransitionSpec(
                        mapShape(tr.getShapeType()),
                        tr.getLength(),
                        tr.getForeRadius(),
                        tr.getAftRadius(),
                        tr.getShapeParameter()
                )));
                xCursor += tr.getLength();
            } else if (child instanceof FinSet fs) {
                fins.add(new Stage.MountedFinSet(toFinSetSpec(fs), xCursor));
            }
        }

        Stage stage = new Stage("core", sections, fins);
        return new RocketSpec(rocket.getName(), List.of(stage));
    }

    private static FinSetSpec toFinSetSpec(FinSet fs) {
        CoordinateIF[] pts = fs.getFinPoints();
        double[][] arr = new double[pts.length][2];
        for (int i = 0; i < pts.length; i++) {
            arr[i][0] = pts[i].x();
            arr[i][1] = pts[i].y();
        }
        double mountRadius = (fs.getParent() instanceof BodyTube parent)
                ? parent.getOuterRadius()
                : 0.025; // sensible default for free-mounted fins
        return new FinSetSpec(
                fs.getFinCount(),
                fs.getThickness(),
                fs.getCantAngle(),
                arr,
                mountRadius
        );
    }

    private static ShapeType mapShape(Transition.Shape s) {
        // OpenRocket's enum names match ours; the upstream toString() uses
        // localized strings, so compare by name() explicitly.
        return switch (s.name()) {
            case "CONICAL"   -> ShapeType.CONICAL;
            case "OGIVE"     -> ShapeType.OGIVE;
            case "ELLIPSOID" -> ShapeType.ELLIPSOID;
            case "PARABOLIC" -> ShapeType.PARABOLIC;
            case "POWER"     -> ShapeType.POWER;
            case "HAACK"     -> ShapeType.HAACK;
            default          -> ShapeType.OGIVE;
        };
    }

    /** Depth-first flatten of the component tree. */
    private static List<RocketComponent> flatten(RocketComponent root) {
        List<RocketComponent> out = new ArrayList<>();
        for (RocketComponent c : root.getChildren()) {
            out.add(c);
            out.addAll(flatten(c));
        }
        return out;
    }
}
