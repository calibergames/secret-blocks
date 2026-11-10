package net.calibermc.secretblocks.forge.client;

import net.calibermc.secretblocks.blocks.entity.Disguise;
import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.calibermc.secretblocks.render.BuilderView;
import net.calibermc.secretblocks.render.GogglesView;
import net.calibermc.secretblocks.render.MovingDisguise;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Collection;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Every secret block's model on Forge: its quads come from its
 * disguise ({@link DisguiseQuads}), worked out where the chunk is built, as
 * the block's model data.
 */
final class ForgeDisguiseModel implements BlockStateModel {

	private static final ModelProperty<DisguiseQuads> QUADS = new ModelProperty<>();
	/**
	 * A secret block whose disguise data is missing (for example one saved by
	 * an older build without a block entity, or not yet synced) draws as stone,
	 * the fallback for any unusable disguise. It must never draw nothing: that
	 * leaves an invisible block the player still collides with.
	 */
	private static final Disguise NO_DISGUISE = new Disguise();

	static final ForgeDisguiseModel INSTANCE = new ForgeDisguiseModel();

	private ForgeDisguiseModel() {
	}

	@Override
	public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData data) {
		BlockEntity entity = level.getBlockEntity(pos);
		Disguise disguise = entity instanceof DisguisedBlockEntity disguised ? disguised.getDisguise() : NO_DISGUISE;
		return data.derive().with(QUADS, DisguiseQuads.build(disguise, level, state, pos, RandomSource.create())).build();
	}

	@Override
	public @NotNull Collection<ChunkSectionLayer> getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
		// With no model data (a piston move, drawn by Forge with empty data), the layers of the quads drawn then.
		DisguiseQuads quads = data.get(QUADS);
		return (quads != null ? quads : withoutModelData(state)).layers();
	}

	/**
	 * The disguise's quads in one layer. With no model data (a piston move,
	 * drawn by Forge with empty data), the quads drawn then.
	 */
	@Override
	public void collectParts(RandomSource random, List<BlockModelPart> dest, ModelData data, @Nullable ChunkSectionLayer renderType) {
		DisguiseQuads quads = data.get(QUADS);
		if (quads == null) {
			PistonMovingBlockEntity moving = MovingDisguise.moving();
			if (moving == null) {
				return;
			}
			quads = withoutModelData(moving.getMovedState());
		}
		dest.add(new Layer(quads, renderType));
	}

	/** Not drawn through the plain path: every block render asks with model data and a layer. */
	@Override
	public void collectParts(RandomSource random, List<BlockModelPart> dest) {
	}

	/** One layer of a disguise's quads (every layer, for {@code null}). */
	private record Layer(DisguiseQuads quads, @Nullable ChunkSectionLayer layer) implements BlockModelPart {

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
	}

	/**
	 * Drawn with no model data: as part of a piston move (the disguise the move
	 * carries; a move that carries none draws as the fallback, never another
	 * block's disguise), or the fallback.
	 */
	private static DisguiseQuads withoutModelData(BlockState state) {
		PistonMovingBlockEntity moving = MovingDisguise.moving();
		// Forge asks for the layers, then each layer's quads side by side: built once per block and view.
		Unbuilt key = new Unbuilt(state, moving, BuilderView.isActive(), GogglesView.isWearing(), bakes);
		Built last = LAST.get();
		if (last != null && last.key().equals(key)) {
			return last.quads();
		}
		DisguiseQuads quads = buildWithoutModelData(state, moving);
		LAST.set(new Built(key, quads));
		return quads;
	}

	/**
	 * What the quads drawn without model data depend on (the moving block
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

	private static DisguiseQuads buildWithoutModelData(BlockState state, @Nullable PistonMovingBlockEntity moving) {
		Disguise disguise = MovingDisguise.current();
		if (disguise == null) {
			disguise = NO_DISGUISE;
		}
		BlockPos pos = moving != null ? moving.getBlockPos() : BlockPos.ZERO;
		return DisguiseQuads.build(disguise, Minecraft.getInstance().level, state, pos, RandomSource.create());
	}

	@Override
	public TextureAtlasSprite particleIcon() {
		return stone();
	}

	private static TextureAtlasSprite stone() {
		return Minecraft.getInstance().getBlockRenderer().getBlockModel(Blocks.STONE.defaultBlockState()).particleIcon();
	}
}
