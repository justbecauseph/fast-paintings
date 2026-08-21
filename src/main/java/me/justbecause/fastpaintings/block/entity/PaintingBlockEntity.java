package me.justbecause.fastpaintings.block.entity;

import me.justbecause.fastpaintings.block.PaintingBlock;
import me.justbecause.fastpaintings.block.PaintingPartBlock;
import me.justbecause.fastpaintings.init.ModRegistry;
import me.justbecause.fastpaintings.painting.PaintingFootprint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PaintingBlockEntity extends BlockEntity {

    private @Nullable Holder<PaintingVariant> variant;
    private boolean isRemoving = false;

    public PaintingBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.PAINTING_BLOCK_ENTITY, pos, state);
    }

    public Direction getFacing() {
        BlockState state = this.getBlockState();
        return state.hasProperty(PaintingBlock.FACING) ? state.getValue(PaintingBlock.FACING) : Direction.NORTH;
    }

    public @Nullable Holder<PaintingVariant> getVariant() {
        return this.variant;
    }

    public void setVariant(@Nullable Holder<PaintingVariant> variant) {
        this.variant = variant;
        this.setChanged();
        if (this.level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    public int getPaintingWidth() {
        return this.variant != null ? this.variant.value().width() : 1;
    }

    public int getPaintingHeight() {
        return this.variant != null ? this.variant.value().height() : 1;
    }

    public PaintingFootprint getFootprint() {
        return PaintingFootprint.of(this.worldPosition, this.getFacing(), this.getPaintingWidth(), this.getPaintingHeight());
    }

    public AABB getRenderBoundingBox() {
        return this.getFootprint().boundingBox().inflate(0.5);
    }

    /**
     * Safely destroys all blocks in this painting's multi-block footprint, preventing recursive removal.
     */
    public void removeFootprint(Level level, boolean dropItem, @Nullable Entity causedBy) {
        if (this.isRemoving) {
            return;
        }
        this.isRemoving = true;

        PaintingFootprint footprint = this.getFootprint();

        if (dropItem && level instanceof ServerLevel serverLevel) {
            this.dropItem(serverLevel, causedBy);
        }

        // Remove all helper parts and this anchor
        for (BlockPos pos : footprint.occupiedCells()) {
            if (pos.equals(this.worldPosition)) {
                continue;
            }
            BlockState cellState = level.getBlockState(pos);
            if (cellState.is(ModRegistry.PAINTING_PART_BLOCK)) {
                BlockPos anchorPos = PaintingPartBlock.getAnchorPos(pos, cellState);
                if (this.worldPosition.equals(anchorPos)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        }

        level.setBlock(this.worldPosition, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
    }

    public void dropItem(ServerLevel serverLevel, @Nullable Entity causedBy) {
        if (serverLevel.getGameRules().get(GameRules.ENTITY_DROPS)) {
            serverLevel.playSound(null, this.worldPosition, SoundEvents.PAINTING_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!(causedBy instanceof Player player && player.hasInfiniteMaterials())) {
                Direction facing = this.getFacing();
                Vec3 center = this.getFootprint().boundingBox().getCenter();
                ItemStack stack = new ItemStack(Items.PAINTING);
                if (me.justbecause.fastpaintings.FastPaintings.CONFIG.preserveVariantOnDrop && this.variant != null) {
                    stack.set(net.minecraft.core.component.DataComponents.PAINTING_VARIANT, this.variant);
                }
                ItemEntity itemEntity = new ItemEntity(
                        serverLevel,
                        center.x + facing.getStepX() * 0.15,
                        center.y,
                        center.z + facing.getStepZ() * 0.15,
                        stack
                );
                itemEntity.setDefaultPickUpDelay();
                serverLevel.addFreshEntity(itemEntity);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.variant != null) {
            VariantUtils.writeVariant(output, this.variant);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        VariantUtils.readVariant(input, Registries.PAINTING_VARIANT).ifPresent(this::setVariant);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }
}
