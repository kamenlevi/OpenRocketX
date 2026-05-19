package x.geom;

import java.util.function.DoubleUnaryOperator;

/**
 * Body-of-revolution mesher. The profile function maps axial position
 * x ∈ [0, length] → radius r(x) ≥ 0. The result is a closed surface
 * revolved around the +X axis, capped at each end where r(x) > 0 and
 * crowned to a point where r(x) == 0 (so nose cones are properly closed).
 *
 * Output is positioned with the fore end at x=xOffset and the aft end at
 * x=xOffset+length.
 */
public final class Revolve {

    private Revolve() {}

    public static TriangleMesh build(
            DoubleUnaryOperator profile,
            double length,
            int axialSegments,
            int radialSegments,
            double xOffset
    ) {
        if (axialSegments < 2) throw new IllegalArgumentException("axialSegments >= 2");
        if (radialSegments < 3) throw new IllegalArgumentException("radialSegments >= 3");

        TriangleMesh mesh = new TriangleMesh();

        int[][] ringIdx = new int[axialSegments + 1][radialSegments];
        boolean[] degenerate = new boolean[axialSegments + 1];
        double[] xs = new double[axialSegments + 1];
        double[] rs = new double[axialSegments + 1];

        for (int i = 0; i <= axialSegments; i++) {
            double t = (double) i / axialSegments;
            double x = t * length + xOffset;
            double r = Math.max(0, profile.applyAsDouble(t * length));
            xs[i] = x;
            rs[i] = r;
            degenerate[i] = r < 1e-9;
            if (degenerate[i]) {
                int center = mesh.addVertex(x, 0, 0);
                for (int j = 0; j < radialSegments; j++) ringIdx[i][j] = center;
            } else {
                for (int j = 0; j < radialSegments; j++) {
                    double theta = 2.0 * Math.PI * j / radialSegments;
                    double y = r * Math.cos(theta);
                    double z = r * Math.sin(theta);
                    ringIdx[i][j] = mesh.addVertex(x, y, z);
                }
            }
        }

        // Side faces (quads between consecutive rings → 2 tris each).
        for (int i = 0; i < axialSegments; i++) {
            boolean d0 = degenerate[i];
            boolean d1 = degenerate[i + 1];
            for (int j = 0; j < radialSegments; j++) {
                int jn = (j + 1) % radialSegments;
                int a = ringIdx[i][j];
                int b = ringIdx[i][jn];
                int c = ringIdx[i + 1][jn];
                int d = ringIdx[i + 1][j];
                if (d0 && d1) continue;
                if (d0) {
                    // Tip at low-x end: fan from a (== all of ring i) to ring i+1.
                    mesh.addTriangle(a, c, d);
                } else if (d1) {
                    // Tip at high-x end.
                    mesh.addTriangle(a, b, c);
                } else {
                    mesh.addQuad(a, b, c, d);
                }
            }
        }

        // End caps if non-degenerate ring at the boundary. CCW from outside:
        // fore cap normal is -X (so wind CW as seen from +X), aft cap normal
        // is +X (wind CCW as seen from +X).
        if (!degenerate[0]) {
            int center = mesh.addVertex(xs[0], 0, 0);
            for (int j = 0; j < radialSegments; j++) {
                int jn = (j + 1) % radialSegments;
                mesh.addTriangle(center, ringIdx[0][jn], ringIdx[0][j]);
            }
        }
        if (!degenerate[axialSegments]) {
            int center = mesh.addVertex(xs[axialSegments], 0, 0);
            for (int j = 0; j < radialSegments; j++) {
                int jn = (j + 1) % radialSegments;
                mesh.addTriangle(center, ringIdx[axialSegments][j], ringIdx[axialSegments][jn]);
            }
        }

        return mesh;
    }
}
