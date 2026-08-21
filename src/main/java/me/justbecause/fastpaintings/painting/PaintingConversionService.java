package me.justbecause.fastpaintings.painting;

import me.justbecause.fastpaintings.FastPaintings;
import me.justbecause.fastpaintings.block.entity.PaintingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class PaintingConversionService {

    public static boolean tryConvert(Painting painting, ServerLevel serverLevel) {
        if (!painting.isAlive()) {
            return false;
        }

        // Parity check: skip if entity has special custom data and config requests skipping
        if (FastPaintings.CONFIG.skipSpecialEntityData) {
            if (painting.hasCustomName() || painting.isInvulnerable() || painting.hasGlowingTag()) {
                FastPaintings.LOGGER.debug("Skipping conversion of painting at {}: special entity properties present",
                        painting.blockPosition());
                return false;
            }
        }

        BlockPos anchorPos = painting.blockPosition();
        Direction facing = painting.getDirection();
        Holder<PaintingVariant> variant = painting.getVariant();

        PaintingFootprint footprint = PaintingFootprint.of(
                anchorPos, facing, variant.value().width(), variant.value().height()
        );

        if (!footprint.isSupported(serverLevel)) {
            FastPaintings.LOGGER.debug("Skipping conversion of painting at {}: backing blocks are not solid/supported",
                    anchorPos);
            return false;
        }

        // Validate all occupied cells are convertible (air, water)
        for (BlockPos pos : footprint.occupiedCells()) {
            if (!serverLevel.isLoaded(pos)) {
                return false;
            }
            BlockState state = serverLevel.getBlockState(pos);
            if (!state.isAir() && !state.is(Blocks.WATER)) {
                FastPaintings.LOGGER.debug("Skipping conversion of painting at {}: cell {} is obstructed by {}",
                        anchorPos, pos, state.getBlock());
                return false;
            }
        }

        boolean success = PaintingPlacementService.placePaintingBlocks(serverLevel, footprint, variant, null);
        if (success) {
            painting.kill(serverLevel);
            FastPaintings.LOGGER.debug("Successfully converted painting at {} to block-backed decoration", anchorPos);
            return true;
        }

        return false;
    }

    public static boolean tryRestore(PaintingBlockEntity blockEntity, ServerLevel serverLevel) {
        Holder<PaintingVariant> variant = blockEntity.getVariant();
        if (variant == null) {
            return false;
        }

        BlockPos anchorPos = blockEntity.getBlockPos();
        Direction facing = blockEntity.getFacing();

        Painting painting = new Painting(serverLevel, anchorPos, facing, variant);
        if (painting.survives()) {
            blockEntity.removeFootprint(serverLevel, false, null);
            serverLevel.addFreshEntity(painting);
            return true;
        }

        return false;
    }

    private PaintingConversionService() {}
}
