package me.justbecause.fastpaintings.client.render;

import me.justbecause.fastpaintings.FastPaintings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class PaintingInstrumentation {

    public static boolean forceEnabled = false;

    // Cumulative counters (long) for reliable profiling and snapshot diffing
    public static long totalRenderChecks;
    public static long totalExtractions;
    public static long totalEarlyFrustumRejects;
    public static long totalExtractionFrustumRejects;
    public static long totalLateFrustumRejects;
    public static long totalSkipCount;
    public static long totalFarCount;
    public static long totalSimplifiedCount;
    public static long totalFullCount;
    public static long totalPerBlockLightSamples;
    public static long totalSingleLightSamples;
    public static long totalCustomGeometrySubmissions;
    public static long totalQuadsSubmitted;
    public static long totalVerticesSubmitted;

    private PaintingInstrumentation() {}

    public static boolean isEnabled() {
        return forceEnabled || (FastPaintings.CONFIG != null && FastPaintings.CONFIG.debugInstrumentation);
    }

    public static void reset() {
        totalRenderChecks = 0;
        totalExtractions = 0;
        totalEarlyFrustumRejects = 0;
        totalExtractionFrustumRejects = 0;
        totalLateFrustumRejects = 0;
        totalSkipCount = 0;
        totalFarCount = 0;
        totalSimplifiedCount = 0;
        totalFullCount = 0;
        totalPerBlockLightSamples = 0;
        totalSingleLightSamples = 0;
        totalCustomGeometrySubmissions = 0;
        totalQuadsSubmitted = 0;
        totalVerticesSubmitted = 0;
    }

    public static Snapshot takeSnapshot() {
        return new Snapshot(
                totalRenderChecks,
                totalExtractions,
                totalEarlyFrustumRejects,
                totalExtractionFrustumRejects,
                totalLateFrustumRejects,
                totalSkipCount,
                totalFarCount,
                totalSimplifiedCount,
                totalFullCount,
                totalPerBlockLightSamples,
                totalSingleLightSamples,
                totalCustomGeometrySubmissions,
                totalQuadsSubmitted,
                totalVerticesSubmitted
        );
    }

    public record Snapshot(
            long renderChecks,
            long extractions,
            long earlyFrustumRejects,
            long extractionFrustumRejects,
            long lateFrustumRejects,
            long skipCount,
            long farCount,
            long simplifiedCount,
            long fullCount,
            long perBlockLightSamples,
            long singleLightSamples,
            long customGeometrySubmissions,
            long quadsSubmitted,
            long verticesSubmitted
    ) {
        public Snapshot delta(Snapshot previous) {
            return new Snapshot(
                    this.renderChecks - previous.renderChecks,
                    this.extractions - previous.extractions,
                    this.earlyFrustumRejects - previous.earlyFrustumRejects,
                    this.extractionFrustumRejects - previous.extractionFrustumRejects,
                    this.lateFrustumRejects - previous.lateFrustumRejects,
                    this.skipCount - previous.skipCount,
                    this.farCount - previous.farCount,
                    this.simplifiedCount - previous.simplifiedCount,
                    this.fullCount - previous.fullCount,
                    this.perBlockLightSamples - previous.perBlockLightSamples,
                    this.singleLightSamples - previous.singleLightSamples,
                    this.customGeometrySubmissions - previous.customGeometrySubmissions,
                    this.quadsSubmitted - previous.quadsSubmitted,
                    this.verticesSubmitted - previous.verticesSubmitted
            );
        }

        public String toFormattedString() {
            return String.format(
                    """
                    Fast Paintings:

                    Render checks:                 %d
                    Extracted:                     %d
                    Early frustum rejects:         %d
                    Extraction frustum rejects:    %d
                    Late frustum rejects:          %d
                    Subpixel SKIP:                 %d
                    FAR:                           %d
                    SIMPLIFIED:                    %d
                    FULL:                          %d

                    Per-block light samples:       %d
                    Single light samples:          %d
                    Geometry submissions:          %d
                    Quads submitted:               %d
                    Vertices submitted:            %d
                    """,
                    renderChecks,
                    extractions,
                    earlyFrustumRejects,
                    extractionFrustumRejects,
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
}