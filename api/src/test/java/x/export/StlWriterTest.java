package x.export;

import org.junit.jupiter.api.Test;
import x.geom.Revolve;
import x.geom.TriangleMesh;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

class StlWriterTest {

    @Test
    void binaryHeaderAndTriCount() {
        TriangleMesh m = Revolve.build(x -> 0.02, 0.1, 2, 8, 0.0);
        byte[] data = StlWriter.toBinary(m, "test");

        assertTrue(data.length > 84, "must have at least header + count");
        ByteBuffer buf = ByteBuffer.wrap(data, 80, 4).order(ByteOrder.LITTLE_ENDIAN);
        int triCount = buf.getInt();
        assertEquals(m.triangleCount(), triCount);
        assertEquals(84 + 50 * triCount, data.length);

        String header = new String(data, 0, 11, java.nio.charset.StandardCharsets.US_ASCII);
        assertEquals("OpenRocketX", header);
    }

    @Test
    void asciiRoundTripsTriangleCount() {
        TriangleMesh m = Revolve.build(x -> 0.02, 0.1, 2, 8, 0.0);
        String ascii = StlWriter.toAscii(m, "tube");
        long facets = ascii.lines().filter(l -> l.startsWith("facet normal")).count();
        assertEquals(m.triangleCount(), facets);
        assertTrue(ascii.startsWith("solid tube"));
        assertTrue(ascii.trim().endsWith("endsolid tube"));
    }
}
