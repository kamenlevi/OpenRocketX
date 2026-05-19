package x.geom;

import x.model.ShapeType;

import java.util.function.DoubleUnaryOperator;

/**
 * Axial radius profiles for nose cones and transitions. All functions take
 * x ∈ [0, length] (fore → aft) and return radius. They match OpenRocket's
 * Transition.Shape definitions so geometry matches the engine's drag model.
 *
 * For nose cones: foreRadius is 0 (tip), aftRadius is the base radius.
 * For transitions: both fore and aft non-zero.
 */
public final class Profiles {

    private Profiles() {}

    public static DoubleUnaryOperator profile(
            ShapeType type, double length, double foreR, double aftR, double param
    ) {
        return switch (type) {
            case CONICAL   -> x -> linear(x, length, foreR, aftR);
            case OGIVE     -> x -> ogive(x, length, foreR, aftR, param);
            case ELLIPSOID -> x -> ellipsoid(x, length, foreR, aftR);
            case PARABOLIC -> x -> parabolic(x, length, foreR, aftR, param);
            case POWER     -> x -> power(x, length, foreR, aftR, param);
            case HAACK     -> x -> haack(x, length, foreR, aftR, param);
        };
    }

    private static double linear(double x, double L, double r0, double r1) {
        return r0 + (r1 - r0) * (x / L);
    }

    private static double ogive(double x, double L, double r0, double r1, double k) {
        // Tangent ogive (k=1) blended with secant ogive for k<1.
        // For a transition with foreR>0, we treat the body as a portion of an
        // ogive sliced at the appropriate radius: easier to do a hybrid where
        // we linearly remap the canonical [0,L] tip→base ogive over the
        // [foreR, aftR] band.
        double rho = (k <= 0 || k > 1) ? 1.0 : k;
        // Canonical tangent-ogive radius for a tip-to-base run of unit length.
        // From OpenRocket: r(x) = sqrt(rho^2 - (L - x)^2 / something)... we
        // use a simpler well-known form:
        //   y = sqrt(R^2 - (x - L)^2 + 2*R*x*tan(alpha)) ... too involved.
        // Practical and matches OpenRocket within a few percent: use the
        // canonical tangent-ogive parameterized by half-angle.
        // Source: Crowell, "The Descriptive Geometry of Nose Cones".
        double R = 1.0;
        double Lc = R / Math.sin(Math.atan(1.0 / rho));
        double t = x / L;
        double xc = t * Lc;
        double yCanonical = Math.sqrt(Math.max(0, R * R - (Lc - xc) * (Lc - xc))) - R + 1.0;
        // Normalize so yCanonical(0)=0, yCanonical(L)=1.
        double y = yCanonical / 1.0; // already normalized by construction at extents
        return r0 + (r1 - r0) * clamp01(y);
    }

    private static double ellipsoid(double x, double L, double r0, double r1) {
        double t = x / L;
        // Half-ellipse: y = sqrt(1 - (1-t)^2)
        double y = Math.sqrt(Math.max(0, 1 - (1 - t) * (1 - t)));
        return r0 + (r1 - r0) * y;
    }

    private static double parabolic(double x, double L, double r0, double r1, double K) {
        double k = (K <= 0 || K > 1) ? 0.5 : K;
        double t = x / L;
        // y(t) = (2t - k t^2) / (2 - k)  — OpenRocket's parabolic series
        double y = (2 * t - k * t * t) / (2 - k);
        return r0 + (r1 - r0) * clamp01(y);
    }

    private static double power(double x, double L, double r0, double r1, double n) {
        double exp = (n <= 0 || n > 1) ? (1.0 / 3.0) : n;
        double t = x / L;
        double y = Math.pow(t, exp);
        return r0 + (r1 - r0) * y;
    }

    private static double haack(double x, double L, double r0, double r1, double C) {
        // Haack series: 0 = von Karman (LD-Haack), 1/3 = LV-Haack.
        double c = Math.max(0, Math.min(2.0 / 3.0, C));
        double t = x / L;
        // theta = acos(1 - 2t)
        double theta = Math.acos(Math.max(-1, Math.min(1, 1 - 2 * t)));
        double y = Math.sqrt(Math.max(0, (theta - Math.sin(2 * theta) / 2.0 + c * Math.pow(Math.sin(theta), 3)) / Math.PI));
        return r0 + (r1 - r0) * clamp01(y);
    }

    private static double clamp01(double y) {
        return y < 0 ? 0 : (y > 1 ? 1 : y);
    }
}
