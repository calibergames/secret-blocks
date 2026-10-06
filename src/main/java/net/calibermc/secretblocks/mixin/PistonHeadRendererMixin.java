package net.calibermc.secretblocks.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.calibermc.secretblocks.blocks.SecretPiston;
import net.calibermc.secretblocks.blocks.SecretPistonHead;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.calibermc.secretblocks.render.MovingDisguise;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.PistonHeadRenderer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drawing a move (nothing vanilla shows during it): while vanilla draws a
 * moving block, the disguise it carries is the one secret blocks draw
 * ({@link MovingDisguise}), and a Secret Piston's head is drawn as its own
 * disguised head, never vanilla's.
 */
@Mixin(PistonHeadRenderer.class)
public abstract class PistonHeadRendererMixin {

	@Inject(method = "render(Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
			at = @At("HEAD"))
	private void secretblocks$begin(PistonMovingBlockEntity moving, float partialTick, PoseStack poses, MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
		MovingDisguise.begin(moving);
	}

	@Inject(method = "render(Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
			at = @At("RETURN"))
	private void secretblocks$end(PistonMovingBlockEntity moving, float partialTick, PoseStack poses, MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
		MovingDisguise.end();
	}

	@Redirect(method = "render(Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
			require = 2, at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/Blocks;PISTON_HEAD:Lnet/minecraft/world/level/block/Block;"))
	private Block secretblocks$head() {
		PistonMovingBlockEntity moving = MovingDisguise.moving();
		if (moving != null) {
			Block moved = moving.getMovedState().getBlock();
			if (moved instanceof SecretPiston || moved instanceof SecretPistonHead) {
				return ModBlocks.SECRET_PISTON_HEAD;
			}
		}
		return Blocks.PISTON_HEAD;
	}
}
