package me.justbecause.fastpaintings.compat.distantdecorations;

import me.justbecause.distantdecorations.api.DecorationProvider;
import me.justbecause.distantdecorations.api.DecorationRegistry;
import me.justbecause.distantdecorations.api.DecorationType;
import me.justbecause.fastpaintings.FastPaintings;
import me.justbecause.fastpaintings.block.PaintingBlock;
import me.justbecause.fastpaintings.block.entity.PaintingBlockEntity;
import me.justbecause.fastpaintings.painting.PaintingFootprint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public class FastPaintingDistantDecorationProvider implements DecorationProvider<FastPaintingDistantData> {

    public static final Identifier TYPE_ID = Identifier.fromNamespaceAndPath(FastPaintings.MOD_ID, "painting");

    public static final DecorationType<FastPaintingDistantData> TYPE = new DecorationType<>(
        TYPE_ID,
        (data, buf) -> {
            buf.writeIdentifier(data.assetId());
            buf.writeByte(data.direction().get2DDataValue());
            buf.writeVarInt(data.width());
            buf.writeVarInt(data.height());
        },
        buf -> new FastPaintingDistantData(
            buf.readIdentifier(),
            Direction.from2DDataValue(buf.readByte()),
            buf.readVarInt(),
            buf.readVarInt()
        )
    );

    public static void init() {
        DecorationRegistry.registerProvider(new FastPaintingDistantDecorationProvider());
    }

    @Override
    public DecorationType<FastPaintingDistantData> type() {
        return TYPE;
    }

    @Override
    public boolean matches(BlockEntity blockEntity) {
        return blockEntity instanceof PaintingBlockEntity;
    }

    @Override
    public @Nullable FastPaintingDistantData capture(ServerLevel level, BlockPos pos, BlockEntity blockEntity) {
        if (blockEntity instanceof PaintingBlockEntity pbe) {
            Holder<PaintingVariant> variantHolder = pbe.getVariant();
            if (variantHolder == null) {
                return null;
            }
            Identifier variantId = variantHolder.unwrapKey().map(k -> k.identifier()).orElse(null);
            Direction facing = pbe.getFacing();
            PaintingVariant variant = variantHolder.value();
            return new FastPaintingDistantData(variant.assetId(), facing, variant.width(), variant.height());
        }
        return null;
    }

    @Override
    public AABB calculateBounds(ServerLevel level, BlockPos pos, FastPaintingDistantData data) {
        return PaintingFootprint.calculateBoundingBox(pos, data.direction(), data.width(), data.height());
    }
}
