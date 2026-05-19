package x.geom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RevolveTest {

    @Test
    void cylinderIsManifold() {
        TriangleMesh m = Revolve.build(x -> 0.05, 0.5, 4, 32, 0.0);
        assertTrue(m.isEdgeManifold(), "cylinder must be a closed manifold surface");
        // Sanity: 32 quads × 4 axial × 2 tris = 256 side + 2 × 32 caps = 320 tris.
        assertEquals(256 + 64, m.triangleCount());
    }

    @Test
    void noseConeTipIsClosed() {
        TriangleMesh m = Revolve.build(x -> Math.sqrt(Math.max(0, 1 - (1 - x) * (1 - x))) * 0.05,
                1.0, 16, 32, 0.0);
        assertTrue(m.isEdgeManifold(), "nose cone with tip at x=0 must be closed");
    }

    @Test
    void cylinderEndCapsArePresent() {
        TriangleMesh m = Revolve.build(x -> 0.05, 0.5, 2, 16, 0.0);
        // Tip at neither end → both caps fire → 16 + 16 tri caps.
        long capTris = m.triangles().stream()
                .filter(t -> {
                    Vec3 a = m.vertices().get(t.a());
                    Vec3 b = m.vertices().get(t.b());
                    Vec3 c = m.vertices().get(t.c());
                    return a.x() == b.x() && b.x() == c.x();
                })
                .count();
        assertEquals(32, capTris);
    }
}
