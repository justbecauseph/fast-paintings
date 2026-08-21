package me.justbecause.fastpaintings.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class PaintingLodManager {

    // Base thresholds (pixels)
    public static final double BASE_SKIP_THRESHOLD = 1.0;
    public static final double BASE_FAR_THRESHOLD = 12.0;
    public static final double BASE_SIMPLIFIED_THRESHOLD = 64.0;

    // Hysteresis promotion thresholds (pixels)
    public static final double PROMOTION_SKIP_TO_FAR = 2.0;
    public static final double PROMOTION_FAR_TO_SIMPLIFIED = 16.0;
    public static final double PROMOTION_SIMPLIFIED_TO_FULL = 72.0;

    // Hysteresis demotion thresholds (pixels)
    public static final double DEMOTION_FULL_TO_SIMPLIFIED = 56.0;
    public static final double DEMOTION_SIMPLIFIED_TO_FAR = 10.0;
    public static final double DEMOTION_FAR_TO_SKIP = 0.75;

    private PaintingLodManager() {}

    /**
     * Classifies the LOD tier based on projected screen size and previous frame LOD tier.
     */
    public static PaintingBlockRenderState.Lod classifyLod(
            double projectedSize,
            PaintingBlockRenderState.@Nullable Lod previousLod
    ) {
        if (previousLod == null) {
            if (projectedSize < BASE_SKIP_THRESHOLD) {
                return PaintingBlockRenderState.Lod.SKIP;
            } else if (projectedSize < BASE_FAR_THRESHOLD) {
                return PaintingBlockRenderState.Lod.FAR;
            } else if (projectedSize < BASE_SIMPLIFIED_THRESHOLD) {
                return PaintingBlockRenderState.Lod.SIMPLIFIED;
            } else {
                return PaintingBlockRenderState.Lod.FULL;
            }
        }

        return switch (previousLod) {
            case FULL -> {
                if (projectedSize < DEMOTION_FAR_TO_SKIP) {
                    yield PaintingBlockRenderState.Lod.SKIP;
                } else if (projectedSize < DEMOTION_SIMPLIFIED_TO_FAR) {
                    yield PaintingBlockRenderState.Lod.FAR;
                } else if (projectedSize < DEMOTION_FULL_TO_SIMPLIFIED) {
                    yield PaintingBlockRenderState.Lod.SIMPLIFIED;
                } else {
                    yield PaintingBlockRenderState.Lod.FULL;
                }
            }
            case SIMPLIFIED -> {
                if (projectedSize >= PROMOTION_SIMPLIFIED_TO_FULL) {
                    yield PaintingBlockRenderState.Lod.FULL;
                } else if (projectedSize < DEMOTION_FAR_TO_SKIP) {
                    yield PaintingBlockRenderState.Lod.SKIP;
                } else if (projectedSize < DEMOTION_SIMPLIFIED_TO_FAR) {
                    yield PaintingBlockRenderState.Lod.FAR;
                } else {
                    yield PaintingBlockRenderState.Lod.SIMPLIFIED;
                }
            }
            case FAR -> {
                if (projectedSize >= PROMOTION_SIMPLIFIED_TO_FULL) {
                    yield PaintingBlockRenderState.Lod.FULL;
                } else if (projectedSize >= PROMOTION_FAR_TO_SIMPLIFIED) {
                    yield PaintingBlockRenderState.Lod.SIMPLIFIED;
                } else if (projectedSize < DEMOTION_FAR_TO_SKIP) {
                    yield PaintingBlockRenderState.Lod.SKIP;
                } else {
                    yield PaintingBlockRenderState.Lod.FAR;
                }
            }
            case SKIP -> {
                if (projectedSize >= PROMOTION_SIMPLIFIED_TO_FULL) {
                    yield PaintingBlockRenderState.Lod.FULL;
                } else if (projectedSize >= PROMOTION_FAR_TO_SIMPLIFIED) {
                    yield PaintingBlockRenderState.Lod.SIMPLIFIED;
                } else if (projectedSize >= PROMOTION_SKIP_TO_FAR) {
                    yield PaintingBlockRenderState.Lod.FAR;
                } else {
                    yield PaintingBlockRenderState.Lod.SKIP;
                }
            }
        };
    }
}