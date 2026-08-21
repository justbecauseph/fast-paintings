package me.justbecause.fastpaintings;

import me.justbecause.fastpaintings.command.PaintingMigrationCommand;
import me.justbecause.fastpaintings.config.FastPaintingsConfig;
import me.justbecause.fastpaintings.event.PaintingEntityHandler;
import me.justbecause.fastpaintings.init.ModRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FastPaintings implements ModInitializer {
    public static final String MOD_ID = "fastpaintings";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static FastPaintingsConfig CONFIG = new FastPaintingsConfig();

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Fast Paintings for Minecraft 26.2...");
        CONFIG = FastPaintingsConfig.load();
        ModRegistry.init();
        PaintingEntityHandler.init();
        PaintingMigrationCommand.init();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
