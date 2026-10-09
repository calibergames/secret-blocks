package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.DisguiseLook;
import net.calibermc.secretblocks.blocks.SecretBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A secret block broken by the world (a piston, a falling block, a mob)
 * sounds and looks like the face toward whatever broke it, or its top.
 */
@Mixin(Level.class)
public abstract class LevelDestroyBlockMixin {

	@Redirect(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getId(Lnet/minecraft/world/level/block/state/BlockState;)I"))
	private int secretblocks$disguiseBreak(BlockState state, BlockPos pos, boolean dropBlock, @Nullable Entity entity) {
		if (state.getBlock() instanceof SecretBlock) {
			return Block.getId(DisguiseLook.shown((Level) (Object) this, pos, DisguiseLook.faceFor(entity, pos)));
		}
		return Block.getId(state);
	}
}
