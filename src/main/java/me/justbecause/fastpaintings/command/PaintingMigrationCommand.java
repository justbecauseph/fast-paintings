package me.justbecause.fastpaintings.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.justbecause.fastpaintings.block.entity.PaintingBlockEntity;
import me.justbecause.fastpaintings.painting.PaintingConversionService;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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
        ServerLevel level = source.getLevel();

        int paintingEntitiesCount = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Painting) {
                paintingEntitiesCount++;
            }
        }

        final int finalCount = paintingEntitiesCount;
        source.sendSuccess(() -> Component.literal(
                String.format("§6[FastPaintings]§r Loaded Painting Entities in current dimension: §e%d§r", finalCount)
        ), false);

        return 1;
    }

    private static int runConvert(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        int converted = 0;
        List<Painting> paintings = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Painting painting) {
                paintings.add(painting);
            }
        }

        for (Painting painting : paintings) {
            if (PaintingConversionService.tryConvert(painting, level)) {
                converted++;
            }
        }

        final int count = converted;
        source.sendSuccess(() -> Component.literal(
                String.format("§6[FastPaintings]§r Converted §a%d§r painting entities to block-backed paintings.", count)
        ), true);

        return converted;
    }

    private static int runRestore(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        int restored = 0;
        if (source.getEntity() instanceof ServerPlayer player) {
            BlockPos playerPos = player.blockPosition();
            int chunkRadius = 8;
            int playerChunkX = playerPos.getX() >> 4;
            int playerChunkZ = playerPos.getZ() >> 4;

            List<PaintingBlockEntity> toRestore = new ArrayList<>();
            for (int cx = playerChunkX - chunkRadius; cx <= playerChunkX + chunkRadius; cx++) {
                for (int cz = playerChunkZ - chunkRadius; cz <= playerChunkZ + chunkRadius; cz++) {
                    if (level.hasChunk(cx, cz)) {
                        LevelChunk chunk = level.getChunk(cx, cz);
                        for (BlockEntity be : chunk.getBlockEntities().values()) {
                            if (be instanceof PaintingBlockEntity pbe) {
                                toRestore.add(pbe);
                            }
                        }
                    }
                }
            }

            for (PaintingBlockEntity pbe : toRestore) {
                if (PaintingConversionService.tryRestore(pbe, level)) {
                    restored++;
                }
            }
        }

        final int count = restored;
        source.sendSuccess(() -> Component.literal(
                String.format("§6[FastPaintings]§r Restored §a%d§r block-backed paintings to vanilla entities.", count)
        ), true);

        return restored;
    }

    private PaintingMigrationCommand() {}
}
