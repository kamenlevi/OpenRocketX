package x.model;

/** Right circular cylinder, axis +X. */
public record BodyTubeSpec(
        double length,
        double outerRadius,
        double innerRadius
) {
    public BodyTubeSpec {
        if (length <= 0) throw new IllegalArgumentException("length must be > 0");
        if (outerRadius <= 0) throw new IllegalArgumentException("outerRadius must be > 0");
        if (innerRadius < 0 || innerRadius >= outerRadius) {
            throw new IllegalArgumentException("innerRadius must be in [0, outerRadius)");
        }
    }
}
