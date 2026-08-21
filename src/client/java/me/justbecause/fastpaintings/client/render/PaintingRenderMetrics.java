package me.justbecause.fastpaintings.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class PaintingRenderMetrics {

    private static final PaintingRenderMetrics INSTANCE = new PaintingRenderMetrics();

    public static PaintingRenderMetrics getInstance() {
        return INSTANCE;
    }

    public int viewportHeight = 1080;
    public double fovDegrees = 70.0;
    public double focalLengthPixels = 771.2;
    public @Nullable Frustum currentFrustum;

    private int lastViewportHeight = -1;
    private double lastFovDegrees = -1.0;

    public void update(Minecraft minecraft, Camera camera) {
        int height = (minecraft != null && minecraft.getWindow() != null) ? minecraft.getWindow().getHeight() : 1080;
        double fov = (camera != null) ? camera.getFov() : 70.0;
        Frustum frustum = (camera != null) ? camera.getCullFrustum() : null;
        update(height, fov, frustum);
    }

    public void update(int viewportHeight, double fovDegrees, @Nullable Frustum frustum) {
        this.currentFrustum = frustum;
        int safeHeight = Math.max(1, viewportHeight);
        double safeFov = fovDegrees > 0 ? fovDegrees : 70.0;

        if (this.lastViewportHeight != safeHeight || this.lastFovDegrees != safeFov) {
            this.viewportHeight = safeHeight;
            this.fovDegrees = safeFov;
            this.lastViewportHeight = safeHeight;
            this.lastFovDegrees = safeFov;

            double fovRadians = Math.toRadians(safeFov);
            this.focalLengthPixels = (safeHeight / 2.0) / Math.tan(Math.max(0.001, fovRadians / 2.0));
        }
    }

    public double calculateProjectedSize(double worldSize, double distance) {
        if (distance <= 0.001) {
            return Double.MAX_VALUE;
        }
        return (worldSize * this.focalLengthPixels) / distance;
    }
}