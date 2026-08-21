package me.justbecause.fastpaintings.client.render;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaintingRenderMetricsTest {

    private PaintingRenderMetrics metrics;

    @BeforeEach
    void setup() {
        metrics = new PaintingRenderMetrics();
        // 1080p, 70 deg FOV
        metrics.update(1080, 70.0, null);
    }

    @Test
    @DisplayName("Focal length calculation matches trigonometric formula")
    void testFocalLength() {
        double fovRad = Math.toRadians(70.0);
        double expectedFocal = 540.0 / Math.tan(fovRad / 2.0);
        assertEquals(expectedFocal, metrics.focalLengthPixels, 0.001);
    }

    @Test
    @DisplayName("Projected size scales linearly with world size and inversely with distance")
    void testProjectedSizeScaling() {
        // 1x1 painting at 100m
        double size1x1at100 = metrics.calculateProjectedSize(1.0, 100.0);
        // 16x16 painting at 100m should be 16x larger
        double size16x16at100 = metrics.calculateProjectedSize(16.0, 100.0);
        assertEquals(size1x1at100 * 16.0, size16x16at100, 0.001);

        // 1x1 painting at 50m should be 2x larger than at 100m
        double size1x1at50 = metrics.calculateProjectedSize(1.0, 50.0);
        assertEquals(size1x1at100 * 2.0, size1x1at50, 0.001);
    }

    @Test
    @DisplayName("Zero or negative distance does not throw and returns max value")
    void testZeroDistanceSafety() {
        double sizeZero = metrics.calculateProjectedSize(1.0, 0.0);
        assertEquals(Double.MAX_VALUE, sizeZero);

        double sizeNegative = metrics.calculateProjectedSize(1.0, -10.0);
        assertEquals(Double.MAX_VALUE, sizeNegative);
    }

    @Test
    @DisplayName("Large painting is visible at much greater distance than small painting")
    void testLargePaintingVisibilityAdvantage() {
        // At 200m:
        // 1x1 painting: projected size ~ 3.8 px (FAR)
        double size1x1 = metrics.calculateProjectedSize(1.0, 200.0);
        PaintingBlockRenderState.Lod lod1x1 = PaintingLodManager.classifyLod(size1x1, null);
        assertEquals(PaintingBlockRenderState.Lod.FAR, lod1x1);

        // 16x16 painting at 200m: projected size ~ 61.7 px (SIMPLIFIED or FULL)
        double size16x16 = metrics.calculateProjectedSize(16.0, 200.0);
        PaintingBlockRenderState.Lod lod16x16 = PaintingLodManager.classifyLod(size16x16, null);
        assertEquals(PaintingBlockRenderState.Lod.SIMPLIFIED, lod16x16);
    }
}