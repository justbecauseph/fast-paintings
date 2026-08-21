package me.justbecause.fastpaintings.client.render;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaintingInstrumentationTest {

    @BeforeEach
    void setup() {
        PaintingInstrumentation.resetFrame();
    }

    @Test
    @DisplayName("Reset frame clears all metrics counters")
    void testResetFrame() {
        PaintingInstrumentation.loadedPaintings = 100;
        PaintingInstrumentation.earlyFrustumRejects = 50;
        PaintingInstrumentation.lateFrustumRejects = 5;
        PaintingInstrumentation.skipCount = 20;
        PaintingInstrumentation.farCount = 10;
        PaintingInstrumentation.simplifiedCount = 10;
        PaintingInstrumentation.fullCount = 5;
        PaintingInstrumentation.perBlockLightSamples = 128;
        PaintingInstrumentation.singleLightSamples = 20;
        PaintingInstrumentation.customGeometrySubmissions = 25;
        PaintingInstrumentation.quadsSubmitted = 150;
        PaintingInstrumentation.verticesSubmitted = 600;

        PaintingInstrumentation.resetFrame();

        assertEquals(0, PaintingInstrumentation.loadedPaintings);
        assertEquals(0, PaintingInstrumentation.earlyFrustumRejects);
        assertEquals(0, PaintingInstrumentation.lateFrustumRejects);
        assertEquals(0, PaintingInstrumentation.skipCount);
        assertEquals(0, PaintingInstrumentation.farCount);
        assertEquals(0, PaintingInstrumentation.simplifiedCount);
        assertEquals(0, PaintingInstrumentation.fullCount);
        assertEquals(0, PaintingInstrumentation.perBlockLightSamples);
        assertEquals(0, PaintingInstrumentation.singleLightSamples);
        assertEquals(0, PaintingInstrumentation.customGeometrySubmissions);
        assertEquals(0, PaintingInstrumentation.quadsSubmitted);
        assertEquals(0, PaintingInstrumentation.verticesSubmitted);
    }

    @Test
    @DisplayName("Formatted debug output contains all tracked fields")
    void testFormattedOutput() {
        PaintingInstrumentation.loadedPaintings = 3120;
        PaintingInstrumentation.earlyFrustumRejects = 2470;
        PaintingInstrumentation.skipCount = 310;
        PaintingInstrumentation.farCount = 210;
        PaintingInstrumentation.simplifiedCount = 92;
        PaintingInstrumentation.fullCount = 38;
        PaintingInstrumentation.perBlockLightSamples = 684;
        PaintingInstrumentation.singleLightSamples = 302;

        String output = PaintingInstrumentation.getFormattedDebugOutput();
        assertTrue(output.contains("Loaded:                   3120"));
        assertTrue(output.contains("Early frustum rejects:    2470"));
        assertTrue(output.contains("SKIP:                     310"));
        assertTrue(output.contains("FAR:                      210"));
        assertTrue(output.contains("SIMPLIFIED:               92"));
        assertTrue(output.contains("FULL:                     38"));
        assertTrue(output.contains("Per-block light samples:  684"));
        assertTrue(output.contains("Single light samples:     302"));
    }
}