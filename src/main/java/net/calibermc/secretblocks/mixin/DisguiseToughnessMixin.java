package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.DisguiseLook;
import net.calibermc.secretblocks.blocks.SecretBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A secret block takes as long to mine as the look of the face being mined
 * ({@link DisguiseLook#destroyProgress}). Every mining path, client and
 * server, asks the block state.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class DisguiseToughnessMixin {

	@Shadow
	protected abstract BlockState asState();

	@Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
	private void secretblocks$disguiseProgress(Player player, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
		if (asState().getBlock() instanceof SecretBlock) {
			cir.setReturnValue(DisguiseLook.destroyProgress(player, level, pos));
		}
	}
}
