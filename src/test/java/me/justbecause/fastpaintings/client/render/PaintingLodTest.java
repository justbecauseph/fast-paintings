package me.justbecause.fastpaintings.client.render;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

    @ParameterizedTest(name = "Distance {0} blocks -> LOD {1}")
    @CsvSource({
            "0.0, FULL",
            "32.0, FULL",
            "63.9, FULL",
            "64.0, SIMPLIFIED",
            "100.0, SIMPLIFIED",
            "255.9, SIMPLIFIED",
            "256.0, FAR",
            "500.0, FAR",
            "1000.0, FAR"
    })
    @DisplayName("LOD tier matches squared distance threshold")
    void testLodDistance(double distance, PaintingBlockRenderState.Lod expectedLod) {
        AABB box = new AABB(0, 0, 0, 0, 0, 0);
        Vec3 cameraPos = new Vec3(0, 0, distance);

        double distanceSq = box.distanceToSqr(cameraPos);
        PaintingBlockRenderState.Lod actualLod;
        if (distanceSq < 64.0 * 64.0) {
            actualLod = PaintingBlockRenderState.Lod.FULL;
        } else if (distanceSq < 256.0 * 256.0) {
            actualLod = PaintingBlockRenderState.Lod.SIMPLIFIED;
        } else {
            actualLod = PaintingBlockRenderState.Lod.FAR;
        }

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
    }
}
