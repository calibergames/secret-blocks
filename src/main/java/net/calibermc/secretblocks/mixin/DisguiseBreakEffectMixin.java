package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.DisguiseLook;
import net.calibermc.secretblocks.blocks.SecretBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A player breaking a secret block hears and sees the look of the face they
 * mined: the break event (sound and particles, played by every client) names
 * that look instead of the secret block.
 */
@Mixin(Block.class)
public abstract class DisguiseBreakEffectMixin {

	@Inject(method = "spawnDestroyParticles", at = @At("HEAD"), cancellable = true)
	private void secretblocks$disguiseBreak(Level level, Player player, BlockPos pos, BlockState state, CallbackInfo ci) {
		if (state.getBlock() instanceof SecretBlock) {
			level.levelEvent(player, 2001, pos, Block.getId(DisguiseLook.shown(level, pos, DisguiseLook.faceFor(player, pos))));
			ci.cancel();
		}
	}
}
