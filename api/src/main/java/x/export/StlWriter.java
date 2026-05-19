package x.export;

import x.geom.Triangle;
import x.geom.TriangleMesh;
import x.geom.Vec3;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/**
 * Binary STL writer — the format every slicer reads.
 *
 * Format:
 *   80-byte header (anything, conventionally ASCII tag)
 *   uint32 little-endian: number of triangles
 *   for each triangle:
 *     3 × float32 normal (nx, ny, nz)
 *     3 × float32 v0 (x,y,z)
 *     3 × float32 v1
 *     3 × float32 v2
 *     uint16 attribute byte count (0)
 * Total per-triangle bytes: 50.
 */
public final class StlWriter {

    private StlWriter() {}

    public static byte[] toBinary(TriangleMesh mesh, String headerTag) {
        int n = mesh.triangleCount();
        int totalSize = 84 + 50 * n;
        ByteBuffer buf = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN);

        byte[] header = new byte[80];
        byte[] tag = ("OpenRocketX " + headerTag).getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(tag, 0, header, 0, Math.min(tag.length, 80));
        buf.put(header);

        buf.putInt(n);

        for (int i = 0; i < n; i++) {
            Triangle t = mesh.triangles().get(i);
            Vec3 a = mesh.vertices().get(t.a());
            Vec3 b = mesh.vertices().get(t.b());
            Vec3 c = mesh.vertices().get(t.c());
            Vec3 normal = mesh.normalOf(i);

            buf.putFloat((float) normal.x());
            buf.putFloat((float) normal.y());
            buf.putFloat((float) normal.z());

            buf.putFloat((float) a.x()); buf.putFloat((float) a.y()); buf.putFloat((float) a.z());
            buf.putFloat((float) b.x()); buf.putFloat((float) b.y()); buf.putFloat((float) b.z());
            buf.putFloat((float) c.x()); buf.putFloat((float) c.y()); buf.putFloat((float) c.z());

            buf.putShort((short) 0);
        }

        return buf.array();
    }

    public static void writeBinary(TriangleMesh mesh, OutputStream out, String tag) throws IOException {
        out.write(toBinary(mesh, tag));
    }

    /**
     * ASCII STL is human-readable but ~5x larger than binary; useful for
     * debugging. Tools accept both.
     */
    public static String toAscii(TriangleMesh mesh, String solidName) {
        StringBuilder sb = new StringBuilder(mesh.triangleCount() * 200);
        sb.append("solid ").append(solidName).append('\n');
        Locale L = Locale.US;
        for (int i = 0; i < mesh.triangleCount(); i++) {
            Triangle t = mesh.triangles().get(i);
            Vec3 a = mesh.vertices().get(t.a());
            Vec3 b = mesh.vertices().get(t.b());
            Vec3 c = mesh.vertices().get(t.c());
            Vec3 n = mesh.normalOf(i);
            sb.append(String.format(L, "facet normal %e %e %e%n", n.x(), n.y(), n.z()));
            sb.append("  outer loop\n");
            sb.append(String.format(L, "    vertex %e %e %e%n", a.x(), a.y(), a.z()));
            sb.append(String.format(L, "    vertex %e %e %e%n", b.x(), b.y(), b.z()));
            sb.append(String.format(L, "    vertex %e %e %e%n", c.x(), c.y(), c.z()));
            sb.append("  endloop\n");
            sb.append("endfacet\n");
        }
        sb.append("endsolid ").append(solidName).append('\n');
        return sb.toString();
    }

    public static void writeAscii(TriangleMesh mesh, OutputStream out, String solidName) throws IOException {
        try (Writer w = new OutputStreamWriter(out, StandardCharsets.US_ASCII)) {
            w.write(toAscii(mesh, solidName));
        }
    }
}
