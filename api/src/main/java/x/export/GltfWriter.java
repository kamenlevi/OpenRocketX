package x.export;

import x.geom.Triangle;
import x.geom.TriangleMesh;
import x.geom.Vec3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Minimal GLB (binary glTF 2.0) writer. Outputs a single mesh primitive with
 * POSITION + NORMAL attributes and TRIANGLES topology. No materials, textures,
 * or animations — sufficient for round-tripping geometry to Three.js or any
 * glTF viewer.
 *
 * GLB structure (LE everywhere):
 *   header (12 bytes):  magic 0x46546c67 ('glTF') | version=2 | totalLength
 *   chunk 0 (JSON):     length | type 0x4e4f534a ('JSON') | padded JSON bytes
 *   chunk 1 (BIN):      length | type 0x004e4942 ('BIN\0') | padded binary
 *
 * Reference: https://registry.khronos.org/glTF/specs/2.0/glTF-2.0.html
 */
public final class GltfWriter {

    private static final int MAGIC = 0x46546C67;        // 'glTF'
    private static final int VERSION = 2;
    private static final int CHUNK_JSON = 0x4E4F534A;   // 'JSON'
    private static final int CHUNK_BIN  = 0x004E4942;   // 'BIN\0'

    private GltfWriter() {}

    public static byte[] toGlb(TriangleMesh mesh) {
        int vCount = mesh.vertexCount();
        int tCount = mesh.triangleCount();

        // Build the binary buffer: POSITION (vec3 float32), NORMAL (vec3 float32),
        // INDICES (uint32). glTF requires alignment per element so we order by
        // size (4-byte floats and uint32) and the chunk pads to 4-byte multiples.
        int positionLen = vCount * 3 * 4;
        int normalLen   = vCount * 3 * 4;
        int indicesLen  = tCount * 3 * 4;
        int binLen = positionLen + normalLen + indicesLen;
        int binPadded = align4(binLen);

        ByteBuffer bin = ByteBuffer.allocate(binPadded).order(ByteOrder.LITTLE_ENDIAN);

        // POSITION
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        for (Vec3 v : mesh.vertices()) {
            float fx = (float) v.x(), fy = (float) v.y(), fz = (float) v.z();
            bin.putFloat(fx); bin.putFloat(fy); bin.putFloat(fz);
            if (fx < minX) minX = fx; if (fy < minY) minY = fy; if (fz < minZ) minZ = fz;
            if (fx > maxX) maxX = fx; if (fy > maxY) maxY = fy; if (fz > maxZ) maxZ = fz;
        }

        // NORMAL — average face normals per vertex.
        float[] nx = new float[vCount], ny = new float[vCount], nz = new float[vCount];
        for (int i = 0; i < tCount; i++) {
            Triangle t = mesh.triangles().get(i);
            Vec3 n = mesh.normalOf(i);
            float fx = (float) n.x(), fy = (float) n.y(), fz = (float) n.z();
            nx[t.a()] += fx; ny[t.a()] += fy; nz[t.a()] += fz;
            nx[t.b()] += fx; ny[t.b()] += fy; nz[t.b()] += fz;
            nx[t.c()] += fx; ny[t.c()] += fy; nz[t.c()] += fz;
        }
        for (int i = 0; i < vCount; i++) {
            double L = Math.sqrt(nx[i] * nx[i] + ny[i] * ny[i] + nz[i] * nz[i]);
            if (L > 0) { nx[i] /= L; ny[i] /= L; nz[i] /= L; }
            bin.putFloat(nx[i]); bin.putFloat(ny[i]); bin.putFloat(nz[i]);
        }

        // INDICES
        for (Triangle t : mesh.triangles()) {
            bin.putInt(t.a()); bin.putInt(t.b()); bin.putInt(t.c());
        }

        while (bin.position() < binPadded) bin.put((byte) 0);

        // JSON manifest.
        String json = String.format(java.util.Locale.US, """
            {
              "asset": {"version": "2.0", "generator": "OpenRocketX"},
              "scene": 0,
              "scenes": [{"nodes": [0]}],
              "nodes": [{"mesh": 0}],
              "meshes": [{"primitives": [{"attributes": {"POSITION": 0, "NORMAL": 1}, "indices": 2, "mode": 4}]}],
              "buffers": [{"byteLength": %d}],
              "bufferViews": [
                {"buffer": 0, "byteOffset": 0, "byteLength": %d, "target": 34962},
                {"buffer": 0, "byteOffset": %d, "byteLength": %d, "target": 34962},
                {"buffer": 0, "byteOffset": %d, "byteLength": %d, "target": 34963}
              ],
              "accessors": [
                {"bufferView": 0, "componentType": 5126, "count": %d, "type": "VEC3", "min": [%f, %f, %f], "max": [%f, %f, %f]},
                {"bufferView": 1, "componentType": 5126, "count": %d, "type": "VEC3"},
                {"bufferView": 2, "componentType": 5125, "count": %d, "type": "SCALAR"}
              ]
            }
            """,
            binLen,
            positionLen,
            positionLen, normalLen,
            positionLen + normalLen, indicesLen,
            vCount, minX, minY, minZ, maxX, maxY, maxZ,
            vCount,
            tCount * 3
        );

        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        int jsonPadded = align4(jsonBytes.length);

        int totalLength = 12 + 8 + jsonPadded + 8 + binPadded;
        ByteBuffer out = ByteBuffer.allocate(totalLength).order(ByteOrder.LITTLE_ENDIAN);

        out.putInt(MAGIC);
        out.putInt(VERSION);
        out.putInt(totalLength);

        out.putInt(jsonPadded);
        out.putInt(CHUNK_JSON);
        out.put(jsonBytes);
        for (int i = jsonBytes.length; i < jsonPadded; i++) out.put((byte) 0x20); // pad JSON with spaces

        out.putInt(binPadded);
        out.putInt(CHUNK_BIN);
        out.put(bin.array());

        return out.array();
    }

    private static int align4(int n) {
        return (n + 3) & ~3;
    }
}
