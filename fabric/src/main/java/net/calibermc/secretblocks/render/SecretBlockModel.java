package net.calibermc.secretblocks.render;

import java.util.List;
import java.util.function.Predicate;

import net.calibermc.secretblocks.blocks.entity.Disguise;
import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Every secret block's model on Fabric: it draws the block's disguise
 * through the Fabric Renderer API ({@link DisguiseRenderer}). Renderers that
 * only collect plain model parts get none.
 */
@Environment(EnvType.CLIENT)
public class SecretBlockModel implements BlockStateModel {

	/**
	 * A secret block whose disguise data is missing (for example one saved by
	 * an older build without a block entity, or not yet synced) draws as stone,
	 * the renderer's fallback for any unusable disguise. It must never draw
	 * nothing: that leaves an invisible block the player still collides with.
	 */
	private static final Disguise NO_DISGUISE = new Disguise();

	@Override
	public void collectParts(RandomSource random, List<BlockModelPart> parts) {
	}

	@Override
	public TextureAtlasSprite particleIcon() {
		return Minecraft.getInstance().getBlockRenderer().getBlockModel(Blocks.STONE.defaultBlockState()).particleIcon();
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random,
			Predicate<@Nullable Direction> cullTest) {
		MovingDisguise.during(blockView, () -> emit(emitter, blockView, pos, state, random, cullTest));
	}

	private static void emit(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random,
			Predicate<@Nullable Direction> cullTest) {
		// Drawn as part of a piston move: the disguise the move carries (there is no block entity during it;
		// a move that carries none draws as the fallback, never another block's disguise).
		Disguise disguise = MovingDisguise.current();
		if (disguise == null && MovingDisguise.isDrawing()) {
			disguise = NO_DISGUISE;
		}
		if (disguise == null) {
			BlockEntity entity = blockView.getBlockEntity(pos);
			disguise = entity instanceof DisguisedBlockEntity disguised ? disguised.getDisguise() : NO_DISGUISE;
		}
		DisguiseRenderer.render(disguise, emitter, blockView, state, pos, random, cullTest);
	}
}
