package me.justbecause.fastpaintings.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class PaintingBlockRenderState extends BlockEntityRenderState {

    public enum Lod {
        FULL,
        SIMPLIFIED,
        FAR
    }

    public Direction direction = Direction.NORTH;
    public @Nullable PaintingVariant variant;
    public Lod lod = Lod.FULL;
    public int singleLight;
    public int[] lightCoordsPerBlock = new int[0];
    public @Nullable AABB renderBoundingBox;
}

