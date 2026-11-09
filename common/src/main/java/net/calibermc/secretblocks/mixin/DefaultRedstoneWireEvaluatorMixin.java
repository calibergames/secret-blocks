package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.SecretRedstoneWire;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.DefaultRedstoneWireEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A Secret Redstone Wire directly above or below counts as a neighbouring
 * wire (power - 1). Vanilla wire only reads wires beside it and one step up
 * or down, so a wire resting on a Wire would otherwise get nothing from it.
 * The default wire rules only: worlds with the experimental redstone
 * setting compute wire power their own way.
 */
@Mixin(DefaultRedstoneWireEvaluator.class)
public abstract class DefaultRedstoneWireEvaluatorMixin {

	@Inject(method = "calculateTargetStrength", at = @At("RETURN"), cancellable = true)
	private void secretblocks$readWireAboveAndBelow(Level level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		int strength = cir.getReturnValueI();
		for (Direction direction : new Direction[] {Direction.UP, Direction.DOWN}) {
			BlockState neighbour = level.getBlockState(pos.relative(direction));
			if (neighbour.getBlock() instanceof SecretRedstoneWire) {
				strength = Math.max(strength, neighbour.getValue(RedStoneWireBlock.POWER) - 1);
			}
		}
		cir.setReturnValue(strength);
	}
}
