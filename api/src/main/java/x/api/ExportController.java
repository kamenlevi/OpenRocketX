package x.api;

import io.javalin.http.Context;
import x.export.GltfWriter;
import x.export.StlWriter;
import x.geom.RocketMesh;
import x.geom.TriangleMesh;
import x.model.RocketSpec;

/**
 * Export a posted rocket design as STL or GLB.
 *
 * Request body: JSON matching x.model.RocketSpec (records map 1:1 to JSON).
 * Response body: binary STL / GLB with the appropriate content type and a
 * Content-Disposition: attachment hint for direct-download UX.
 */
public class ExportController {

    public void stl(Context ctx) throws Exception {
        RocketSpec spec = Json.M.readValue(ctx.bodyInputStream(), RocketSpec.class);
        TriangleMesh mesh = RocketMesh.build(spec);
        warnIfNonManifold(ctx, mesh);
        byte[] data = StlWriter.toBinary(mesh, sanitize(spec.name()));
        ctx.contentType("application/octet-stream");
        ctx.header("Content-Disposition",
                "attachment; filename=\"" + sanitize(spec.name()) + ".stl\"");
        ctx.result(data);
    }

    public void gltf(Context ctx) throws Exception {
        RocketSpec spec = Json.M.readValue(ctx.bodyInputStream(), RocketSpec.class);
        TriangleMesh mesh = RocketMesh.build(spec);
        warnIfNonManifold(ctx, mesh);
        byte[] data = GltfWriter.toGlb(mesh);
        ctx.contentType("model/gltf-binary");
        ctx.header("Content-Disposition",
                "attachment; filename=\"" + sanitize(spec.name()) + ".glb\"");
        ctx.result(data);
    }

    private static void warnIfNonManifold(Context ctx, TriangleMesh mesh) {
        if (!mesh.isEdgeManifold()) {
            // Surfacing this so callers can warn the user before sending to
            // a slicer; we still return the mesh because some viewers tolerate
            // non-manifold input.
            ctx.header("X-OpenRocketX-Warning", "non-manifold-mesh");
        }
    }

    private static String sanitize(String name) {
        if (name == null || name.isBlank()) return "rocket";
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
