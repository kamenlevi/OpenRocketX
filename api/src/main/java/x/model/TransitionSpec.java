package x.model;

/**
 * Tapered body section connecting two radii (fore and aft). When fore > aft
 * this is a boattail; when fore < aft this is a shoulder/reducer.
 */
public record TransitionSpec(
        ShapeType shape,
        double length,
        double foreRadius,
        double aftRadius,
        double shapeParam
) {
    public TransitionSpec {
        if (length <= 0) throw new IllegalArgumentException("length must be > 0");
        if (foreRadius <= 0 || aftRadius <= 0) {
            throw new IllegalArgumentException("radii must be > 0");
        }
    }
}
