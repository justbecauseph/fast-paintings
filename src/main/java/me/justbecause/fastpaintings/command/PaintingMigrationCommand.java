package me.justbecause.fastpaintings.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.justbecause.fastpaintings.block.entity.PaintingBlockEntity;
import me.justbecause.fastpaintings.painting.PaintingConversionService;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

public final class PaintingMigrationCommand {

    public static void init() {
        CommandRegistrationCallback.EVENT.register(PaintingMigrationCommand::register);
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext buildContext,
            Commands.CommandSelection selection
    ) {
        dispatcher.register(
                Commands.literal("fastpaintings")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("stats").executes(PaintingMigrationCommand::runStats))
                        .then(Commands.literal("convert").executes(PaintingMigrationCommand::runConvert))
                        .then(Commands.literal("restore").executes(PaintingMigrationCommand::runRestore))
        );
    }

    private static int runStats(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int totalEntities = 0;
        int totalBlockPaintings = 0;

        for (ServerLevel level : source.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity.getType() == EntityTypes.PAINTING) {
                    totalEntities++;
                }
            }
        }

        final int entities = totalEntities;
        source.sendSuccess(() -> Component.literal(
                String.format("§6[FastPaintings]§r Total loaded vanilla painting entities across all worlds: §e%d§r", entities)
        ), false);

        return 1;
    }

    private static int runConvert(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int converted = 0;

        for (ServerLevel level : source.getServer().getAllLevels()) {
            List<Painting> paintings = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity.getType() == EntityTypes.PAINTING && entity instanceof Painting painting) {
                    paintings.add(painting);
                }
            }

            for (Painting painting : paintings) {
                if (PaintingConversionService.tryConvert(painting, level)) {
                    converted++;
                }
            }
        }

        final int count = converted;
        source.sendSuccess(() -> Component.literal(
                String.format("§6[FastPaintings]§r Converted §a%d§r painting entities across all dimensions to block-backed paintings.", count)
        ), true);

        return converted;
    }

    private static int runRestore(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int restored = 0;
        int skipped = 0;

        Iterable<ServerLevel> levels = (source.getEntity() instanceof ServerPlayer player)
                ? List.of(player.level())
                : source.getServer().getAllLevels();

        for (ServerLevel level : levels) {
            List<PaintingBlockEntity> toRestore = new ArrayList<>();

            // Find all loaded PaintingBlockEntity instances
            if (source.getEntity() instanceof ServerPlayer player && level == player.level()) {
                int chunkRadius = 16;
                int playerChunkX = SectionPos.blockToSectionCoord(player.getBlockX());
                int playerChunkZ = SectionPos.blockToSectionCoord(player.getBlockZ());
                for (int cx = playerChunkX - chunkRadius; cx <= playerChunkX + chunkRadius; cx++) {
                    for (int cz = playerChunkZ - chunkRadius; cz <= playerChunkZ + chunkRadius; cz++) {
                        if (level.getChunkSource().hasChunk(cx, cz)) {
                            LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                            if (chunk != null) {
                                for (BlockEntity be : chunk.getBlockEntities().values()) {
                                    if (be instanceof PaintingBlockEntity pbe) {
                                        toRestore.add(pbe);
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Console global execution across all loaded chunks
                for (ServerPlayer p : level.players()) {
                    int pChunkX = SectionPos.blockToSectionCoord(p.getBlockX());
                    int pChunkZ = SectionPos.blockToSectionCoord(p.getBlockZ());
                    for (int cx = pChunkX - 16; cx <= pChunkX + 16; cx++) {
                        for (int cz = pChunkZ - 16; cz <= pChunkZ + 16; cz++) {
                            if (level.getChunkSource().hasChunk(cx, cz)) {
                                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                                if (chunk != null) {
                                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                                        if (be instanceof PaintingBlockEntity pbe && !toRestore.contains(pbe)) {
                                            toRestore.add(pbe);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            for (PaintingBlockEntity pbe : toRestore) {
                if (PaintingConversionService.tryRestore(pbe, level)) {
                    restored++;
                } else {
                    skipped++;
                }
            }
        }

        final int count = restored;
        final int skippedCount = skipped;
        source.sendSuccess(() -> Component.literal(
                String.format("§6[FastPaintings]§r Restored §a%d§r block-backed paintings back to vanilla entities (Skipped: %d).", count, skippedCount)
        ), true);

        return restored;
    }

    private PaintingMigrationCommand() {}
}
