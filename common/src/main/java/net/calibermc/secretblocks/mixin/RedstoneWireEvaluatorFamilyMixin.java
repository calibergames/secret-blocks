package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.RedstoneWireFamily;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.RedstoneWireEvaluator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A wire's power evaluator reads the power of neighbouring wires of its own
 * block only; any wire block of the family counts (see
 * {@link RedStoneWireFamilyMixin}).
 */
@Mixin(RedstoneWireEvaluator.class)
public abstract class RedstoneWireEvaluatorFamilyMixin {

	@Shadow
	@Final
	protected RedstoneWireBlock wireBlock;

	@Redirect(method = "getWireSignal",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Ljava/lang/Object;)Z"))
	private boolean secretblocks$isAnyWire(BlockState state, Object block) {
		if (block == wireBlock) {
			return RedstoneWireFamily.isWire(state);
		}
		return state.is((Block) block);
	}
}
