package me.justbecause.fastpaintings.client.render;

import me.justbecause.fastpaintings.FastPaintings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class PaintingInstrumentation {

    public static boolean forceEnabled = false;

    public static int loadedPaintings;
    public static int paintingsExtracted;
    public static int earlyFrustumRejects;
    public static int lateFrustumRejects;
    public static int skipCount;
    public static int farCount;
    public static int simplifiedCount;
    public static int fullCount;
    public static int perBlockLightSamples;
    public static int singleLightSamples;
    public static int customGeometrySubmissions;
    public static int quadsSubmitted;
    public static int verticesSubmitted;

    private PaintingInstrumentation() {}

    public static boolean isEnabled() {
        return forceEnabled || (FastPaintings.CONFIG != null && FastPaintings.CONFIG.debugInstrumentation);
    }

    public static void resetFrame() {
        loadedPaintings = 0;
        paintingsExtracted = 0;
        earlyFrustumRejects = 0;
        lateFrustumRejects = 0;
        skipCount = 0;
        farCount = 0;
        simplifiedCount = 0;
        fullCount = 0;
        perBlockLightSamples = 0;
        singleLightSamples = 0;
        customGeometrySubmissions = 0;
        quadsSubmitted = 0;
        verticesSubmitted = 0;
    }

    public static String getFormattedDebugOutput() {
        return String.format(
                """
                Fast Paintings:

                Loaded:                   %d
                Early frustum rejects:    %d
                Late frustum rejects:     %d
                SKIP:                     %d
                FAR:                      %d
                SIMPLIFIED:               %d
                FULL:                     %d

                Per-block light samples:  %d
                Single light samples:     %d
                Geometry submissions:     %d
                Quads submitted:          %d
                Vertices submitted:       %d
                """,
                loadedPaintings,
                earlyFrustumRejects,
                lateFrustumRejects,
                skipCount,
                farCount,
                simplifiedCount,
                fullCount,
                perBlockLightSamples,
                singleLightSamples,
                customGeometrySubmissions,
                quadsSubmitted,
                verticesSubmitted
        );
    }
}