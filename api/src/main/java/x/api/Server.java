package x.api;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenRocketX backend. Binds 127.0.0.1 only — this is a local engine for the
 * Tauri frontend, not a public server. The port is chosen by the OS unless
 * the OPENROCKETX_PORT env var is set; the chosen port is printed on the
 * first line of stdout in the form "PORT=NNNNN" so the Tauri sidecar can
 * read it.
 *
 * Routes:
 *   GET  /healthz                  liveness
 *   GET  /api/motors               list motors (stub until core integration)
 *   POST /api/designs/preview      design JSON → mesh JSON for Three.js
 *   POST /api/export/stl           design JSON → application/octet-stream STL
 *   POST /api/export/gltf          design JSON → model/gltf-binary GLB
 *   POST /api/simulate             design JSON → sim result (stub)
 *   WS   /api/simulate/stream      live sim events (stub)
 */
public class Server {

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) {
        int requestedPort = parsePort(System.getenv("OPENROCKETX_PORT"));
        Javalin app = build();
        app.start("127.0.0.1", requestedPort);
        int port = app.port();
        // First line of stdout: PORT=NNNNN. The Tauri sidecar reads this.
        System.out.println("PORT=" + port);
        System.out.flush();
        LOG.info("OpenRocketX engine listening on 127.0.0.1:{}", port);
    }

    public static Javalin build() {
        Javalin app = Javalin.create(cfg -> {
            cfg.showJavalinBanner = false;
            cfg.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.allowHost("http://127.0.0.1");
                    it.allowHost("http://localhost");
                    it.allowHost("tauri://localhost");
                    it.exposeHeader("Content-Disposition");
                });
            });
            cfg.requestLogger.http((ctx, ms) ->
                LOG.info("{} {} → {} ({} ms)", ctx.method(), ctx.path(), ctx.status(), ms));
        });

        app.get("/healthz", ctx -> ctx.result("ok"));

        DesignController designs = new DesignController();
        MotorController motors = new MotorController();
        ExportController exports = new ExportController();
        SimController sim = new SimController();

        app.post("/api/designs/preview", designs::preview);
        app.post("/api/designs/validate", designs::validate);

        app.get("/api/motors", motors::list);
        app.get("/api/motors/{id}", motors::get);

        app.post("/api/export/stl", exports::stl);
        app.post("/api/export/gltf", exports::gltf);

        app.post("/api/simulate", sim::run);
        app.ws("/api/simulate/stream", sim::stream);

        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            LOG.warn("400: {}", e.getMessage());
            ctx.status(400).result(e.getMessage());
        });
        app.exception(Exception.class, (e, ctx) -> {
            LOG.error("500 on {}", ctx.path(), e);
            ctx.status(500).result(e.getMessage() == null ? "internal error" : e.getMessage());
        });

        return app;
    }

    private static int parsePort(String env) {
        if (env == null || env.isEmpty()) return 0;
        try {
            int p = Integer.parseInt(env);
            return (p >= 0 && p <= 65535) ? p : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
