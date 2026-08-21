package me.justbecause.fastpaintings.init;

import me.justbecause.fastpaintings.FastPaintings;
import me.justbecause.fastpaintings.block.PaintingBlock;
import me.justbecause.fastpaintings.block.PaintingPartBlock;
import me.justbecause.fastpaintings.block.entity.PaintingBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

public final class ModRegistry {

    public static final Identifier PAINTING_BLOCK_ID = FastPaintings.id("painting");
    public static final Identifier PAINTING_PART_BLOCK_ID = FastPaintings.id("painting_part");

    public static final ResourceKey<Block> PAINTING_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, PAINTING_BLOCK_ID);
    public static final ResourceKey<Block> PAINTING_PART_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, PAINTING_PART_BLOCK_ID);
    public static final ResourceKey<BlockEntityType<?>> PAINTING_BE_KEY = ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, PAINTING_BLOCK_ID);

    public static final TagKey<Block> PAINTINGS_BLOCK_TAG = TagKey.create(Registries.BLOCK, FastPaintings.id("paintings"));
    public static final TagKey<EntityType<?>> PAINTINGS_ENTITY_TYPE_TAG = TagKey.create(Registries.ENTITY_TYPE, FastPaintings.id("paintings"));

    public static final PaintingBlock PAINTING_BLOCK = new PaintingBlock(PAINTING_BLOCK_KEY);
    public static final PaintingPartBlock PAINTING_PART_BLOCK = new PaintingPartBlock(PAINTING_PART_BLOCK_KEY);

    public static final BlockEntityType<PaintingBlockEntity> PAINTING_BLOCK_ENTITY = new BlockEntityType<>(
            PaintingBlockEntity::new,
            Set.of(PAINTING_BLOCK)
    );

    public static void init() {
        Registry.register(BuiltInRegistries.BLOCK, PAINTING_BLOCK_ID, PAINTING_BLOCK);
        Registry.register(BuiltInRegistries.BLOCK, PAINTING_PART_BLOCK_ID, PAINTING_PART_BLOCK);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, PAINTING_BLOCK_ID, PAINTING_BLOCK_ENTITY);
    }

    private ModRegistry() {}
}
