package x.export;

import org.junit.jupiter.api.Test;
import x.geom.RocketMesh;
import x.geom.TriangleMesh;
import x.model.BodyTubeSpec;
import x.model.FinSetSpec;
import x.model.NoseConeSpec;
import x.model.RocketSpec;
import x.model.ShapeType;
import x.model.Stage;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end test for the headline feature: a single-stage rocket with nose
 * cone + body tube + 3 trapezoidal fins should produce an STL whose body is
 * present (the upstream OpenRocket OBJ exporter shipped only fins).
 */
class RocketExportTest {

    @Test
    void singleStageRocketHasNoseAndBodyAndFins() {
        // Single-stage rocket: 12cm ogive nose, 50cm body (5cm OD), 3 trap fins.
        NoseConeSpec nose = new NoseConeSpec(ShapeType.OGIVE, 0.12, 0.025, 1.0, 0.0015);
        BodyTubeSpec body = new BodyTubeSpec(0.50, 0.025, 0.024);
        double[][] finPts = {
                {0, 0}, {0.10, 0}, {0.10 - 0.03, 0.06}, {0.10 - 0.03 - 0.04, 0.06}
        };
        FinSetSpec fins = new FinSetSpec(3, 0.003, 0.0, finPts, 0.025);

        Stage stage = new Stage(
                "core",
                List.of(new Stage.NoseConeSection(nose), new Stage.BodyTubeSection(body)),
                List.of(new Stage.MountedFinSet(fins, 0.12 + 0.50 - 0.12))
        );
        RocketSpec rocket = new RocketSpec("test-rocket", List.of(stage));

        TriangleMesh mesh = RocketMesh.build(rocket);

        // Headline assertion: the mesh must contain triangles spanning the
        // nose, body, and fins — not just the fins, which is the upstream bug.
        boolean hasNoseTip = mesh.vertices().stream().anyMatch(v ->
                Math.abs(v.x()) < 1e-6 && v.y() == 0 && v.z() == 0);
        boolean hasBodyMidSurface = mesh.vertices().stream().anyMatch(v ->
                v.x() > 0.20 && v.x() < 0.50
                && Math.abs(Math.hypot(v.y(), v.z()) - 0.025) < 1e-6);
        boolean hasFinTip = mesh.vertices().stream().anyMatch(v ->
                v.x() > 0.55 && Math.hypot(v.y(), v.z()) > 0.07);

        assertTrue(hasNoseTip, "nose cone tip vertex must be present");
        assertTrue(hasBodyMidSurface, "body tube surface vertices must be present");
        assertTrue(hasFinTip, "fin tip vertices must be present");

        assertTrue(mesh.triangleCount() > 1000, "non-trivial mesh expected");
    }

    @Test
    void exportedBinaryStlIsParseable() throws Exception {
        BodyTubeSpec body = new BodyTubeSpec(0.20, 0.025, 0.024);
        RocketSpec rocket = new RocketSpec("tube",
                List.of(new Stage("core",
                        List.of(new Stage.BodyTubeSection(body)),
                        List.of())));

        byte[] stl = StlWriter.toBinary(RocketMesh.build(rocket), "tube");

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(stl))) {
            byte[] header = new byte[80];
            in.readFully(header);
            ByteBuffer countBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            in.readFully(countBuf.array());
            int triCount = countBuf.getInt(0);
            assertTrue(triCount > 0);
            assertEquals(stl.length, 84 + 50L * triCount);
        }
    }
}
