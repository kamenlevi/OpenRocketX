package x.model;

/**
 * Profile families OpenRocket supports for nose cones and transitions.
 * The numeric `param` on Transition tunes families that have a free parameter
 * (Haack series C, power series n, parabolic K).
 */
public enum ShapeType {
    CONICAL,
    OGIVE,
    ELLIPSOID,
    PARABOLIC,
    POWER,
    HAACK
}
