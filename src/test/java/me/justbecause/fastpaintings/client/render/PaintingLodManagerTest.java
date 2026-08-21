package me.justbecause.fastpaintings.client.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaintingLodManagerTest {

    @Test
    @DisplayName("Hysteresis prevents flickering between SIMPLIFIED and FULL around 64 px")
    void testSimplifiedFullHysteresis() {
        // When currently FULL:
        // Stays FULL at 63.9 px (> 56.0 demotion threshold)
        assertEquals(PaintingBlockRenderState.Lod.FULL, PaintingLodManager.classifyLod(63.9, PaintingBlockRenderState.Lod.FULL));
        assertEquals(PaintingBlockRenderState.Lod.FULL, PaintingLodManager.classifyLod(56.0, PaintingBlockRenderState.Lod.FULL));
        // Demotes to SIMPLIFIED when strictly below 56.0 px
        assertEquals(PaintingBlockRenderState.Lod.SIMPLIFIED, PaintingLodManager.classifyLod(55.9, PaintingBlockRenderState.Lod.FULL));

        // When currently SIMPLIFIED:
        // Stays SIMPLIFIED at 64.1 px (< 72.0 promotion threshold)
        assertEquals(PaintingBlockRenderState.Lod.SIMPLIFIED, PaintingLodManager.classifyLod(64.1, PaintingBlockRenderState.Lod.SIMPLIFIED));
        assertEquals(PaintingBlockRenderState.Lod.SIMPLIFIED, PaintingLodManager.classifyLod(71.9, PaintingBlockRenderState.Lod.SIMPLIFIED));
        // Promotes to FULL when >= 72.0 px
        assertEquals(PaintingBlockRenderState.Lod.FULL, PaintingLodManager.classifyLod(72.0, PaintingBlockRenderState.Lod.SIMPLIFIED));
    }

    @Test
    @DisplayName("Hysteresis prevents flickering between FAR and SIMPLIFIED around 12 px")
    void testFarSimplifiedHysteresis() {
        // When currently SIMPLIFIED:
        // Stays SIMPLIFIED at 11.0 px (>= 10.0 demotion threshold)
        assertEquals(PaintingBlockRenderState.Lod.SIMPLIFIED, PaintingLodManager.classifyLod(11.0, PaintingBlockRenderState.Lod.SIMPLIFIED));
        assertEquals(PaintingBlockRenderState.Lod.SIMPLIFIED, PaintingLodManager.classifyLod(10.0, PaintingBlockRenderState.Lod.SIMPLIFIED));
        // Demotes to FAR when strictly below 10.0 px
        assertEquals(PaintingBlockRenderState.Lod.FAR, PaintingLodManager.classifyLod(9.9, PaintingBlockRenderState.Lod.SIMPLIFIED));

        // When currently FAR:
        // Stays FAR at 13.0 px (< 16.0 promotion threshold)
        assertEquals(PaintingBlockRenderState.Lod.FAR, PaintingLodManager.classifyLod(13.0, PaintingBlockRenderState.Lod.FAR));
        assertEquals(PaintingBlockRenderState.Lod.FAR, PaintingLodManager.classifyLod(15.9, PaintingBlockRenderState.Lod.FAR));
        // Promotes to SIMPLIFIED when >= 16.0 px
        assertEquals(PaintingBlockRenderState.Lod.SIMPLIFIED, PaintingLodManager.classifyLod(16.0, PaintingBlockRenderState.Lod.FAR));
    }

    @Test
    @DisplayName("Hysteresis prevents flickering between SKIP and FAR around 1 px")
    void testSkipFarHysteresis() {
        // When currently FAR:
        // Stays FAR at 0.85 px (>= 0.75 demotion threshold)
        assertEquals(PaintingBlockRenderState.Lod.FAR, PaintingLodManager.classifyLod(0.85, PaintingBlockRenderState.Lod.FAR));
        assertEquals(PaintingBlockRenderState.Lod.FAR, PaintingLodManager.classifyLod(0.75, PaintingBlockRenderState.Lod.FAR));
        // Demotes to SKIP when strictly below 0.75 px
        assertEquals(PaintingBlockRenderState.Lod.SKIP, PaintingLodManager.classifyLod(0.74, PaintingBlockRenderState.Lod.FAR));

        // When currently SKIP:
        // Stays SKIP at 1.5 px (< 2.0 promotion threshold)
        assertEquals(PaintingBlockRenderState.Lod.SKIP, PaintingLodManager.classifyLod(1.5, PaintingBlockRenderState.Lod.SKIP));
        assertEquals(PaintingBlockRenderState.Lod.SKIP, PaintingLodManager.classifyLod(1.99, PaintingBlockRenderState.Lod.SKIP));
        // Promotes to FAR when >= 2.0 px
        assertEquals(PaintingBlockRenderState.Lod.FAR, PaintingLodManager.classifyLod(2.0, PaintingBlockRenderState.Lod.SKIP));
    }

    @ParameterizedTest(name = "Large jump from {0} with size {1} -> {2}")
    @CsvSource({
            "SKIP, 100.0, FULL",
            "SKIP, 30.0, SIMPLIFIED",
            "FULL, 0.5, SKIP",
            "FULL, 5.0, FAR",
            "FAR, 100.0, FULL"
    })
    @DisplayName("Multi-tier jumps resolve correctly under hysteresis")
    void testMultiTierJumps(PaintingBlockRenderState.Lod prevLod, double projectedSize, PaintingBlockRenderState.Lod expectedLod) {
        assertEquals(expectedLod, PaintingLodManager.classifyLod(projectedSize, prevLod));
    }
}