package me.justbecause.fastpaintings.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.justbecause.fastpaintings.FastPaintings;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;

public class FastPaintingsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean convertExistingPaintings = true;
    public boolean convertOnPlacement = true;
    public boolean convertCommandCreatedPaintings = true;
    public boolean skipSpecialEntityData = true;
    public boolean preserveVariantOnDrop = true;

    public static FastPaintingsConfig load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("fastpaintings.json");
        File file = configPath.toFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                FastPaintingsConfig config = GSON.fromJson(reader, FastPaintingsConfig.class);
                if (config != null) {
                    config.save();
                    return config;
                }
            } catch (Exception e) {
                FastPaintings.LOGGER.error("Failed to load fastpaintings.json, falling back to defaults", e);
            }
        }
        FastPaintingsConfig config = new FastPaintingsConfig();
        config.save();
        return config;
    }

    public void save() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("fastpaintings.json");
        File file = configPath.toFile();
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(this, writer);
        } catch (Exception e) {
            FastPaintings.LOGGER.error("Failed to save fastpaintings.json", e);
        }
    }
}
