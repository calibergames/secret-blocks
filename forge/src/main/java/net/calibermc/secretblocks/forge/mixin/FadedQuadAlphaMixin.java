package net.calibermc.secretblocks.forge.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.calibermc.secretblocks.forge.client.FadedQuad;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Forge's block renderer passes every quad an alpha of 1 and ignores the
 * alpha in its vertex colours, so a faded disguise quad ({@link FadedQuad})
 * would be drawn opaque. Its own alpha is applied here; every other quad is
 * drawn as before.
 */
@Mixin(ModelBlockRenderer.class)
public abstract class FadedQuadAlphaMixin {

	@ModifyArg(method = "putQuadData", index = 6, at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFFF[IIZ)V"))
	private float secretblocks$fadedAlpha(PoseStack.Pose pose, BakedQuad quad, float[] brightness, float red, float green, float blue,
			float alpha, int[] lights, int overlay, boolean readColor) {
		return quad instanceof FadedQuad faded ? alpha * faded.alpha() : alpha;
	}
}
