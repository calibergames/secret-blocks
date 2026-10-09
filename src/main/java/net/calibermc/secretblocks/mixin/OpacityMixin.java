package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.Opacity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds {@link Opacity#OPAQUE} to the secret blocks that carry it (one place,
 * for every block class: vanilla's and ours), and makes a see-through one let
 * light and sky light through and shade its neighbours as glass does.
 */
@Mixin(Block.class)
public abstract class OpacityMixin {

	@Redirect(method = "<init>", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/StateDefinition$Builder;create(Ljava/util/function/Function;Lnet/minecraft/world/level/block/state/StateDefinition$Factory;)Lnet/minecraft/world/level/block/state/StateDefinition;"))
	private StateDefinition<Block, BlockState> secretblocks$addOpaque(StateDefinition.Builder<Block, BlockState> builder,
			java.util.function.Function<Block, BlockState> defaultState, StateDefinition.Factory<Block, BlockState> factory) {
		if (Opacity.carries((Block) (Object) this)) {
			builder.add(Opacity.OPAQUE);
		}
		return builder.create(defaultState, factory);
	}

	@Inject(method = "propagatesSkylightDown", at = @At("HEAD"), cancellable = true)
	private void secretblocks$skyLight(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		// As glass, unless it holds water (a waterlogged secret stair or slab), as vanilla does.
		if (Opacity.seeThrough(state) && state.getFluidState().isEmpty()) {
			cir.setReturnValue(true);
		}
	}
}
