package x.model;

/**
 * Nose cone at the top of the rocket. Axis is +X; the tip sits at x=0 and
 * the base sits at x=length with radius=baseRadius.
 *
 * For OGIVE/POWER/PARABOLIC/HAACK families, shapeParam tunes the profile.
 * Sensible defaults: 1.0 (tangent ogive), 0.5 (parabolic), 1/3 (power),
 * 0.0 (von Karman in Haack).
 */
public record NoseConeSpec(
        ShapeType shape,
        double length,
        double baseRadius,
        double shapeParam,
        double wallThickness
) {
    public NoseConeSpec {
        if (length <= 0) throw new IllegalArgumentException("length must be > 0");
        if (baseRadius <= 0) throw new IllegalArgumentException("baseRadius must be > 0");
    }
}
