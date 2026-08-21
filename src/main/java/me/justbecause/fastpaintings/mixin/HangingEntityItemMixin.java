package me.justbecause.fastpaintings.mixin;

import me.justbecause.fastpaintings.FastPaintings;
import me.justbecause.fastpaintings.painting.PaintingPlacementService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HangingEntityItem.class)
public abstract class HangingEntityItemMixin extends Item {

    @Shadow
    @Final
    private EntityType<? extends HangingEntity> type;

    @Shadow
    protected abstract boolean mayPlace(Player player, Direction direction, ItemStack itemStack, BlockPos blockPos);

    public HangingEntityItemMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void fastpaintings$interceptPaintingPlacement(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (this.type == EntityTypes.PAINTING && FastPaintings.CONFIG.convertOnPlacement) {
            BlockPos clickedPos = context.getClickedPos();
            Direction clickedFace = context.getClickedFace();
            BlockPos blockPos = clickedPos.relative(clickedFace);
            Player player = context.getPlayer();
            ItemStack itemInHand = context.getItemInHand();

            if (player != null && !this.mayPlace(player, clickedFace, itemInHand, blockPos)) {
                cir.setReturnValue(InteractionResult.FAIL);
                return;
            }

            Level level = context.getLevel();
            if (level.isClientSide()) {
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }

            Holder<PaintingVariant> forcedVariant = itemInHand.get(DataComponents.PAINTING_VARIANT);
            boolean placed = PaintingPlacementService.tryPlacePainting(
                    level, blockPos, clickedFace, forcedVariant, player, level.getRandom()
            );

            if (placed) {
                if (player == null || !player.hasInfiniteMaterials()) {
                    itemInHand.shrink(1);
                }
                cir.setReturnValue(InteractionResult.SUCCESS);
            } else {
                cir.setReturnValue(InteractionResult.CONSUME);
            }
        }
    }
}
