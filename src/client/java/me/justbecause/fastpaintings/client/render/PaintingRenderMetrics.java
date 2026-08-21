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
    public long lastUpdatedFrameTime = -1;
    public @Nullable Frustum currentFrustum;

    public void update(Minecraft minecraft, Camera camera, long gameTime) {
        if (this.lastUpdatedFrameTime == gameTime && this.currentFrustum != null) {
            return;
        }
        int height = (minecraft != null && minecraft.getWindow() != null) ? minecraft.getWindow().getHeight() : 1080;
        float fov = (camera != null) ? camera.getFov() : 70.0F;
        Frustum frustum = (camera != null) ? camera.getCullFrustum() : null;
        update(height, fov, frustum);
        this.lastUpdatedFrameTime = gameTime;
    }

    public void update(int viewportHeight, double fovDegrees, @Nullable Frustum frustum) {
        this.viewportHeight = Math.max(1, viewportHeight);
        this.fovDegrees = fovDegrees > 0 ? fovDegrees : 70.0;
        double fovRadians = Math.toRadians(this.fovDegrees);
        this.focalLengthPixels = (this.viewportHeight / 2.0) / Math.tan(Math.max(0.001, fovRadians / 2.0));
        this.currentFrustum = frustum;
    }

    public double calculateProjectedSize(double worldSize, double distance) {
        if (distance <= 0.001) {
            return Double.MAX_VALUE;
        }
        return (worldSize * this.focalLengthPixels) / distance;
    }
}