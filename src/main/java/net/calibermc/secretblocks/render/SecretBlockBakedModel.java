package net.calibermc.secretblocks.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.calibermc.secretblocks.blocks.entity.Disguise;
import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class SecretBlockBakedModel implements FabricBakedModel, BakedModel {



	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, net.minecraft.util.RandomSource random) {
		// Quads come from emitBlockQuads (Fabric Renderer API); renderers that
		// only use getQuads get none. Never null (BakedModel contract).
		return new ArrayList<>();
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
		return Minecraft.getInstance().getModelManager().getModel(new ModelResourceLocation("minecraft:block/block")).getTransforms();
	}

	@Override
	public ItemOverrides getOverrides() {
		return ItemOverrides.EMPTY;
	}

	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	/**
	 * A secret block whose disguise data is missing (for example one saved by
	 * an older build without a block entity, or not yet synced) draws as stone,
	 * the renderer's fallback for any unusable disguise. It must never draw
	 * nothing: that leaves an invisible block the player still collides with.
	 */
	private static final Disguise NO_DISGUISE = new Disguise();

	@Override
	public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<net.minecraft.util.RandomSource> randomSupplier, RenderContext context) {
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
		DisguiseRenderer.render(disguise, blockView, state, pos, randomSupplier, context);
	}

	@Override
	public void emitItemQuads(ItemStack stack, Supplier<net.minecraft.util.RandomSource> randomSupplier, RenderContext context) {

	}




}
