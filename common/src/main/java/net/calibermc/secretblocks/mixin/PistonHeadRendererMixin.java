package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.SecretPiston;
import net.calibermc.secretblocks.blocks.SecretPistonHead;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.calibermc.secretblocks.render.MovingBlockSource;
import net.calibermc.secretblocks.render.MovingDisguise;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.PistonHeadRenderer;
import net.minecraft.client.renderer.blockentity.state.PistonHeadRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A piston move's render states (nothing vanilla shows during it): each
 * moving block's state carries the move ({@link MovingBlockSource}), so that
 * when it is drawn later the disguise it carries is the one secret blocks
 * draw ({@code MovingBlockDrawMixin}), and a Secret Piston's head is a Secret
 * Piston Head, drawn as its own disguised head, never vanilla's.
 */
@Mixin(PistonHeadRenderer.class)
public abstract class PistonHeadRendererMixin {

	private static final String EXTRACT = "extractRenderState(Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;"
			+ "Lnet/minecraft/client/renderer/blockentity/state/PistonHeadRenderState;FLnet/minecraft/world/phys/Vec3;"
			+ "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V";

	@Inject(method = EXTRACT, at = @At("HEAD"))
	private void secretblocks$begin(PistonMovingBlockEntity moving, PistonHeadRenderState state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling, CallbackInfo ci) {
		MovingDisguise.begin(moving);
	}

	@Inject(method = EXTRACT, at = @At("RETURN"))
	private void secretblocks$end(PistonMovingBlockEntity moving, PistonHeadRenderState state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling, CallbackInfo ci) {
		for (MovingBlockRenderState block : new MovingBlockRenderState[] {state.block, state.base}) {
			if (block != null) {
				((MovingBlockSource) block).secretblocks$setMoving(moving);
			}
		}
		MovingDisguise.end();
	}

	@Redirect(method = EXTRACT, require = 2,
			at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/Blocks;PISTON_HEAD:Lnet/minecraft/world/level/block/Block;"))
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
