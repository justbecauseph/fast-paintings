package me.justbecause.fastpaintings.block;

import com.mojang.serialization.MapCodec;
import me.justbecause.fastpaintings.block.entity.PaintingBlockEntity;
import me.justbecause.fastpaintings.init.ModRegistry;
import me.justbecause.fastpaintings.painting.PaintingFootprint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class PaintingPartBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {

    public static final MapCodec<PaintingPartBlock> CODEC = simpleCodec(PaintingPartBlock::new);

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty OFFSET_X = IntegerProperty.create("offset_x", 0, 15);
    public static final IntegerProperty OFFSET_Y = IntegerProperty.create("offset_y", 0, 15);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public PaintingPartBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OFFSET_X, PaintingFootprint.ANCHOR_OFFSET_INDEX)
                .setValue(OFFSET_Y, PaintingFootprint.ANCHOR_OFFSET_INDEX)
                .setValue(WATERLOGGED, Boolean.FALSE));
    }

    public PaintingPartBlock(ResourceKey<Block> key) {
        this(Properties.of()
                .setId(key)
                .mapColor(MapColor.NONE)
                .noCollision()
                .noOcclusion()
                .instabreak()
                .sound(SoundType.WOOL)
                .pushReaction(PushReaction.DESTROY)
                .isRedstoneConductor((state, level, pos) -> false));
    }

    public static BlockPos getAnchorPos(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        int offsetX = state.getValue(OFFSET_X);
        int offsetY = state.getValue(OFFSET_Y);
        return PaintingFootprint.getAnchorPos(pos, facing, offsetX, offsetY);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OFFSET_X, OFFSET_Y, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PaintingBlock.getShapeForFacing(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof Projectile projectile) {
            if (level instanceof ServerLevel serverLevel
                    && projectile.mayInteract(serverLevel, pos)
                    && projectile.mayBreak(serverLevel)) {
                return this.getShape(state, level, pos, context);
            }
        }
        return Shapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos backingPos = pos.relative(facing.getOpposite());
        BlockState backingState = level.getBlockState(backingPos);
        return backingState.isSolid() || DiodeBlock.isDiode(backingState);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide()) {
            if (!this.canSurvive(state, level, pos)) {
                BlockPos anchorPos = getAnchorPos(pos, state);
                if (level.getBlockEntity(anchorPos) instanceof PaintingBlockEntity be) {
                    be.removeFootprint(level, true, null);
                } else {
                    level.removeBlock(pos, false);
                }
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos anchorPos = getAnchorPos(pos, state);
            if (level.getBlockEntity(anchorPos) instanceof PaintingBlockEntity be) {
                be.removeFootprint(level, !player.isCreative(), player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hitResult, Projectile projectile) {
        BlockPos pos = hitResult.getBlockPos();
        if (level instanceof ServerLevel serverLevel
                && projectile.mayInteract(serverLevel, pos)
                && projectile.mayBreak(serverLevel)) {
            BlockPos anchorPos = getAnchorPos(pos, state);
            if (level.getBlockEntity(anchorPos) instanceof PaintingBlockEntity be) {
                be.removeFootprint(level, true, projectile);
            }
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        BlockPos anchorPos = getAnchorPos(pos, state);
        if (level.getBlockEntity(anchorPos) instanceof PaintingBlockEntity be) {
            be.removeFootprint(level, true, null);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, net.minecraft.util.RandomSource random) {
        if (direction.getOpposite() == state.getValue(FACING) && !this.canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (state.getValue(WATERLOGGED)) {
            scheduledTickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = new ItemStack(Items.PAINTING);
        BlockPos anchorPos = getAnchorPos(pos, state);
        if (level.getBlockEntity(anchorPos) instanceof PaintingBlockEntity be && be.getVariant() != null) {
            stack.set(DataComponents.PAINTING_VARIANT, be.getVariant());
        }
        return stack;
    }
}
