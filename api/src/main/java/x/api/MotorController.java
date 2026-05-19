package x.api;

import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

/**
 * Motor database queries. The real implementation reads OpenRocket's
 * Thrustcurve SQLite DB (core/src/main/resources/datafiles/thrustcurves)
 * via the upstream ThrustCurveMotor / MotorDigest classes. That's wired up
 * in x.bridge when the project is built with -PwithCore=true.
 *
 * In the standalone build we return a stub list so the UI can be developed
 * against a real-shaped API.
 */
public class MotorController {

    public void list(Context ctx) {
        String impulseClass = ctx.queryParam("class"); // A..O
        String manufacturer = ctx.queryParam("mfg");
        ctx.json(Map.of(
                "filter", Map.of(
                        "class", impulseClass == null ? "" : impulseClass,
                        "manufacturer", manufacturer == null ? "" : manufacturer
                ),
                "motors", List.of(
                        // Stub data shaped like the wire format we'll keep
                        // when the core engine is wired in.
                        Map.of(
                                "id", "Estes-D12-3",
                                "manufacturer", "Estes",
                                "designation", "D12",
                                "delay", "3",
                                "diameter", 0.024,
                                "length", 0.070,
                                "totalImpulseNs", 16.84,
                                "averageThrustN", 12.0,
                                "burnTimeS", 1.6,
                                "impulseClass", "D"
                        ),
                        Map.of(
                                "id", "AeroTech-H128W",
                                "manufacturer", "AeroTech",
                                "designation", "H128W",
                                "delay", "M",
                                "diameter", 0.029,
                                "length", 0.124,
                                "totalImpulseNs", 176.0,
                                "averageThrustN", 128.0,
                                "burnTimeS", 1.4,
                                "impulseClass", "H"
                        )
                ),
                "stub", true,
                "note", "Real database wired via x.bridge when built with -PwithCore=true"
        ));
    }

    public void get(Context ctx) {
        String id = ctx.pathParam("id");
        ctx.json(Map.of(
                "id", id,
                "stub", true,
                "thrustcurve", List.of(
                        new double[]{0.0, 0.0},
                        new double[]{0.1, 5.0},
                        new double[]{0.5, 12.0},
                        new double[]{1.5, 11.0},
                        new double[]{1.7, 0.0}
                )
        ));
    }
}
