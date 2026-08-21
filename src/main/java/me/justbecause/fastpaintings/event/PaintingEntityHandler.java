package me.justbecause.fastpaintings.event;

import me.justbecause.fastpaintings.FastPaintings;
import me.justbecause.fastpaintings.painting.PaintingConversionService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.painting.Painting;

public final class PaintingEntityHandler {

    public static void init() {
        ServerEntityEvents.ENTITY_LOAD.register(PaintingEntityHandler::onEntityLoad);
    }

    private static void onEntityLoad(Entity entity, ServerLevel serverLevel) {
        if (entity instanceof Painting painting) {
            if (!painting.isLoadedFromDisk() || FastPaintings.CONFIG.convertExistingPaintings) {
                serverLevel.getServer().schedule(new TickTask(serverLevel.getServer().getTickCount(), () -> {
                    PaintingConversionService.tryConvert(painting, serverLevel);
                }));
            }
        }
    }

    private PaintingEntityHandler() {}
}
