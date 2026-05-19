package x.geom;

public record Vec3(double x, double y, double z) {
    public Vec3 add(Vec3 o) { return new Vec3(x + o.x, y + o.y, z + o.z); }
    public Vec3 sub(Vec3 o) { return new Vec3(x - o.x, y - o.y, z - o.z); }
    public Vec3 scale(double s) { return new Vec3(x * s, y * s, z * s); }
    public double dot(Vec3 o) { return x * o.x + y * o.y + z * o.z; }
    public Vec3 cross(Vec3 o) {
        return new Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
    }
    public double length() { return Math.sqrt(x * x + y * y + z * z); }
    public Vec3 normalized() {
        double L = length();
        return L > 0 ? new Vec3(x / L, y / L, z / L) : new Vec3(0, 0, 0);
    }
}
