package net.calibermc.secretblocks.forge.mixin;

import com.mojang.blaze3d.vertex.QuadInstance;
import net.calibermc.secretblocks.forge.client.QuadTints;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A disguise quad's colour (and, drawn faded, its alpha) on Forge: a quad
 * carries no colour of its own, and the block renderer colours it only
 * through the block's tint sources. A disguise quad holds its colour in its
 * tint index ({@link QuadTints}), which is applied here; every other quad is
 * drawn as before.
 */
@Mixin(ModelBlockRenderer.class)
public abstract class DisguiseQuadColorMixin {

	@Shadow
	@Final
	private QuadInstance quadInstance;

	@Inject(method = "putQuadWithTint", at = @At("HEAD"), cancellable = true)
	private void secretblocks$disguiseColor(BlockQuadOutput output, float x, float y, float z, BlockAndTintGetter level, BlockState state,
			BlockPos pos, BakedQuad quad, CallbackInfo ci) {
		int tintIndex = quad.materialInfo().tintIndex();
		if (QuadTints.holdsColor(tintIndex)) {
			quadInstance.multiplyColor(QuadTints.argb(quad));
			output.put(x, y, z, quad, quadInstance);
			ci.cancel();
		}
	}
}
