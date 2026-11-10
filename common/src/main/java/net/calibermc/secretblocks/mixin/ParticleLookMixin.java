package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.DisguiseLook;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Block particles from a secret block come from its look: the chips that
 * fly off the face being hit, and the burst when it breaks (its top, where
 * the break names the secret block itself and it is still there to read).
 */
@Mixin(ClientLevel.class)
public abstract class ParticleLookMixin {

	/** The chips, where the level reads the block being hit (NeoForge reads it in its overload with the hit). */
	@Group(name = "secretblocks$crackLook", min = 1, max = 1)
	@ModifyVariable(method = "addBreakingBlockEffect(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)V", require = 0,
			at = @At("STORE"))
	private BlockState secretblocks$crackLook(BlockState state, BlockPos pos, Direction side) {
		return DisguiseLook.looks((ClientLevel) (Object) this, pos, state, side);
	}

	@Group(name = "secretblocks$crackLook", min = 1, max = 1)
	@ModifyVariable(method = "addBreakingBlockEffect(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/phys/HitResult;)V",
			require = 0, at = @At("STORE"))
	private BlockState secretblocks$crackLookWithHit(BlockState state, BlockPos pos, Direction side, HitResult hit) {
		return DisguiseLook.looks((ClientLevel) (Object) this, pos, state, side);
	}

	@ModifyVariable(method = "addDestroyBlockEffect", at = @At("HEAD"), argsOnly = true)
	private BlockState secretblocks$destroyLook(BlockState state, BlockPos pos) {
		return DisguiseLook.looks((ClientLevel) (Object) this, pos, state, Direction.UP);
	}
}
