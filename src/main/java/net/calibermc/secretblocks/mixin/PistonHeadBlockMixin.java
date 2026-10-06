package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.SecretPiston;
import net.calibermc.secretblocks.blocks.SecretPistonHead;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla's head belongs only to a vanilla piston of its type. A Secret
 * Piston head belongs to a Secret Piston of its type, extended and facing the
 * same way; this decides whether the head survives, whether
 * breaking it breaks the base, and the reverse.
 */
@Mixin(PistonHeadBlock.class)
public abstract class PistonHeadBlockMixin {

	@Inject(method = "isFittingBase", at = @At("HEAD"), cancellable = true)
	private void secretblocks$fittingBase(BlockState head, BlockState base, CallbackInfoReturnable<Boolean> cir) {
		boolean secretHead = head.getBlock() instanceof SecretPistonHead;
		if (secretHead || base.getBlock() instanceof SecretPiston) {
			cir.setReturnValue(secretHead && base.getBlock() instanceof SecretPiston piston
					&& piston.isSticky() == (head.getValue(PistonHeadBlock.TYPE) == PistonType.STICKY)
					&& base.getValue(PistonBaseBlock.EXTENDED)
					&& base.getValue(PistonBaseBlock.FACING) == head.getValue(PistonHeadBlock.FACING));
		}
	}
}
