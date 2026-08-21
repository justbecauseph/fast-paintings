package me.justbecause.fastpaintings.client.render;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaintingInstrumentationTest {

    @BeforeEach
    void setup() {
        PaintingInstrumentation.reset();
    }

    @Test
    @DisplayName("Reset clears all cumulative metrics counters")
    void testReset() {
        PaintingInstrumentation.totalLoaded = 100;
        PaintingInstrumentation.totalExtractions = 90;
        PaintingInstrumentation.totalEarlyFrustumRejects = 50;
        PaintingInstrumentation.totalLateFrustumRejects = 5;
        PaintingInstrumentation.totalSkipCount = 20;
        PaintingInstrumentation.totalFarCount = 10;
        PaintingInstrumentation.totalSimplifiedCount = 10;
        PaintingInstrumentation.totalFullCount = 5;
        PaintingInstrumentation.totalPerBlockLightSamples = 128;
        PaintingInstrumentation.totalSingleLightSamples = 20;
        PaintingInstrumentation.totalCustomGeometrySubmissions = 25;
        PaintingInstrumentation.totalQuadsSubmitted = 150;
        PaintingInstrumentation.totalVerticesSubmitted = 600;

        PaintingInstrumentation.reset();

        assertEquals(0, PaintingInstrumentation.totalLoaded);
        assertEquals(0, PaintingInstrumentation.totalExtractions);
        assertEquals(0, PaintingInstrumentation.totalEarlyFrustumRejects);
        assertEquals(0, PaintingInstrumentation.totalLateFrustumRejects);
        assertEquals(0, PaintingInstrumentation.totalSkipCount);
        assertEquals(0, PaintingInstrumentation.totalFarCount);
        assertEquals(0, PaintingInstrumentation.totalSimplifiedCount);
        assertEquals(0, PaintingInstrumentation.totalFullCount);
        assertEquals(0, PaintingInstrumentation.totalPerBlockLightSamples);
        assertEquals(0, PaintingInstrumentation.totalSingleLightSamples);
        assertEquals(0, PaintingInstrumentation.totalCustomGeometrySubmissions);
        assertEquals(0, PaintingInstrumentation.totalQuadsSubmitted);
        assertEquals(0, PaintingInstrumentation.totalVerticesSubmitted);
    }

    @Test
    @DisplayName("Snapshots calculate deltas accurately for benchmark windows")
    void testSnapshotDelta() {
        PaintingInstrumentation.totalLoaded = 100;
        PaintingInstrumentation.totalExtractions = 80;
        PaintingInstrumentation.totalEarlyFrustumRejects = 20;
        PaintingInstrumentation.totalPerBlockLightSamples = 256;

        PaintingInstrumentation.Snapshot start = PaintingInstrumentation.takeSnapshot();

        // Simulate benchmark frames
        PaintingInstrumentation.totalLoaded += 3000;
        PaintingInstrumentation.totalExtractions += 600;
        PaintingInstrumentation.totalEarlyFrustumRejects += 2400;
        PaintingInstrumentation.totalPerBlockLightSamples += 512;

        PaintingInstrumentation.Snapshot end = PaintingInstrumentation.takeSnapshot();
        PaintingInstrumentation.Snapshot delta = end.delta(start);

        assertEquals(3000, delta.loaded());
        assertEquals(600, delta.extractions());
        assertEquals(2400, delta.earlyFrustumRejects());
        assertEquals(512, delta.perBlockLightSamples());

        String formatted = delta.toFormattedString();
        assertTrue(formatted.contains("Loaded:                   3000"));
        assertTrue(formatted.contains("Extracted:                600"));
        assertTrue(formatted.contains("Early frustum rejects:    2400"));
        assertTrue(formatted.contains("Per-block light samples:  512"));
    }
}