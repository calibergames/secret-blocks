package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.DisguiseLook;
import net.calibermc.secretblocks.blocks.MiningFace;
import net.calibermc.secretblocks.blocks.SecretBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The client keeps the face where it started mining (for the mining time:
 * the server keeps the face of the start action too, so both count the same
 * time), and the hits while mining a secret block sound like the look of
 * the face being hit.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class ClientMiningLookMixin {

	@Shadow
	@Final
	private Minecraft minecraft;

	/**
	 * Where mining of a block starts: a creative break, or survival mining of
	 * a new block (also reached when mining moves on to another block:
	 * {@code continueDestroyBlock} starts it). A refused or repeated start
	 * keeps the face, as the server does.
	 */
	@Inject(method = "startDestroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/tutorial/Tutorial;onDestroyBlock(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;F)V"))
	private void secretblocks$keepStartFace(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
		keep(face);
	}

	/** A creative break while the attack key is held (the first call here; the others are survival progress). */
	@Inject(method = "continueDestroyBlock", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/tutorial/Tutorial;onDestroyBlock(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;F)V"))
	private void secretblocks$keepCreativeFace(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
		keep(face);
	}

	private void keep(Direction face) {
		if (minecraft.player != null) {
			((MiningFace) minecraft.player).secretblocks$setMiningFace(face);
		}
	}

	@ModifyVariable(method = "continueDestroyBlock", at = @At("STORE"))
	private SoundType secretblocks$hitLook(SoundType sound, BlockPos pos, Direction face) {
		BlockState state = minecraft.level.getBlockState(pos);
		return state.getBlock() instanceof SecretBlock ? DisguiseLook.shown(minecraft.level, pos, face).getSoundType() : sound;
	}
}
