package x.geom;

import x.model.FinSetSpec;

/**
 * Build the mesh for a fin set. Each fin is a prism: extrude the 2D
 * fin polygon along the body's tangential direction (±thickness/2),
 * then translate radially out from mountRadius and rotate around the
 * body axis to produce `count` symmetric copies.
 *
 * Fin polygon coordinates: x along the rocket axis (root at y=0), y
 * radially outward. We extrude in the z direction (tangent to the body
 * at that azimuth) then rotate the whole prism around +X.
 *
 * Side caps are added so each fin is a closed, manifold solid; the meshes
 * sit on the outer body surface, slicers will handle the union with the
 * body tube.
 */
public final class FinExtrude {

    private FinExtrude() {}

    public static TriangleMesh build(FinSetSpec spec, double xOffset) {
        TriangleMesh all = new TriangleMesh();
        for (int i = 0; i < spec.count(); i++) {
            double phi = 2.0 * Math.PI * i / spec.count();
            TriangleMesh fin = singleFin(spec, xOffset, phi);
            all.merge(fin);
        }
        return all;
    }

    private static TriangleMesh singleFin(FinSetSpec spec, double xOffset, double phi) {
        TriangleMesh m = new TriangleMesh();
        double[][] pts = spec.finPoints();
        int n = pts.length;
        double halfT = spec.thickness() / 2.0;
        double r0 = spec.mountRadius();

        // Build two parallel rings of vertices (front/back faces of the prism).
        // Local coordinates before rotation:
        //   x_local = pts[i][0] + xOffset      (axial)
        //   y_local = pts[i][1] + r0           (radial offset from axis)
        //   z_local = ±halfT                   (tangential, the extrude axis)
        // Then rotate (y,z) by phi around the x axis.
        int[] front = new int[n];
        int[] back  = new int[n];
        double cosP = Math.cos(phi);
        double sinP = Math.sin(phi);
        for (int i = 0; i < n; i++) {
            double xa = pts[i][0] + xOffset;
            double yr = pts[i][1] + r0;
            // front (z = +halfT)
            double yf = yr * cosP - halfT * sinP;
            double zf = yr * sinP + halfT * cosP;
            front[i] = m.addVertex(xa, yf, zf);
            // back (z = -halfT)
            double yb = yr * cosP - (-halfT) * sinP;
            double zb = yr * sinP + (-halfT) * cosP;
            back[i]  = m.addVertex(xa, yb, zb);
        }

        // Two flat faces of the prism. Use a fan triangulation (assumes convex-
        // ish polygons; trapezoids and ellipses are fine, free-form may be
        // non-convex — for V1 we ship fan and document the limitation).
        // Front face: outward normal points in +z_local direction (after rotate).
        for (int i = 1; i < n - 1; i++) {
            m.addTriangle(front[0], front[i], front[i + 1]);
        }
        // Back face: reverse winding.
        for (int i = 1; i < n - 1; i++) {
            m.addTriangle(back[0], back[i + 1], back[i]);
        }

        // Side band connecting front[i]..back[i] around the perimeter.
        for (int i = 0; i < n; i++) {
            int in = (i + 1) % n;
            int a = front[i];
            int b = front[in];
            int c = back[in];
            int d = back[i];
            m.addQuad(a, b, c, d);
        }

        return m;
    }
}
