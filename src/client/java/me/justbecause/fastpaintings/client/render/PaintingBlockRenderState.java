package me.justbecause.fastpaintings.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class PaintingBlockRenderState extends BlockEntityRenderState {
    public Direction direction = Direction.NORTH;
    public @Nullable PaintingVariant variant;
    public int[] lightCoordsPerBlock = new int[0];
}
