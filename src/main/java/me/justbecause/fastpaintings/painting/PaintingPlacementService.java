package me.justbecause.fastpaintings.painting;

import me.justbecause.fastpaintings.block.PaintingBlock;
import me.justbecause.fastpaintings.block.PaintingPartBlock;
import me.justbecause.fastpaintings.block.entity.PaintingBlockEntity;
import me.justbecause.fastpaintings.init.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PaintingPlacementService {

    public static boolean tryPlacePainting(
            Level level,
            BlockPos anchorPos,
            Direction facing,
            @Nullable Holder<PaintingVariant> forcedVariant,
            @Nullable Player player,
            RandomSource random
    ) {
        if (facing.getAxis().isVertical()) {
            return false;
        }

        Holder<PaintingVariant> selectedVariant;
        if (forcedVariant != null) {
            PaintingFootprint footprint = PaintingFootprint.of(
                    anchorPos, facing, forcedVariant.value().width(), forcedVariant.value().height()
            );
            if (!canPlaceFootprint(level, footprint)) {
                return false;
            }
            selectedVariant = forcedVariant;
        } else {
            Optional<Holder<PaintingVariant>> variantOpt = selectBestVariant(level, anchorPos, facing, random);
            if (variantOpt.isEmpty()) {
                return false;
            }
            selectedVariant = variantOpt.get();
        }

        PaintingFootprint footprint = PaintingFootprint.of(
                anchorPos, facing, selectedVariant.value().width(), selectedVariant.value().height()
        );

        return placePaintingBlocks(level, footprint, selectedVariant, player);
    }

    public static Optional<Holder<PaintingVariant>> selectBestVariant(
            Level level,
            BlockPos anchorPos,
            Direction facing,
            RandomSource random
    ) {
        List<Holder<PaintingVariant>> candidates = new ArrayList<>();
        level.registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT)
                .getTagOrEmpty(PaintingVariantTags.PLACEABLE)
                .forEach(candidates::add);

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        // Filter out variants that cannot fit or lack support
        candidates.removeIf(variant -> {
            PaintingFootprint footprint = PaintingFootprint.of(
                    anchorPos, facing, variant.value().width(), variant.value().height()
            );
            return !canPlaceFootprint(level, footprint);
        });

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        // Keep only the largest area variants (exact vanilla parity)
        int maxArea = candidates.stream().mapToInt(v -> v.value().area()).max().orElse(0);
        candidates.removeIf(v -> v.value().area() < maxArea);

        return Util.getRandomSafe(candidates, random);
    }

    public static boolean canPlaceFootprint(Level level, PaintingFootprint footprint) {
        if (!footprint.isSupported(level)) {
            return false;
        }
        for (BlockPos pos : footprint.occupiedCells()) {
            if (!level.hasChunkAt(pos)) {
                return false;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.is(Blocks.WATER)) {
                return false;
            }
        }
        return level.noBlockCollision(null, footprint.boundingBox())
                && level.noBorderCollision(null, footprint.boundingBox());
    }

    public static boolean placePaintingBlocks(
            Level level,
            PaintingFootprint footprint,
            Holder<PaintingVariant> variant,
            @Nullable Player player
    ) {
        BlockPos anchorPos = footprint.anchor();
        Direction facing = footprint.facing();

        // 1. Place the Anchor Block
        boolean isAnchorWaterlogged = level.getFluidState(anchorPos).getType() == Fluids.WATER;
        BlockState anchorState = ModRegistry.PAINTING_BLOCK.defaultBlockState()
                .setValue(PaintingBlock.FACING, facing)
                .setValue(PaintingBlock.WATERLOGGED, isAnchorWaterlogged);

        level.setBlock(anchorPos, anchorState, Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
        if (level.getBlockEntity(anchorPos) instanceof PaintingBlockEntity be) {
            be.setVariant(variant);
        } else {
            return false;
        }

        // 2. Place Part Blocks for all other cells
        for (BlockPos cellPos : footprint.occupiedCells()) {
            if (cellPos.equals(anchorPos)) {
                continue;
            }
            boolean isWaterlogged = level.getFluidState(cellPos).getType() == Fluids.WATER;
            int offsetX = PaintingFootprint.getOffsetX(cellPos, anchorPos, facing);
            int offsetY = PaintingFootprint.getOffsetY(cellPos, anchorPos);

            BlockState partState = ModRegistry.PAINTING_PART_BLOCK.defaultBlockState()
                    .setValue(PaintingPartBlock.FACING, facing)
                    .setValue(PaintingPartBlock.OFFSET_X, offsetX)
                    .setValue(PaintingPartBlock.OFFSET_Y, offsetY)
                    .setValue(PaintingPartBlock.WATERLOGGED, isWaterlogged);

            level.setBlock(cellPos, partState, Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
        }

        level.playSound(null, anchorPos, SoundEvents.PAINTING_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (player != null) {
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, anchorPos);
        }

        return true;
    }

    private PaintingPlacementService() {}
}
