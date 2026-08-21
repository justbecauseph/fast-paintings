package me.justbecause.fastpaintings.test;

import me.justbecause.fastpaintings.block.PaintingBlock;
import me.justbecause.fastpaintings.block.PaintingPartBlock;
import me.justbecause.fastpaintings.block.entity.PaintingBlockEntity;
import me.justbecause.fastpaintings.init.ModRegistry;
import me.justbecause.fastpaintings.painting.PaintingConversionService;
import me.justbecause.fastpaintings.painting.PaintingFootprint;
import me.justbecause.fastpaintings.painting.PaintingPlacementService;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.entity.decoration.painting.PaintingVariants;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class FastPaintingsGameTests {

    @GameTest
    public void test1x1PlacementAndBreak(GameTestHelper helper) {
        BlockPos wallPos = new BlockPos(2, 2, 2);
        BlockPos paintingPos = new BlockPos(2, 2, 3);
        helper.setBlock(wallPos, Blocks.STONE);
        helper.setBlock(paintingPos, Blocks.AIR);

        ServerLevel level = helper.getLevel();
        Holder<PaintingVariant> kebab = level.registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT)
                .getOrThrow(PaintingVariants.KEBAB);

        boolean placed = PaintingPlacementService.tryPlacePainting(
                level, helper.absolutePos(paintingPos), Direction.SOUTH, kebab, null, level.getRandom()
        );
        helper.assertTrue(placed, "Failed to place 1x1 kebab painting");

        BlockState state = helper.getBlockState(paintingPos);
        helper.assertTrue(state.is(ModRegistry.PAINTING_BLOCK), "Anchor block was not placed");
        helper.assertTrue(state.getValue(PaintingBlock.FACING) == Direction.SOUTH, "Facing does not match SOUTH");

        PaintingBlockEntity be = helper.getBlockEntity(paintingPos, PaintingBlockEntity.class);
        helper.assertTrue(be.getVariant() != null && be.getVariant().is(PaintingVariants.KEBAB), "Variant was not set to kebab");

        // Break anchor
        be.removeFootprint(level, false, null);
        helper.assertTrue(helper.getBlockState(paintingPos).isAir(), "Painting was not removed on break");

        helper.succeed();
    }

    @GameTest
    public void test2x2MigrationAndRestore(GameTestHelper helper) {
        BlockPos wallOrigin = new BlockPos(1, 1, 2);
        // Build 3x3 stone wall
        for (int x = 1; x <= 3; x++) {
            for (int y = 1; y <= 3; y++) {
                helper.setBlock(new BlockPos(x, y, 2), Blocks.STONE);
                helper.setBlock(new BlockPos(x, y, 3), Blocks.AIR);
            }
        }

        ServerLevel level = helper.getLevel();
        Holder<PaintingVariant> match = level.registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT)
                .getOrThrow(PaintingVariants.MATCH); // 2x2

        BlockPos anchorPos = helper.absolutePos(new BlockPos(2, 2, 3));
        Painting entity = new Painting(level, anchorPos, Direction.SOUTH, match);
        level.addFreshEntity(entity);

        helper.assertTrue(entity.isAlive(), "Spawned painting entity is not alive");
        helper.assertTrue(entity.getPos().equals(anchorPos), "Entity getPos does not match anchorPos");

        // Convert entity to block-backed painting
        boolean converted = PaintingConversionService.tryConvert(entity, level);
        helper.assertTrue(converted, "Conversion failed for 2x2 match painting");
        helper.assertTrue(!entity.isAlive(), "Entity was not killed after successful conversion");

        BlockPos relativeAnchor = new BlockPos(2, 2, 3);
        BlockState anchorState = helper.getBlockState(relativeAnchor);
        helper.assertTrue(anchorState.is(ModRegistry.PAINTING_BLOCK), "Anchor block state missing");

        PaintingBlockEntity be = helper.getBlockEntity(relativeAnchor, PaintingBlockEntity.class);
        helper.assertTrue(be.getVariant() != null && be.getVariant().is(PaintingVariants.MATCH), "Variant is not match");

        // Restore back to entity
        boolean restored = PaintingConversionService.tryRestore(be, level);
        helper.assertTrue(restored, "Failed to restore 2x2 painting back to vanilla entity");

        helper.succeed();
    }

    @GameTest
    public void testWaterloggedPreservation(GameTestHelper helper) {
        BlockPos wallPos = new BlockPos(2, 2, 2);
        BlockPos waterPos = new BlockPos(2, 2, 3);
        helper.setBlock(wallPos, Blocks.STONE);
        helper.setBlock(waterPos, Blocks.WATER);

        ServerLevel level = helper.getLevel();
        Holder<PaintingVariant> kebab = level.registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT)
                .getOrThrow(PaintingVariants.KEBAB);

        boolean placed = PaintingPlacementService.tryPlacePainting(
                level, helper.absolutePos(waterPos), Direction.SOUTH, kebab, null, level.getRandom()
        );
        helper.assertTrue(placed, "Failed to place painting in water");

        BlockState state = helper.getBlockState(waterPos);
        helper.assertTrue(state.getValue(PaintingBlock.WATERLOGGED), "Painting is not waterlogged");

        PaintingBlockEntity be = helper.getBlockEntity(waterPos, PaintingBlockEntity.class);
        be.removeFootprint(level, false, null);

        BlockState restoredState = helper.getBlockState(waterPos);
        helper.assertTrue(restoredState.is(Blocks.WATER), "Water was deleted instead of preserved after painting removal");

        helper.succeed();
    }

    @GameTest
    public void testBackingWallDestruction(GameTestHelper helper) {
        BlockPos wallPos = new BlockPos(2, 2, 2);
        BlockPos paintingPos = new BlockPos(2, 2, 3);
        helper.setBlock(wallPos, Blocks.STONE);
        helper.setBlock(paintingPos, Blocks.AIR);

        ServerLevel level = helper.getLevel();
        Holder<PaintingVariant> kebab = level.registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT)
                .getOrThrow(PaintingVariants.KEBAB);

        PaintingPlacementService.tryPlacePainting(
                level, helper.absolutePos(paintingPos), Direction.SOUTH, kebab, null, level.getRandom()
        );

        // Break backing wall
        helper.setBlock(wallPos, Blocks.AIR);

        // Update neighbor
        BlockState paintingState = helper.getBlockState(paintingPos);
        if (paintingState.is(ModRegistry.PAINTING_BLOCK)) {
            PaintingBlockEntity be = helper.getBlockEntity(paintingPos, PaintingBlockEntity.class);
            if (!be.getFootprint().isSupported(level)) {
                be.removeFootprint(level, true, null);
            }
        }

        helper.assertTrue(helper.getBlockState(paintingPos).isAir(), "Painting did not drop after backing wall was removed");
        helper.succeed();
    }
}
