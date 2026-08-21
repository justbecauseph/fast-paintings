package me.justbecause.fastpaintings.client.render;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaintingLodTest {

    @BeforeAll
    static void setup() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @ParameterizedTest(name = "Initial projected size {0} px -> LOD {1}")
    @CsvSource({
            "0.5, SKIP",
            "0.99, SKIP",
            "1.0, FAR",
            "5.0, FAR",
            "11.99, FAR",
            "12.0, SIMPLIFIED",
            "30.0, SIMPLIFIED",
            "63.99, SIMPLIFIED",
            "64.0, FULL",
            "100.0, FULL",
            "500.0, FULL"
    })
    @DisplayName("Initial LOD matches projected pixel size thresholds")
    void testInitialLodProjectedSize(double projectedSize, PaintingBlockRenderState.Lod expectedLod) {
        PaintingBlockRenderState.Lod actualLod = PaintingLodManager.classifyLod(projectedSize, null);
        assertEquals(expectedLod, actualLod);
    }

    @Test
    @DisplayName("PaintingBlockRenderState default values")
    void testRenderStateDefaults() {
        PaintingBlockRenderState state = new PaintingBlockRenderState();
        assertEquals(Direction.NORTH, state.direction);
        assertEquals(PaintingBlockRenderState.Lod.FULL, state.lod);
        assertEquals(0, state.singleLight);
        assertEquals(0, state.lightCoordsPerBlock.length);
        assertEquals(0.0, state.projectedSize);
    }
}