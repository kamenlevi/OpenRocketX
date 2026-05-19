package x.geom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable indexed triangle mesh. Vertices are deduplicated within rounding
 * tolerance via addVertex(); use addTriangle() with the returned indices.
 * Use merge() to combine sub-meshes into a single output.
 *
 * Edge-manifoldness is checkable via isEdgeManifold() — every edge must be
 * shared by exactly two triangles for a watertight closed surface, which is
 * what slicers want.
 */
public class TriangleMesh {

    private static final double WELD_EPS = 1e-7;
    private static final double WELD_SCALE = 1.0 / WELD_EPS;

    private record WeldKey(long ix, long iy, long iz) {}

    private final List<Vec3> vertices = new ArrayList<>();
    private final List<Triangle> triangles = new ArrayList<>();
    private final Map<WeldKey, Integer> weldIndex = new HashMap<>();

    public int addVertex(double x, double y, double z) {
        WeldKey key = new WeldKey(
                Math.round(x * WELD_SCALE),
                Math.round(y * WELD_SCALE),
                Math.round(z * WELD_SCALE));
        Integer existing = weldIndex.get(key);
        if (existing != null) return existing;
        int idx = vertices.size();
        vertices.add(new Vec3(x, y, z));
        weldIndex.put(key, idx);
        return idx;
    }

    public int addVertex(Vec3 v) { return addVertex(v.x(), v.y(), v.z()); }

    public void addTriangle(int a, int b, int c) {
        if (a == b || b == c || a == c) return; // degenerate
        triangles.add(new Triangle(a, b, c));
    }

    public void addQuad(int a, int b, int c, int d) {
        addTriangle(a, b, c);
        addTriangle(a, c, d);
    }

    public void merge(TriangleMesh other) {
        int[] remap = new int[other.vertices.size()];
        for (int i = 0; i < other.vertices.size(); i++) {
            Vec3 v = other.vertices.get(i);
            remap[i] = addVertex(v);
        }
        for (Triangle t : other.triangles) {
            addTriangle(remap[t.a()], remap[t.b()], remap[t.c()]);
        }
    }

    public List<Vec3> vertices() { return vertices; }
    public List<Triangle> triangles() { return triangles; }
    public int vertexCount() { return vertices.size(); }
    public int triangleCount() { return triangles.size(); }

    /** Outward normal for the triangle at index i. */
    public Vec3 normalOf(int i) {
        Triangle t = triangles.get(i);
        Vec3 a = vertices.get(t.a());
        Vec3 b = vertices.get(t.b());
        Vec3 c = vertices.get(t.c());
        return b.sub(a).cross(c.sub(a)).normalized();
    }

    /**
     * Edge-manifold check: every edge shared by exactly two triangles. This
     * is the necessary condition for a watertight printable mesh.
     */
    public boolean isEdgeManifold() {
        Map<Long, Integer> edgeCount = new HashMap<>();
        for (Triangle t : triangles) {
            bumpEdge(edgeCount, t.a(), t.b());
            bumpEdge(edgeCount, t.b(), t.c());
            bumpEdge(edgeCount, t.c(), t.a());
        }
        for (int c : edgeCount.values()) {
            if (c != 2) return false;
        }
        return true;
    }

    private static void bumpEdge(Map<Long, Integer> m, int u, int v) {
        long key = u < v ? ((long) u << 32) | (v & 0xffffffffL)
                         : ((long) v << 32) | (u & 0xffffffffL);
        m.merge(key, 1, Integer::sum);
    }
}
