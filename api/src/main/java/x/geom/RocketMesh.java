package x.geom;

import x.model.BodyTubeSpec;
import x.model.FinSetSpec;
import x.model.NoseConeSpec;
import x.model.RocketSpec;
import x.model.Stage;
import x.model.TransitionSpec;

import java.util.function.DoubleUnaryOperator;

/**
 * Builds the full triangle mesh for a RocketSpec. Sections are laid out tip
 * (x=0) to base along +X. Fin sets are translated to the configured offset
 * within the stage.
 *
 * Default tesselation: 64 radial segments, ~32 axial segments per body
 * section. Override per-call if you need finer detail.
 */
public final class RocketMesh {

    public record Tesselation(int radialSegments, int noseAxial, int bodyAxial, int transitionAxial) {
        public static final Tesselation DEFAULT = new Tesselation(64, 32, 4, 24);
    }

    private RocketMesh() {}

    public static TriangleMesh build(RocketSpec spec) {
        return build(spec, Tesselation.DEFAULT);
    }

    public static TriangleMesh build(RocketSpec spec, Tesselation tess) {
        TriangleMesh mesh = new TriangleMesh();
        double xCursor = 0.0;

        for (Stage stage : spec.stages()) {
            double stageStart = xCursor;
            for (Stage.BodySection section : stage.sections()) {
                if (section instanceof Stage.NoseConeSection ns) {
                    mesh.merge(buildNose(ns.spec(), xCursor, tess));
                    xCursor += ns.spec().length();
                } else if (section instanceof Stage.BodyTubeSection bt) {
                    mesh.merge(buildBodyTube(bt.spec(), xCursor, tess));
                    xCursor += bt.spec().length();
                } else if (section instanceof Stage.TransitionSection tr) {
                    mesh.merge(buildTransition(tr.spec(), xCursor, tess));
                    xCursor += tr.spec().length();
                }
            }
            for (Stage.MountedFinSet fin : stage.fins()) {
                mesh.merge(FinExtrude.build(fin.spec(), stageStart + fin.xOffset()));
            }
        }
        return mesh;
    }

    private static TriangleMesh buildNose(NoseConeSpec ns, double xOff, Tesselation t) {
        DoubleUnaryOperator p = Profiles.profile(
                ns.shape(), ns.length(), 0.0, ns.baseRadius(), ns.shapeParam());
        return Revolve.build(p, ns.length(), t.noseAxial(), t.radialSegments(), xOff);
    }

    private static TriangleMesh buildBodyTube(BodyTubeSpec bt, double xOff, Tesselation t) {
        double r = bt.outerRadius();
        return Revolve.build(x -> r, bt.length(), t.bodyAxial(), t.radialSegments(), xOff);
    }

    private static TriangleMesh buildTransition(TransitionSpec tr, double xOff, Tesselation t) {
        DoubleUnaryOperator p = Profiles.profile(
                tr.shape(), tr.length(), tr.foreRadius(), tr.aftRadius(), tr.shapeParam());
        return Revolve.build(p, tr.length(), t.transitionAxial(), t.radialSegments(), xOff);
    }

    /** Convenience for V1: build a mesh for just a single fin set at the origin. */
    public static TriangleMesh forFinSet(FinSetSpec spec) {
        return FinExtrude.build(spec, 0.0);
    }
}
