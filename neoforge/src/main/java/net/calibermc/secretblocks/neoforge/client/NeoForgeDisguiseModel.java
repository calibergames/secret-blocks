package net.calibermc.secretblocks.neoforge.client;

import net.calibermc.secretblocks.blocks.entity.Disguise;
import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.calibermc.secretblocks.render.BuilderView;
import net.calibermc.secretblocks.render.GogglesView;
import net.calibermc.secretblocks.render.MovingDisguise;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Every secret block's model on NeoForge and Forge: its quads come from its
 * disguise ({@link DisguiseQuads}), worked out where the chunk is built, as
 * the block's model data.
 */
final class NeoForgeDisguiseModel implements IDynamicBakedModel {

	private static final ModelProperty<DisguiseQuads> QUADS = new ModelProperty<>();
	/**
	 * A secret block whose disguise data is missing (for example one saved by
	 * an older build without a block entity, or not yet synced) draws as stone,
	 * the fallback for any unusable disguise. It must never draw nothing: that
	 * leaves an invisible block the player still collides with.
	 */
	private static final Disguise NO_DISGUISE = new Disguise();

	private final ItemTransforms transforms;

	NeoForgeDisguiseModel(ItemTransforms transforms) {
		this.transforms = transforms;
	}

	@Override
	public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData data) {
		BlockEntity entity = level.getBlockEntity(pos);
		Disguise disguise = entity instanceof DisguisedBlockEntity disguised ? disguised.getDisguise() : NO_DISGUISE;
		return data.derive().with(QUADS, DisguiseQuads.build(disguise, level, state, pos, RandomSource.create())).build();
	}

	@Override
	public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
		// With no model data (a piston move, drawn by Forge with empty data), the layers of the quads drawn then.
		DisguiseQuads quads = data.get(QUADS);
		return (quads != null ? quads : withoutModelData(state)).layers();
	}

	@Override
	public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
			@NotNull ModelData data, @Nullable RenderType renderType) {
		if (state == null) {
			return List.of();
		}
		DisguiseQuads quads = data.get(QUADS);
		if (quads == null) {
			quads = withoutModelData(state);
		}
		return quads.quads(renderType, side);
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
	public boolean useAmbientOcclusion() {
		return true;
	}

	@Override
	public boolean isGui3d() {
		return false;
	}

	@Override
	public boolean usesBlockLight() {
		return true;
	}

	@Override
	public boolean isCustomRenderer() {
		return false;
	}

	@Override
	public TextureAtlasSprite getParticleIcon() {
		return Minecraft.getInstance().getBlockRenderer().getBlockModel(Blocks.STONE.defaultBlockState()).getParticleIcon();
	}

	@Override
	public ItemTransforms getTransforms() {
		return transforms;
	}

	@Override
	public ItemOverrides getOverrides() {
		return ItemOverrides.EMPTY;
	}
}
