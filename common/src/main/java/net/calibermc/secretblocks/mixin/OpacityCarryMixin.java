package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.Opacity;
import net.calibermc.secretblocks.blocks.SecretBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A secret block's shape updates keep {@link Opacity#OPAQUE}: some vanilla
 * blocks rebuild their state from a default one (redstone dust does), which
 * would forget it.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class OpacityCarryMixin {

	@Shadow
	protected abstract BlockState asState();

	@Inject(method = "updateShape", at = @At("RETURN"), cancellable = true)
	private void secretblocks$keepOpaque(Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos,
			CallbackInfoReturnable<BlockState> cir) {
		BlockState self = asState();
		BlockState result = cir.getReturnValue();
		if (self.getBlock() instanceof SecretBlock && self.hasProperty(Opacity.OPAQUE) && result.hasProperty(Opacity.OPAQUE)
				&& result.getValue(Opacity.OPAQUE) != self.getValue(Opacity.OPAQUE)) {
			cir.setReturnValue(result.setValue(Opacity.OPAQUE, self.getValue(Opacity.OPAQUE)));
		}
	}
}
