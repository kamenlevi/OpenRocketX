package x.api;

import io.javalin.http.Context;
import io.javalin.websocket.WsConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Run a flight simulation. The real implementation calls
 * info.openrocket.core.simulation.SimulationEngine through x.bridge.
 *
 * Without core: produce a smooth parabolic toy curve so the frontend can be
 * developed end-to-end. The WebSocket protocol is the eventual one; the
 * payload shape is what core integration will produce verbatim.
 */
public class SimController {

    public void run(Context ctx) {
        // Synchronous result (whole flight in one response). Useful for
        // export to CSV and for non-streaming clients.
        List<Map<String, Object>> samples = simulate(120, 0.05);
        Map<String, Object> last = samples.get(samples.size() - 1);
        ctx.json(Map.of(
                "samples", samples,
                "summary", Map.of(
                        "apogeeM", peak(samples, "altitude"),
                        "maxVelMs", peak(samples, "speed"),
                        "burnoutS", 1.6,
                        "flightTimeS", last.get("t")
                ),
                "stub", true
        ));
    }

    public void stream(WsConfig ws) {
        ws.onConnect(ctx -> {
            // Stream the same toy trajectory in real-time-ish chunks.
            new Thread(() -> {
                try {
                    for (Map<String, Object> sample : simulate(120, 0.05)) {
                        if (!ctx.session.isOpen()) return;
                        ctx.send(Json.M.writeValueAsString(sample));
                        Thread.sleep(20);
                    }
                    if (ctx.session.isOpen()) {
                        ctx.send("{\"done\": true}");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    // log + close handled by Javalin
                }
            }, "sim-stream").start();
        });
    }

    private static List<Map<String, Object>> simulate(double durationS, double dtS) {
        // Simple kinematic toy: 3s thrust phase to ~150 m/s, then ballistic.
        java.util.ArrayList<Map<String, Object>> out = new java.util.ArrayList<>();
        double g = 9.81;
        double thrustAccel = 50.0;
        double burnT = 3.0;
        double t = 0;
        double v = 0, h = 0;
        for (int i = 0; t <= durationS && h >= 0; i++) {
            double a = (t < burnT ? thrustAccel - g : -g);
            v += a * dtS;
            h += v * dtS;
            if (h < 0) { h = 0; v = 0; }
            Map<String, Object> sample = new HashMap<>();
            sample.put("t", round(t));
            sample.put("altitude", round(h));
            sample.put("speed", round(Math.abs(v)));
            sample.put("velocity", new double[]{0, 0, round(v)});
            sample.put("acceleration", round(a));
            out.add(sample);
            t += dtS;
        }
        return out;
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private static double peak(List<Map<String, Object>> samples, String key) {
        double max = 0;
        for (Map<String, Object> s : samples) {
            double v = ((Number) s.get(key)).doubleValue();
            if (v > max) max = v;
        }
        return max;
    }
}
