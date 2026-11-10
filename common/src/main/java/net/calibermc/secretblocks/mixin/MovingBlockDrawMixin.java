package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.render.MovingBlockSource;
import net.calibermc.secretblocks.render.MovingDisguise;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.feature.BlockFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While each moving block is drawn, the piston move it came from (if any) is
 * the one secret blocks draw ({@link MovingDisguise}); the other blocks this
 * renderer draws come after the moving ones, with no move.
 */
@Mixin(BlockFeatureRenderer.class)
public abstract class MovingBlockDrawMixin {

	@ModifyVariable(method = "render", at = @At("STORE"))
	private MovingBlockRenderState secretblocks$begin(MovingBlockRenderState state) {
		MovingDisguise.begin(((MovingBlockSource) state).secretblocks$moving());
		return state;
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollection;getBlockSubmits()Ljava/util/List;"))
	private void secretblocks$afterMovingBlocks(CallbackInfo ci) {
		MovingDisguise.end();
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void secretblocks$end(CallbackInfo ci) {
		MovingDisguise.end();
	}
}
