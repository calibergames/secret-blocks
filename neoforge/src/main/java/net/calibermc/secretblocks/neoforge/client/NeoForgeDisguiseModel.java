package net.calibermc.secretblocks.neoforge.client;

import net.calibermc.secretblocks.blocks.SecretBlock;
import net.calibermc.secretblocks.blocks.entity.Disguise;
import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.calibermc.secretblocks.render.BuilderView;
import net.calibermc.secretblocks.render.GogglesView;
import net.calibermc.secretblocks.render.MovingDisguise;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Every secret block's model on NeoForge: its parts come from its disguise
 * ({@link DisguiseQuads}), worked out where the chunk is built, one part per
 * layer the disguise draws in.
 */
final class NeoForgeDisguiseModel implements DynamicBlockStateModel {

	static final NeoForgeDisguiseModel INSTANCE = new NeoForgeDisguiseModel();

	/**
	 * A secret block whose disguise data is missing (for example one saved by
	 * an older build without a block entity, or not yet synced) draws as stone,
	 * the fallback for any unusable disguise. It must never draw nothing: that
	 * leaves an invisible block the player still collides with.
	 */
	private static final Disguise NO_DISGUISE = new Disguise();

	private NeoForgeDisguiseModel() {
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockModelPart> parts) {
		if (!(state.getBlock() instanceof SecretBlock)) {
			return;
		}
		DisguiseQuads quads = level.getBlockEntity(pos) instanceof DisguisedBlockEntity disguised
				? DisguiseQuads.build(disguised.getDisguise(), level, state, pos, RandomSource.create())
				: withoutDisguiseData(state);
		for (ChunkSectionLayer layer : quads.layers()) {
			parts.add(new Layer(quads, layer));
		}
	}

	@Override
	public TextureAtlasSprite particleIcon() {
		return stone();
	}

	private static TextureAtlasSprite stone() {
		return Minecraft.getInstance().getBlockRenderer().getBlockModel(Blocks.STONE.defaultBlockState()).particleIcon();
	}

	/** One layer of a disguise's quads. */
	private record Layer(DisguiseQuads quads, ChunkSectionLayer layer) implements BlockModelPart {

		@Override
		public List<BakedQuad> getQuads(@Nullable Direction side) {
			return quads.quads(layer, side);
		}

		@Override
		public boolean useAmbientOcclusion() {
			return true;
		}

		@Override
		public TextureAtlasSprite particleIcon() {
			return stone();
		}

		@Override
		public ChunkSectionLayer getRenderType(BlockState state) {
			return layer;
		}
	}

	/**
	 * Drawn where there is no disguised block entity: as part of a piston move
	 * (the disguise the move carries; a move that carries none draws as the
	 * fallback, never another block's disguise), or the fallback.
	 */
	private static DisguiseQuads withoutDisguiseData(BlockState state) {
		PistonMovingBlockEntity moving = MovingDisguise.moving();
		// Built once per block and view while the move is drawn.
		Unbuilt key = new Unbuilt(state, moving, BuilderView.isActive(), GogglesView.isWearing(), bakes);
		Built last = LAST.get();
		if (last != null && last.key().equals(key)) {
			return last.quads();
		}
		Disguise disguise = MovingDisguise.current();
		BlockPos pos = moving != null ? moving.getBlockPos() : BlockPos.ZERO;
		DisguiseQuads quads = DisguiseQuads.build(disguise != null ? disguise : NO_DISGUISE, Minecraft.getInstance().level, state, pos,
				RandomSource.create());
		LAST.set(new Built(key, quads));
		return quads;
	}

	/**
	 * What the quads drawn without disguise data depend on (the moving block
	 * entity by identity, and which model bake their sprites come from).
	 */
	private record Unbuilt(BlockState state, @Nullable PistonMovingBlockEntity moving, boolean builder, boolean goggles, int bake) {
		@Override
		public boolean equals(Object other) {
			return other instanceof Unbuilt that && state == that.state && moving == that.moving
					&& builder == that.builder && goggles == that.goggles && bake == that.bake;
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(state) * 31 + System.identityHashCode(moving);
		}
	}

	private record Built(Unbuilt key, DisguiseQuads quads) {
	}

	private static final ThreadLocal<Built> LAST = new ThreadLocal<>();
	private static volatile int bakes;

	/** After every model bake (a resource reload): quads cached before it hold stale sprites. */
	static void modelsBaked() {
		bakes++;
	}
}
