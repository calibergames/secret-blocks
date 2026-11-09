package net.calibermc.secretblocks.neoforge.mixin;

import net.calibermc.secretblocks.render.SecretCulling;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Face culling beside secret blocks ({@link SecretCulling}). NeoForge's
 * block renderer asks the face test that also takes the level and the
 * block's position; the two-state one passes neither.
 */
@Mixin(Block.class)
public abstract class CullingMixin {

	@Inject(method = "shouldRenderFace(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z",
			at = @At("HEAD"), cancellable = true)
	private static void secretblocks$drawFace(BlockGetter level, BlockPos pos, BlockState self, BlockState neighbour, Direction side,
			CallbackInfoReturnable<Boolean> cir) {
		Boolean draw = SecretCulling.drawFace(self, level, pos, side, pos.relative(side));
		if (draw != null) {
			cir.setReturnValue(draw);
		}
	}
}
