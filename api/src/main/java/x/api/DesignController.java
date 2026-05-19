package x.api;

import io.javalin.http.Context;
import x.geom.RocketMesh;
import x.geom.Triangle;
import x.geom.TriangleMesh;
import x.geom.Vec3;
import x.model.RocketSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Endpoints the designer page calls as the user edits a rocket. preview()
 * returns a JSON mesh payload Three.js can ingest directly (positions +
 * indices); the same parametric model is rendered both ways so the 3D
 * preview is always consistent with the eventual STL export.
 */
public class DesignController {

    public void preview(Context ctx) throws Exception {
        RocketSpec spec = Json.M.readValue(ctx.bodyInputStream(), RocketSpec.class);
        TriangleMesh mesh = RocketMesh.build(spec);
        ctx.contentType("application/json");
        ctx.result(Json.M.writeValueAsBytes(meshPayload(mesh)));
    }

    public void validate(Context ctx) throws Exception {
        RocketSpec spec = Json.M.readValue(ctx.bodyInputStream(), RocketSpec.class);
        TriangleMesh mesh = RocketMesh.build(spec);
        ctx.json(Map.of(
                "name", spec.name(),
                "stageCount", spec.stages().size(),
                "vertexCount", mesh.vertexCount(),
                "triangleCount", mesh.triangleCount(),
                "manifold", mesh.isEdgeManifold()
        ));
    }

    /** Flat float arrays so Three.js can build a BufferGeometry directly. */
    private static Map<String, Object> meshPayload(TriangleMesh mesh) {
        float[] positions = new float[mesh.vertexCount() * 3];
        for (int i = 0; i < mesh.vertexCount(); i++) {
            Vec3 v = mesh.vertices().get(i);
            positions[3 * i]     = (float) v.x();
            positions[3 * i + 1] = (float) v.y();
            positions[3 * i + 2] = (float) v.z();
        }
        int[] indices = new int[mesh.triangleCount() * 3];
        for (int i = 0; i < mesh.triangleCount(); i++) {
            Triangle t = mesh.triangles().get(i);
            indices[3 * i]     = t.a();
            indices[3 * i + 1] = t.b();
            indices[3 * i + 2] = t.c();
        }
        return Map.of(
                "positions", positions,
                "indices", indices,
                "manifold", mesh.isEdgeManifold()
        );
    }
}
