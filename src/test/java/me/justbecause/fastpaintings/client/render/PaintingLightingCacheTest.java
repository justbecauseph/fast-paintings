package me.justbecause.fastpaintings.client.render;

import me.justbecause.fastpaintings.block.entity.PaintingBlockEntity;
import me.justbecause.fastpaintings.init.ModRegistry;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;

import static org.junit.jupiter.api.Assertions.*;

class PaintingLightingCacheTest {

    @BeforeAll
    static void setup() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        unfreezeRegistry(BuiltInRegistries.BLOCK);
        unfreezeRegistry(BuiltInRegistries.BLOCK_ENTITY_TYPE);
        try {
            ModRegistry.init();
        } catch (Exception ignored) {}
    }

    private static void unfreezeRegistry(Object registry) throws Exception {
        if (registry instanceof MappedRegistry<?> mapped) {
            Field intrusiveField = MappedRegistry.class.getDeclaredField("unregisteredIntrusiveHolders");
            intrusiveField.setAccessible(true);
            if (intrusiveField.get(mapped) == null) {
                intrusiveField.set(mapped, new IdentityHashMap<>());
            }
            Field frozenField = MappedRegistry.class.getDeclaredField("frozen");
            frozenField.setAccessible(true);
            frozenField.set(mapped, false);
        }
    }

    @Test
    @DisplayName("PaintingBlockEntity light cache lifecycle and invalidation")
    void testLightCacheLifecycle() {
        PaintingBlockEntity be = new PaintingBlockEntity(BlockPos.ZERO, ModRegistry.PAINTING_BLOCK.defaultBlockState());

        // Initially dirty
        assertTrue(be.isLightDirty(0, 16));

        // Create cache
        int[] cache = be.getOrCreateLightCache(16);
        assertNotNull(cache);
        assertEquals(16, cache.length);

        // Reusing same length returns the exact same array instance
        assertSame(cache, be.getOrCreateLightCache(16));

        // Set cached light
        be.setCachedLight(cache, 100);
        assertFalse(be.isLightDirty(105, 16));
        assertSame(cache, be.getCachedLight());

        // Within 20 ticks (e.g. tick 119 vs 100), not dirty
        assertFalse(be.isLightDirty(119, 16));

        // At 20 ticks elapsed (tick 120 vs 100), expires and becomes dirty
        assertTrue(be.isLightDirty(120, 16));

        // Explicit markLightDirty
        be.setCachedLight(cache, 100);
        assertFalse(be.isLightDirty(105, 16));
        be.markLightDirty();
        assertTrue(be.isLightDirty(105, 16));

        // Requesting different segment size resizes and becomes dirty
        int[] resized = be.getOrCreateLightCache(256);
        assertEquals(256, resized.length);
        assertTrue(be.isLightDirty(105, 256));
    }

    @Test
    @DisplayName("PaintingBlockEntity maintains transient lastRenderLod without persisting")
    void testTransientRenderLod() {
        PaintingBlockEntity be = new PaintingBlockEntity(BlockPos.ZERO, ModRegistry.PAINTING_BLOCK.defaultBlockState());
        assertNull(be.getLastRenderLod());

        be.setLastRenderLod(PaintingBlockRenderState.Lod.SIMPLIFIED);
        assertEquals(PaintingBlockRenderState.Lod.SIMPLIFIED, be.getLastRenderLod());

        be.setLastRenderLod(PaintingBlockRenderState.Lod.FULL);
        assertEquals(PaintingBlockRenderState.Lod.FULL, be.getLastRenderLod());
    }
}