package x.api;

import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServerIntegrationTest {

    Javalin app;
    HttpClient http;
    String base;

    @BeforeAll
    void boot() {
        app = Server.build().start("127.0.0.1", 0);
        base = "http://127.0.0.1:" + app.port();
        http = HttpClient.newHttpClient();
    }

    @AfterAll
    void stop() {
        if (app != null) app.stop();
    }

    @Test
    void healthz() throws Exception {
        HttpResponse<String> resp = http.send(
                HttpRequest.newBuilder(URI.create(base + "/healthz")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertEquals("ok", resp.body());
    }

    @Test
    void motorListStubResponds() throws Exception {
        HttpResponse<String> resp = http.send(
                HttpRequest.newBuilder(URI.create(base + "/api/motors")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("Estes-D12-3"));
    }

    @Test
    void exportStlEndToEnd() throws Exception {
        String design = """
            {
              "name": "test",
              "stages": [{
                "name": "core",
                "sections": [
                  {"kind": "nose", "spec": {"shape": "OGIVE", "length": 0.1, "baseRadius": 0.025, "shapeParam": 1.0, "wallThickness": 0.0015}},
                  {"kind": "body", "spec": {"length": 0.4, "outerRadius": 0.025, "innerRadius": 0.024}}
                ],
                "fins": [
                  {"spec": {"count": 3, "thickness": 0.003, "cantAngleRad": 0.0,
                            "finPoints": [[0,0],[0.10,0],[0.07,0.06],[0.03,0.06]],
                            "mountRadius": 0.025},
                   "xOffset": 0.38}
                ]
              }]
            }
            """;
        HttpResponse<byte[]> resp = http.send(
                HttpRequest.newBuilder(URI.create(base + "/api/export/stl"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(design)).build(),
                HttpResponse.BodyHandlers.ofByteArray());

        assertEquals(200, resp.statusCode(),
                "expected 200; got " + resp.statusCode() + " body=" + new String(resp.body()));
        byte[] stl = resp.body();
        assertTrue(stl.length > 84, "STL must include header + triangles");
        ByteBuffer buf = ByteBuffer.wrap(stl, 80, 4).order(ByteOrder.LITTLE_ENDIAN);
        int triCount = buf.getInt();
        assertTrue(triCount > 1000, "real rocket should produce many triangles");
        assertEquals(stl.length, 84 + 50L * triCount);

        String header = new String(stl, 0, 11, java.nio.charset.StandardCharsets.US_ASCII);
        assertEquals("OpenRocketX", header);
    }

    @Test
    void designPreviewReturnsMeshJson() throws Exception {
        String design = """
            {
              "name": "preview",
              "stages": [{
                "name": "core",
                "sections": [{"kind": "body", "spec": {"length": 0.2, "outerRadius": 0.025, "innerRadius": 0.024}}],
                "fins": []
              }]
            }
            """;
        HttpResponse<String> resp = http.send(
                HttpRequest.newBuilder(URI.create(base + "/api/designs/preview"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(design)).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode(), resp.body());
        assertTrue(resp.body().contains("\"positions\""));
        assertTrue(resp.body().contains("\"indices\""));
        assertTrue(resp.body().contains("\"manifold\":true"));
    }
}
