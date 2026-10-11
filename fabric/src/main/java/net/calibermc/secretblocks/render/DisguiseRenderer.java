package net.calibermc.secretblocks.render;

import net.calibermc.secretblocks.blocks.DisguiseLook;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.calibermc.secretblocks.blocks.DoorBlock;
import net.calibermc.secretblocks.blocks.TrapdoorBlock;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
import java.util.function.Supplier;
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

	public static void render(Disguise disguise, BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<net.minecraft.util.RandomSource> randomSupplier, RenderContext context) {

		QuadEmitter emitter = context.getEmitter();
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
				tempState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound(direction.name().toLowerCase() + "State"));
			}

			if (tag.contains(direction.name().toLowerCase() + "Direction")) {
				tempDirection = Disguise.byName(tag.getString(direction.name().toLowerCase() + "Direction"));
			}

			if (tag.contains(direction.name().toLowerCase() + "Rotation")) {
				tempRotation = tag.getInt(direction.name().toLowerCase() + "Rotation");
			}

			if (!DisguiseLook.showsItself(tempState, blockView, pos, tempDirection)) {
				tempState = Blocks.STONE.defaultBlockState();
			}
			RenderMaterial layer = builder ? null : DisguiseMaterials.forDisguise(tempState);

			BakedModel fullModel = minecraft.getBlockRenderer().getBlockModel(tempState);
			List<BakedQuad> abqList = fullModel.getQuads(tempState, tempDirection,  randomSupplier.get());

			BlockColors colors = minecraft.getBlockColors();
			int color = 0xFF00_0000 | colors.getColor(tempState, blockView, pos, 0xFF);

			boolean ghost = builder && (BuilderView.shaderPack() || VERTEX_ALPHA_SHADES)
					&& BuilderView.ghostSprite() != null && !abqList.isEmpty();
			if (ghost) {
				// Under a shader pack or on Sodium: the faded block as a translucent ghost face (see BuilderView#shaderPack).
				emitShape(emitter, state, direction);
				emitter.spriteBake(0, BuilderView.ghostSprite(), MutableQuadView.BAKE_LOCK_UV);
				emitter.colorIndex(-1);
				emitter.spriteColor(0, 0xFFFF_FFFF, 0xFFFF_FFFF, 0xFFFF_FFFF, 0xFFFF_FFFF);
				emitter.material(FabricMaterials.fade());
				emitter.emit();
			}

			// A face that shows its look as it is (the same side, unturned, on a
			// full face) is drawn by the look's own model, as the look would be
			// drawn here: connected textures and random variants apply.
			boolean whole = !ghost && tempRotation == 0 && tempDirection == direction
					&& !(state.getBlock() instanceof DoorBlock) && !(state.getBlock() instanceof TrapdoorBlock);
			if (whole) {
				emitWhole(context, emitter, fullModel, abqList, tempState, blockView, pos, randomSupplier, direction, layer, builder);
			}

			for (BakedQuad quad : ghost || whole ? List.<BakedQuad>of() : abqList) {

				TextureAtlasSprite quadSprite = quad.getSprite();

				emitShape(emitter, state, direction);

				emitter.spriteBake(0, quadSprite, 4);

				rotateAndFlip(emitter, tempRotation, direction);

				int faceColor = quad.isTinted() ? color : 0xFFFF_FFFF;
				if (builder) {
					// Faded, so the real block drawn below shows through.
					faceColor = (faceColor & 0x00FF_FFFF) | (BuilderView.DISGUISE_ALPHA << 24);
					emitter.material(FabricMaterials.fade());
				} else if (layer != null) {
					// In the disguise's own pass: stained glass translucent, leaves as the graphics setting has them.
					emitter.material(layer);
				}
				emitter.colorIndex(quad.isTinted() ? 0 : -1);
				emitter.spriteColor(0, faceColor, faceColor, faceColor, faceColor);
				emitter.emit();
			}

			if (GogglesView.isWearing() && !abqList.isEmpty()
					&& GogglesView.overlaySprite() != null && FabricMaterials.overlay() != null) {
				emitGogglesOverlay(emitter, state, direction);
			}
			if (builder && BuilderView.outlineSprite() != null) {
				emitOutline(emitter, state, direction, joinsAbove, joinsBelow);
			}
		}
		if (builder) {
			emitRealBlock(state, blockView, pos, randomSupplier, context);
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
			Supplier<net.minecraft.util.RandomSource> randomSupplier, RenderContext context) {
		DisguiseShapes.realStateOf(state).ifPresent(vanilla -> {
			BlockColors colors = Minecraft.getInstance().getBlockColors();
			BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(vanilla);
			QuadEmitter emitter = context.getEmitter();
			RenderMaterial material = FabricMaterials.real(model.useAmbientOcclusion());
			for (int index = 0; index <= Direction.values().length; index++) {
				Direction face = index < Direction.values().length ? Direction.values()[index] : null;
				for (BakedQuad quad : model.getQuads(vanilla, face, randomSupplier.get())) {
					emitter.fromVanilla(quad, material, face);
					for (int i = 0; i < 4; i++) {
						emitter.pos(i, shrink(emitter.x(i)), shrink(emitter.y(i)), shrink(emitter.z(i)));
					}
					if (quad.isTinted()) {
						int tint = 0xFF00_0000 | colors.getColor(vanilla, blockView, pos, quad.getTintIndex());
						emitter.spriteColor(0, tint, tint, tint, tint);
						emitter.colorIndex(-1);
					}
					emitter.emit();
				}
			}
		});
	}


	private static float shrink(float coordinate) {
		return 0.5f + (coordinate - 0.5f) * 0.996f;
	}


	/**
	 * One side of a look drawn by the look's own model. A model that renders
	 * itself (connected textures, for one) is given a view where secret
	 * blocks read as their looks, and only its quads on this side are kept;
	 * a plain model's quads are copied with their own texture coordinates, so
	 * random variants and rotations show as on the real block. Tints are the
	 * look's, and the material is the look's pass (or the Builder's Goggles'
	 * fade).
	 */
	private static void emitWhole(RenderContext context, QuadEmitter emitter, BakedModel model, List<BakedQuad> quads, BlockState look,
			BlockAndTintGetter blockView, BlockPos pos, Supplier<net.minecraft.util.RandomSource> randomSupplier, Direction direction,
			@org.jetbrains.annotations.Nullable RenderMaterial layer, boolean builder) {
		// Every baked model is a FabricBakedModel; a plain vanilla one is its adapter.
		FabricBakedModel fabric = (FabricBakedModel) model;
		if (!fabric.isVanillaAdapter()) {
			context.pushTransform(quad -> {
				Direction side = quad.cullFace() != null ? quad.cullFace() : quad.lightFace();
				if (side != direction) {
					return false;
				}
				finishWhole(quad, look, blockView, pos, layer, builder);
				return true;
			});
			try {
				fabric.emitBlockQuads(new LookView(blockView, direction), look, pos, randomSupplier, context);
			} finally {
				context.popTransform();
			}
		} else {
			for (BakedQuad quad : quads) {
				emitter.fromVanilla(quad, layer, direction);
				finishWhole(emitter, look, blockView, pos, layer, builder);
				emitter.emit();
			}
		}
	}

	private static void finishWhole(MutableQuadView quad, BlockState look, BlockAndTintGetter blockView, BlockPos pos,
			@org.jetbrains.annotations.Nullable RenderMaterial layer, boolean builder) {
		int tint = 0xFFFF_FFFF;
		if (quad.colorIndex() != -1) {
			tint = 0xFF00_0000 | Minecraft.getInstance().getBlockColors().getColor(look, blockView, pos, quad.colorIndex());
			quad.colorIndex(-1);
		}
		for (int i = 0; i < 4; i++) {
			int color = multiply(quad.spriteColor(i, 0), tint);
			if (builder) {
				// Faded, so the real block drawn below shows through.
				color = (color & 0x00FF_FFFF) | (BuilderView.DISGUISE_ALPHA << 24);
			}
			quad.spriteColor(i, 0, color);
		}
		if (builder) {
			quad.material(FabricMaterials.fade());
		} else if (layer != null) {
			quad.material(layer);
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

	private static void emitShape(QuadEmitter emitter, BlockState state, Direction direction) {
		DisguiseShapes.shape(state, direction, emitter::square);
	}

	/** Turns a face's texture by its disguise's rotation, as {@link DisguiseShapes#uvOrder} orders the corners. */
	private static void rotateAndFlip(QuadEmitter emitter, int rotation, Direction dir) {
		float[] u = new float[4];
		float[] v = new float[4];
		for (int i = 0; i < 4; i++) {
			u[i] = emitter.spriteU(i, 0);
			v[i] = emitter.spriteV(i, 0);
		}
		int[] order = DisguiseShapes.uvOrder(rotation, dir);
		for (int i = 0; i < 4; i++) {
			emitter.sprite(i, 0, u[order[i]], v[order[i]]);
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
		emitter.spriteBake(0, GogglesView.overlaySprite(), 4);
		emitter.colorIndex(-1);
		emitter.spriteColor(0, 0xFFFF_FFFF, 0xFFFF_FFFF, 0xFFFF_FFFF, 0xFFFF_FFFF);
		emitter.material(FabricMaterials.overlay());
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
		emitter.spriteBake(0, BuilderView.outlineSprite(), MutableQuadView.BAKE_LOCK_UV);
		emitter.colorIndex(-1);
		emitter.spriteColor(0, OUTLINE_COLOR, OUTLINE_COLOR, OUTLINE_COLOR, OUTLINE_COLOR);
		emitter.material(FabricMaterials.outline());
		emitter.emit();
	}




}
