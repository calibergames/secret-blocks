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
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;spawnDestroyParticles(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
	private void secretblocks$disguiseBreak(Block block, Level level, BlockPos pos, BlockState state,
			BlockPos destroyed, boolean dropBlock, @Nullable Entity entity) {
		if (block instanceof SecretBlock) {
			level.levelEvent(2001, pos, Block.getId(DisguiseLook.shown(level, pos, DisguiseLook.faceFor(entity, pos))));
		} else {
			block.spawnDestroyParticles(level, pos, state);
		}
	}
}
