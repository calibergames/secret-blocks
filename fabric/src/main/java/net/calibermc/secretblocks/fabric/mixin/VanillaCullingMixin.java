package net.calibermc.secretblocks.fabric.mixin;

import net.calibermc.secretblocks.render.SecretCulling;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Face culling beside secret blocks ({@link SecretCulling}) in the vanilla
 * block renderer, which draws every block with a vanilla model. Its face test
 * is given the level, the block's state and the neighbour's position.
 */
@Mixin(ModelBlockRenderer.class)
public abstract class VanillaCullingMixin {

	@Shadow
	@Final
	private boolean cull;

	@Inject(method = "shouldRenderFace(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z",
			at = @At("HEAD"), cancellable = true)
	private void secretblocks$drawFace(BlockAndTintGetter level, BlockState self, Direction side, BlockPos neighbourPos,
			CallbackInfoReturnable<Boolean> cir) {
		if (cull) {
			BlockPos neighbour = neighbourPos.immutable();
			Boolean draw = SecretCulling.drawFace(self, level, neighbour.relative(side.getOpposite()), side, neighbour);
			if (draw != null) {
				cir.setReturnValue(draw);
			}
		}
	}
}
