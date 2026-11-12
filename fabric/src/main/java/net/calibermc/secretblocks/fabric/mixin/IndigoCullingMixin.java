package net.calibermc.secretblocks.fabric.mixin;

import net.calibermc.secretblocks.render.SecretCulling;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Face culling beside secret blocks ({@link SecretCulling}) in Fabric API's
 * own renderer (Indigo), which draws the secret blocks' model and any other
 * model that is not a plain vanilla one. Its face test sees only the two
 * block states; the block being drawn is in the renderer. The result
 * still goes through Indigo's per-face cache.
 */
@Environment(EnvType.CLIENT)
@Mixin(value = AltModelBlockRendererImpl.class, remap = false)
public abstract class IndigoCullingMixin {

	@Shadow
	private BlockAndTintGetter level;

	@Shadow
	private BlockPos pos;

	@Redirect(method = "shouldCullFace", at = @At(value = "INVOKE", remap = true,
			target = "Lnet/minecraft/world/level/block/Block;shouldRenderFace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"))
	private boolean secretblocks$drawFace(BlockState self, BlockState neighbour, Direction side) {
		Boolean draw = SecretCulling.drawFace(self, level, pos, side, pos.relative(side));
		return draw != null ? draw : Block.shouldRenderFace(self, neighbour, side);
	}
}
