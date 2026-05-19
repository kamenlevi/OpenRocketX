package x.model;

/**
 * A symmetric set of fins around the body. Coordinates of finPoints are
 * (x, y) in the fin's local plane: x is along the body axis (root chord at
 * y=0), y is the radial direction outward from the body surface. Points
 * must form a closed CCW polygon when listed.
 *
 * Example trapezoidal fin (root=10cm, tip=4cm, sweep=3cm, height=6cm):
 *   {(0,0), (10,0), (10-3, 6), (10-3-4, 6)}  // root → trailing → tip → leading
 *
 * For free-form fins, drop any planar polygon in.
 */
public record FinSetSpec(
        int count,
        double thickness,
        double cantAngleRad,
        double[][] finPoints,
        double mountRadius
) {
    public FinSetSpec {
        if (count < 1 || count > 16) {
            throw new IllegalArgumentException("count must be in [1, 16]");
        }
        if (thickness <= 0) throw new IllegalArgumentException("thickness must be > 0");
        if (finPoints == null || finPoints.length < 3) {
            throw new IllegalArgumentException("need at least 3 fin points");
        }
        for (double[] p : finPoints) {
            if (p == null || p.length != 2) {
                throw new IllegalArgumentException("each fin point must be [x,y]");
            }
        }
        if (mountRadius <= 0) throw new IllegalArgumentException("mountRadius must be > 0");
    }
}
