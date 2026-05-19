package x.geom;

import org.junit.jupiter.api.Test;
import x.model.FinSetSpec;

import static org.junit.jupiter.api.Assertions.*;

class FinExtrudeTest {

    private static FinSetSpec trapezoidal(int count) {
        // Root chord 0.10m, tip 0.04m, sweep 0.03m, height 0.06m.
        double[][] pts = {
                {0, 0},
                {0.10, 0},
                {0.10 - 0.03, 0.06},
                {0.10 - 0.03 - 0.04, 0.06}
        };
        return new FinSetSpec(count, 0.003, 0.0, pts, 0.025);
    }

    @Test
    void singleFinIsManifold() {
        TriangleMesh m = FinExtrude.build(trapezoidal(1), 0.0);
        assertTrue(m.isEdgeManifold(), "single fin prism must be edge-manifold");
    }

    @Test
    void threeFinsAreSeparateButCombinedMeshIsValid() {
        TriangleMesh m = FinExtrude.build(trapezoidal(3), 0.0);
        // Three disjoint prisms → each one is manifold; the union's edges
        // are still each shared by exactly 2 triangles.
        assertTrue(m.isEdgeManifold());
        // Each fin = 2 face fans (2 tris each) + 4 side quads (8 tris) = 12 tris.
        // 3 fins → 36 triangles.
        assertEquals(36, m.triangleCount());
    }

    @Test
    void finsRotateAroundAxis() {
        TriangleMesh m = FinExtrude.build(trapezoidal(4), 0.0);
        // All 4 fins should have at least one vertex per quadrant.
        boolean q1 = false, q2 = false, q3 = false, q4 = false;
        for (Vec3 v : m.vertices()) {
            if (v.y() > 0.001 && v.z() > 0.001) q1 = true;
            else if (v.y() < -0.001 && v.z() > 0.001) q2 = true;
            else if (v.y() < -0.001 && v.z() < -0.001) q3 = true;
            else if (v.y() > 0.001 && v.z() < -0.001) q4 = true;
        }
        assertTrue(q1 && q2 && q3 && q4, "fins should populate all four quadrants");
    }
}
