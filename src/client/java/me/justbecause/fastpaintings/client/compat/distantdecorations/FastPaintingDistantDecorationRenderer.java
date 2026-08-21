package me.justbecause.fastpaintings.client.compat.distantdecorations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import me.justbecause.distantdecorations.api.DecorationRecord;
import me.justbecause.distantdecorations.api.DecorationType;
import me.justbecause.distantdecorations.api.client.ClientDecorationRegistry;
import me.justbecause.distantdecorations.api.client.DecorationClientRenderer;
import me.justbecause.distantdecorations.client.spatial.ProjectionMetrics;
import me.justbecause.fastpaintings.compat.distantdecorations.FastPaintingDistantData;
import me.justbecause.fastpaintings.compat.distantdecorations.FastPaintingDistantDecorationProvider;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class FastPaintingDistantDecorationRenderer implements DecorationClientRenderer<FastPaintingDistantData> {

    public static void init() {
        ClientDecorationRegistry.registerRenderer(new FastPaintingDistantDecorationRenderer());
    }

    @Override
    public DecorationType<FastPaintingDistantData> type() {
        return FastPaintingDistantDecorationProvider.TYPE;
    }

    @Override
    public void render(
        DecorationRecord record,
        FastPaintingDistantData data,
        Camera camera,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        ProjectionMetrics metrics,
        double projectedPixelSize
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        TextureAtlas paintingsAtlas = mc.getAtlasManager().getAtlasOrThrow(AtlasIds.PAINTINGS);
        if (paintingsAtlas == null) {
            return;
        }

        TextureAtlasSprite frontSprite = paintingsAtlas.getSprite(data.variantId());
        if (frontSprite == null) {
            return;
        }

        Vec3 cameraPos = camera.position();
        AABB bounds = record.bounds();
        Vec3 center = bounds.getCenter();

        poseStack.pushPose();
        poseStack.translate(center.x - cameraPos.x, center.y - cameraPos.y, center.z - cameraPos.z);

        Direction facing = data.direction();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));

        float width = data.width();
        float height = data.height();
        float x0 = -width / 2.0F;
        float x1 = width / 2.0F;
        float y0 = -height / 2.0F;
        float y1 = height / 2.0F;

        float u0 = frontSprite.getU0();
        float u1 = frontSprite.getU1();
        float v0 = frontSprite.getV0();
        float v1 = frontSprite.getV1();

        int lightCoords = 0x00F000F0; // Full ambient light for distant LOD

        RenderType renderType = RenderTypes.entityCutout(AtlasIds.PAINTINGS);
        submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            vertex(pose, buffer, x0, y0, u1, v1, 0.0F, 0, 0, -1, lightCoords);
            vertex(pose, buffer, x1, y0, u0, v1, 0.0F, 0, 0, -1, lightCoords);
            vertex(pose, buffer, x1, y1, u0, v0, 0.0F, 0, 0, -1, lightCoords);
            vertex(pose, buffer, x0, y1, u1, v0, 0.0F, 0, 0, -1, lightCoords);
        });

        poseStack.popPose();
    }

    private static void vertex(
        PoseStack.Pose pose,
        VertexConsumer buffer,
        float x,
        float y,
        float u,
        float v,
        float z,
        int nx,
        int ny,
        int nz,
        int lightCoords
    ) {
        buffer.addVertex(pose, x, y, z)
            .setColor(-1)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(lightCoords)
            .setNormal(pose, nx, ny, nz);
    }
}
