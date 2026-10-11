package net.calibermc.secretblocks.fabric.mixin;

import net.calibermc.secretblocks.blocks.DisguiseLook;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Block particles from a secret block come from its look: the chips that
 * fly off the face being hit, and the burst when it breaks (its top, where
 * the break names the secret block itself and it is still there to read).
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleLookMixin {

	@Shadow
	protected ClientLevel level;

	@ModifyVariable(method = "crack", at = @At("STORE"))
	private BlockState secretblocks$crackLook(BlockState state, BlockPos pos, Direction side) {
		return DisguiseLook.looks(level, pos, state, side);
	}

	@ModifyVariable(method = "destroy", at = @At("HEAD"), argsOnly = true)
	private BlockState secretblocks$destroyLook(BlockState state, BlockPos pos) {
		return DisguiseLook.looks(level, pos, state, Direction.UP);
	}
}
