package net.calibermc.secretblocks.forge.mixin;

import net.calibermc.secretblocks.render.SecretCulling;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Face culling beside secret blocks ({@link SecretCulling}) in the vanilla
 * block renderer, which draws every block with a vanilla model. Its face
 * test sees only the two block states; the block's level and position come
 * from the method that draws it.
 */
@Mixin(ModelBlockRenderer.class)
public abstract class VanillaCullingMixin {

	private static final String DRAW_ARGS = "Lnet/minecraft/world/level/BlockAndTintGetter;"
			+ "Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;"
			+ "Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;"
			+ "ZLnet/minecraft/util/RandomSource;JILnet/minecraftforge/client/model/data/ModelData;"
			+ "Lnet/minecraft/client/renderer/RenderType;";

	// Forge's overloads with model data and render type: the plain ones only call them.
	@Redirect(method = {
			"tesselateWithAO(" + DRAW_ARGS + ")V",
			"tesselateWithoutAO(" + DRAW_ARGS + ")V"}, require = 2, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/Block;shouldRenderFace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"))
	private boolean secretblocks$drawFace(BlockState self, BlockState neighbour, Direction side,
			BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos) {
		Boolean draw = SecretCulling.drawFace(self, level, pos, side, pos.relative(side));
		return draw != null ? draw : Block.shouldRenderFace(self, neighbour, side);
	}
}
