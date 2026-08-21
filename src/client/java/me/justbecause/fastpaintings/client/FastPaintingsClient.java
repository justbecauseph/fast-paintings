package me.justbecause.fastpaintings.client;

import me.justbecause.fastpaintings.client.render.PaintingBlockRenderer;
import me.justbecause.fastpaintings.init.ModRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

@Environment(EnvType.CLIENT)
public class FastPaintingsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(ModRegistry.PAINTING_BLOCK_ENTITY, PaintingBlockRenderer::new);
    }
}
