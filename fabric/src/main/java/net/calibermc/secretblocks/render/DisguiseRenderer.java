package net.calibermc.secretblocks.render;

import net.calibermc.secretblocks.blocks.DisguiseLook;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.calibermc.secretblocks.blocks.DoorBlock;
import net.calibermc.secretblocks.blocks.TrapdoorBlock;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
import java.util.function.Predicate;
import net.calibermc.secretblocks.blocks.entity.Disguise;
import net.calibermc.secretblocks.platform.Services;

/**
 * Client-only disguise rendering: turns a secret block's per-face disguise
 * (state, drawn side, rotation) into quads with the Fabric Renderer API.
 * Kept out of {@code SecretBlockEntity}, so the block entity holds only data
 * and loads on a dedicated server.
 */
@Environment(EnvType.CLIENT)
public final class DisguiseRenderer {

	/**
	 * Sodium 0.5 reads a vertex colour's alpha as ambient-occlusion shading,
	 * never as transparency, so a face faded by vertex alpha comes out dark and
	 * opaque there: with Sodium the Builder's view draws ghost faces, as under a
	 * shader pack.
	 */
	private static final boolean VERTEX_ALPHA_SHADES = Services.PLATFORM.isModLoaded("sodium");

	private DisguiseRenderer() {
	}

	public static void render(Disguise disguise, QuadEmitter emitter, BlockAndTintGetter blockView, BlockState state, BlockPos pos,
			RandomSource random, Predicate<Direction> cullTest) {

		boolean builder = BuilderView.isActive();
		boolean joinsAbove = builder && DoorHalves.joinsAbove(blockView, pos, state);
		boolean joinsBelow = builder && DoorHalves.joinsBelow(blockView, pos, state);

		CompoundTag tag = disguise.save(new CompoundTag()); // disguise keys only, not the inventory
		Minecraft minecraft = Minecraft.getInstance();

		for (Direction direction : Direction.values()) {

			BlockState tempState = Blocks.STONE.defaultBlockState();
			Direction tempDirection = Direction.NORTH;
			int tempRotation = 0;

			if (tag.contains(direction.name().toLowerCase() + "State")) {
				tempState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK, tag.getCompoundOrEmpty(direction.name().toLowerCase() + "State"));
			}

			if (tag.contains(direction.name().toLowerCase() + "Direction")) {
				tempDirection = Disguise.byName(tag.getStringOr(direction.name().toLowerCase() + "Direction", ""));
			}

			if (tag.contains(direction.name().toLowerCase() + "Rotation")) {
				tempRotation = tag.getIntOr(direction.name().toLowerCase() + "Rotation", 0);
			}

			if (!DisguiseLook.showsItself(tempState, blockView, pos, tempDirection)) {
				tempState = Blocks.STONE.defaultBlockState();
			}
			FabricMaterials.Material layer = builder ? null : DisguiseMaterials.forDisguise(tempState);

			BlockStateModel fullModel = minecraft.getModelManager().getBlockStateModelSet().get(tempState);
			random.setSeed(tempState.getSeed(pos));
			List<BakedQuad> abqList = quadsOf(fullModel, tempDirection, random);

			boolean ghost = builder && (BuilderView.shaderPack() || VERTEX_ALPHA_SHADES)
					&& BuilderView.ghostSprite() != null && !abqList.isEmpty();
			if (ghost) {
				// Under a shader pack or on Sodium: the faded block as a translucent ghost face (see BuilderView#shaderPack).
				emitShape(emitter, state, direction);
				bake(emitter, BuilderView.ghostSprite(), MutableQuadView.BAKE_LOCK_UV);
				emitter.tintIndex(-1);
				emitter.color(0xFFFF_FFFF, 0xFFFF_FFFF, 0xFFFF_FFFF, 0xFFFF_FFFF);
				FabricMaterials.FADE.applyTo(emitter);
				emitter.emit();
			}

			// A face that shows its look as it is (the same side, unturned, on a
			// full face) is drawn by the look's own model, as the look would be
			// drawn here: connected textures and random variants apply.
			boolean whole = !ghost && tempRotation == 0 && tempDirection == direction
					&& !(state.getBlock() instanceof DoorBlock) && !(state.getBlock() instanceof TrapdoorBlock);
			if (whole) {
				emitWhole(emitter, cullTest, fullModel, tempState, blockView, pos, random, direction, layer, builder);
			}

			for (BakedQuad quad : ghost || whole ? List.<BakedQuad>of() : abqList) {

				BakedQuad.MaterialInfo info = quad.materialInfo();

				emitShape(emitter, state, direction);

				bake(emitter, info.sprite(), MutableQuadView.BAKE_LOCK_UV);
				// The look's own pass for this face (from its texture), as vanilla draws it.
				emitter.chunkLayer(info.layer());

				rotateAndFlip(emitter, tempRotation, direction);

				int faceColor = BlockTints.color(tempState, blockView, pos, info.tintIndex());
				if (builder) {
					// Faded, so the real block drawn below shows through.
					faceColor = (faceColor & 0x00FF_FFFF) | (BuilderView.DISGUISE_ALPHA << 24);
					FabricMaterials.FADE.applyTo(emitter);
				} else if (layer != null) {
					// Leaves solid where the graphics settings draw them so.
					layer.applyTo(emitter);
				}
				emitter.tintIndex(-1);
				emitter.color(faceColor, faceColor, faceColor, faceColor);
				emitter.emit();
			}

			if (GogglesView.isWearing() && !abqList.isEmpty()
					&& GogglesView.overlaySprite() != null) {
				emitGogglesOverlay(emitter, state, direction);
			}
			if (builder && BuilderView.outlineSprite() != null) {
				emitOutline(emitter, state, direction, joinsAbove, joinsBelow);
			}
		}
		if (builder) {
			emitRealBlock(state, blockView, pos, random, emitter);
		}
	}

	/**
	 * Builder's view: the vanilla block this secret block stands in for (its
	 * counterpart), with its own properties, drawn solid inside
	 * the faded disguise. Shrunk very slightly so its full faces do not fight
	 * the disguise faces; tinted like the vanilla block (redstone power colour).
	 * Chests have no block model; {@link BuilderChestRenderer} draws them.
	 *
	 * The model's quads are read with the vanilla state and emitted one by
	 * one. Not through {@code bakedModelConsumer().accept(model, state)}:
	 * Indium 1.0.9 ignores that state and reads the model with the secret
	 * block's own, which crashed on redstone wire's multipart model.
	 */
	private static void emitRealBlock(BlockState state, BlockAndTintGetter blockView, BlockPos pos,
			RandomSource random, QuadEmitter emitter) {
		DisguiseShapes.realStateOf(state).ifPresent(vanilla -> {
			BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(vanilla);
			random.setSeed(vanilla.getSeed(pos));
			List<BlockStateModelPart> parts = new java.util.ArrayList<>();
			model.collectParts(random, parts);
			for (BlockStateModelPart part : parts) {
				FabricMaterials.Material material = FabricMaterials.real(part.useAmbientOcclusion());
				for (int index = 0; index <= Direction.values().length; index++) {
					Direction face = index < Direction.values().length ? Direction.values()[index] : null;
					for (BakedQuad quad : part.getQuads(face)) {
						emitter.fromBakedQuad(quad);
						emitter.cullFace(face);
						material.applyTo(emitter);
						for (int i = 0; i < 4; i++) {
							emitter.pos(i, shrink(emitter.x(i)), shrink(emitter.y(i)), shrink(emitter.z(i)));
						}
						if (quad.materialInfo().isTinted()) {
							int tint = BlockTints.color(vanilla, blockView, pos, quad.materialInfo().tintIndex());
							emitter.color(tint, tint, tint, tint);
							emitter.tintIndex(-1);
						}
						emitter.emit();
					}
				}
			}
		});
	}


	private static float shrink(float coordinate) {
		return 0.5f + (coordinate - 0.5f) * 0.996f;
	}


	/**
	 * One side of a look drawn by the look's own model, given a view where
	 * secret blocks read as their looks (so a connected-texture model works out
	 * its connections as for the look itself); only its quads on this side are
	 * kept, with their own texture coordinates, so random variants and
	 * rotations show as on the real block. Tints are the look's, and the
	 * material is the look's pass (or the Builder's Goggles' fade).
	 */
	private static void emitWhole(QuadEmitter emitter, Predicate<Direction> cullTest, BlockStateModel model, BlockState look,
			BlockAndTintGetter blockView, BlockPos pos, RandomSource random, Direction direction,
			@org.jetbrains.annotations.Nullable FabricMaterials.Material layer, boolean builder) {
		emitter.pushTransform(quad -> {
			Direction side = quad.cullFace() != null ? quad.cullFace() : quad.lightFace();
			if (side != direction) {
				return false;
			}
			finishWhole(quad, look, blockView, pos, layer, builder);
			return true;
		});
		try {
			random.setSeed(look.getSeed(pos));
			model.emitQuads(emitter, new LookView(blockView, direction), pos, look, random, cullTest);
		} finally {
			emitter.popTransform();
		}
	}

	/** A model's quads on one side (or none), from every part it draws. */
	private static List<BakedQuad> quadsOf(BlockStateModel model, @org.jetbrains.annotations.Nullable Direction side, RandomSource random) {
		List<BakedQuad> quads = new java.util.ArrayList<>();
		List<BlockStateModelPart> parts = new java.util.ArrayList<>();
		model.collectParts(random, parts);
		for (BlockStateModelPart part : parts) {
			quads.addAll(part.getQuads(side));
		}
		return quads;
	}

	private static void finishWhole(MutableQuadView quad, BlockState look, BlockAndTintGetter blockView, BlockPos pos,
			@org.jetbrains.annotations.Nullable FabricMaterials.Material layer, boolean builder) {
		int tint = 0xFFFF_FFFF;
		if (quad.tintIndex() != -1) {
			tint = BlockTints.color(look, blockView, pos, quad.tintIndex());
			quad.tintIndex(-1);
		}
		for (int i = 0; i < 4; i++) {
			int color = multiply(quad.color(i), tint);
			if (builder) {
				// Faded, so the real block drawn below shows through.
				color = (color & 0x00FF_FFFF) | (BuilderView.DISGUISE_ALPHA << 24);
			}
			quad.color(i, color);
		}
		if (builder) {
			FabricMaterials.FADE.applyTo(quad);
		} else if (layer != null) {
			layer.applyTo(quad);
		}
	}

	/** Two ARGB colours multiplied channel by channel. */
	private static int multiply(int a, int b) {
		int result = 0;
		for (int shift = 0; shift < 32; shift += 8) {
			int channel = ((a >>> shift) & 0xFF) * ((b >>> shift) & 0xFF) / 255;
			result |= channel << shift;
		}
		return result;
	}

	/** Textures the quad with {@code sprite} (its pass from the texture; the caller may change it). */
	private static void bake(QuadEmitter emitter, TextureAtlasSprite sprite, int flags) {
		emitter.materialBake(new Material.Baked(sprite, false), flags);
	}

	private static void emitShape(QuadEmitter emitter, BlockState state, Direction direction) {
		DisguiseShapes.shape(state, direction, emitter::square);
	}

	/** Turns a face's texture by its disguise's rotation, as {@link DisguiseShapes#uvOrder} orders the corners. */
	private static void rotateAndFlip(QuadEmitter emitter, int rotation, Direction dir) {
		float[] u = new float[4];
		float[] v = new float[4];
		for (int i = 0; i < 4; i++) {
			u[i] = emitter.u(i);
			v[i] = emitter.v(i);
		}
		int[] order = DisguiseShapes.uvOrder(rotation, dir);
		for (int i = 0; i < 4; i++) {
			emitter.uv(i, u[order[i]], v[order[i]]);
		}
	}


	/**
	 * Secret Goggles overlay: one translucent, full-bright copy of the
	 * face shape, textured with the animated rainbow stripes and pushed out
	 * 0.001 so it does not flicker against the disguise underneath.
	 */
	private static void emitGogglesOverlay(QuadEmitter emitter, BlockState state, Direction direction) {
		emitShape(emitter, state, direction);
		float dx = direction.getStepX() * 0.001f;
		float dy = direction.getStepY() * 0.001f;
		float dz = direction.getStepZ() * 0.001f;
		for (int i = 0; i < 4; i++) {
			emitter.pos(i, emitter.x(i) + dx, emitter.y(i) + dy, emitter.z(i) + dz);
		}
		bake(emitter, GogglesView.overlaySprite(), MutableQuadView.BAKE_LOCK_UV);
		emitter.tintIndex(-1);
		emitter.color(0xFFFF_FFFF, 0xFFFF_FFFF, 0xFFFF_FFFF, 0xFFFF_FFFF);
		FabricMaterials.OVERLAY.applyTo(emitter);
		emitter.emit();
	}

	/** How wide the builder's outline is, and how far it sits out from the face. */
	private static final float OUTLINE_WIDTH = 1.0f / 32.0f;
	private static final float OUTLINE_LIFT = 0.002f;
	/** Light blue, full-bright. */
	private static final int OUTLINE_COLOR = 0xFF4D_E6FF;

	/**
	 * Builder's view outline: a thin, bright frame around each disguise face,
	 * drawn into the chunk with the faded disguise (so it costs nothing per
	 * frame), out past the Secret Goggles' overlay. Faces the block shares
	 * with the other half of its door get no frame on that edge (and the
	 * faces between the halves none at all), so a door is one panel.
	 */
	private static void emitOutline(QuadEmitter emitter, BlockState state, Direction direction, boolean joinsAbove, boolean joinsBelow) {
		if ((direction == Direction.UP && joinsAbove) || (direction == Direction.DOWN && joinsBelow)) {
			return;
		}
		boolean side = direction.getAxis().isHorizontal();
		DisguiseShapes.shape(state, direction, (face, left, bottom, right, top, depth) -> {
			float w = OUTLINE_WIDTH;
			if (!(side && joinsBelow)) {
				emitOutlineStrip(emitter, face, left, bottom, right, Math.min(top, bottom + w), depth);
			}
			if (!(side && joinsAbove)) {
				emitOutlineStrip(emitter, face, left, Math.max(bottom, top - w), right, top, depth);
			}
			emitOutlineStrip(emitter, face, left, bottom, Math.min(right, left + w), top, depth);
			emitOutlineStrip(emitter, face, Math.max(left, right - w), bottom, right, top, depth);
		});
	}

	private static void emitOutlineStrip(QuadEmitter emitter, Direction face, float left, float bottom, float right, float top, float depth) {
		emitter.square(face, left, bottom, right, top, depth - OUTLINE_LIFT);
		if (Math.abs(depth) < 1.0e-5f) {
			// Lifted off the face, but hidden with it where a neighbour covers it.
			emitter.cullFace(face);
		}
		bake(emitter, BuilderView.outlineSprite(), MutableQuadView.BAKE_LOCK_UV);
		emitter.tintIndex(-1);
		emitter.color(OUTLINE_COLOR, OUTLINE_COLOR, OUTLINE_COLOR, OUTLINE_COLOR);
		FabricMaterials.OUTLINE.applyTo(emitter);
		emitter.emit();
	}




}
