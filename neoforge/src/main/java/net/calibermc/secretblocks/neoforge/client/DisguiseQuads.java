package net.calibermc.secretblocks.neoforge.client;

import net.calibermc.secretblocks.blocks.DisguiseLook;
import net.calibermc.secretblocks.blocks.DoorBlock;
import net.calibermc.secretblocks.blocks.TrapdoorBlock;
import net.calibermc.secretblocks.blocks.entity.Disguise;
import net.calibermc.secretblocks.render.BuilderView;
import net.calibermc.secretblocks.render.DisguiseShapes;
import net.calibermc.secretblocks.render.DoorHalves;
import net.calibermc.secretblocks.render.FaceQuads;
import net.calibermc.secretblocks.render.GogglesView;
import net.minecraft.client.Minecraft;
import net.calibermc.secretblocks.render.BlockTints;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A secret block's disguise as baked quads, sorted by cull face, each in its
 * own render layer, for the NeoForge model. The same faces as the Fabric renderer
 * draws: each face shows its look (the look's own quads where the face shows
 * it as it is, else the look's texture fitted to the face and turned by its
 * rotation), tinted and in its texture's layer; the Secret Goggles' overlay; and
 * in the Builder's Goggles' view the faded look, an outline and the real
 * block inside. Face shapes come from {@link DisguiseShapes}, corners and
 * texture coordinates from {@link FaceQuads} (as the Fabric Renderer API lays
 * them out), so both loaders draw the same faces.
 */
final class DisguiseQuads {

	/** How wide the builder's outline is, and how far it sits out from the face. */
	private static final float OUTLINE_WIDTH = 1.0f / 32.0f;
	private static final float OUTLINE_LIFT = 0.002f;
	/** Light blue, full-bright. */
	private static final int OUTLINE_COLOR = 0xFF4D_E6FF;
	private static final int WHITE = 0xFFFF_FFFF;
	/** Full block and sky light: drawn at full brightness. */
	/** No turn at all (the overlay, the outline and the ghost face): not even rotation 0's corner order. */
	private static final int UNTURNED = -1;

	/** Quads by cull face (index 6: none). */
	private final List<List<BakedQuad>> byFace = new ArrayList<>(7);
	private boolean translucent;

	private DisguiseQuads() {
		for (int i = 0; i < 7; i++) {
			byFace.add(new ArrayList<>());
		}
	}

	/** The quads on one cull face (or none). */
	List<BakedQuad> quads(@Nullable Direction side) {
		return byFace.get(side == null ? 6 : side.get3DDataValue());
	}

	/** Whether any quad is drawn in the translucent layer. */
	boolean translucent() {
		return translucent;
	}

	private void add(@Nullable Direction cullFace, BakedQuad quad) {
		translucent |= quad.materialInfo().layer().translucent();
		byFace.get(cullFace == null ? 6 : cullFace.get3DDataValue()).add(quad);
	}

	/** Builds a disguise's quads; {@code level} may be null (no tints from the world). */
	static DisguiseQuads build(Disguise disguise, @Nullable BlockAndTintGetter level, BlockState state, BlockPos pos, RandomSource random) {
		DisguiseQuads out = new DisguiseQuads();
		Minecraft minecraft = Minecraft.getInstance();
		boolean cutoutLeaves = minecraft.options.cutoutLeaves().get();
		boolean builder = BuilderView.isActive();
		boolean joinsAbove = builder && level != null && DoorHalves.joinsAbove(level, pos, state);
		boolean joinsBelow = builder && level != null && DoorHalves.joinsBelow(level, pos, state);
		CompoundTag tag = disguise.save(new CompoundTag()); // disguise keys only, not the inventory

		for (Direction direction : Direction.values()) {
			String key = direction.name().toLowerCase();
			BlockState look = Blocks.STONE.defaultBlockState();
			Direction lookSide = Direction.NORTH;
			int rotation = 0;
			if (tag.contains(key + "State")) {
				look = NbtUtils.readBlockState(BuiltInRegistries.BLOCK, tag.getCompoundOrEmpty(key + "State"));
			}
			if (tag.contains(key + "Direction")) {
				lookSide = Disguise.byName(tag.getStringOr(key + "Direction", ""));
			}
			if (tag.contains(key + "Rotation")) {
				rotation = tag.getIntOr(key + "Rotation", 0);
			}
			if (level != null && !DisguiseLook.showsItself(look, level, pos, lookSide)) {
				look = Blocks.STONE.defaultBlockState();
			}
			// Leaves solid where the graphics settings draw them so; otherwise each face in its texture's layer.
			ChunkSectionLayer forced = builder ? ChunkSectionLayer.TRANSLUCENT
					: ModelBlockRenderer.forceOpaque(cutoutLeaves, look) ? ChunkSectionLayer.SOLID : null;

			BlockStateModel lookModel = minecraft.getModelManager().getBlockStateModelSet().get(look);
			random.setSeed(look.getSeed(pos));
			List<BakedQuad> lookQuads = quadsOf(lookModel, lookSide, random);

			TextureAtlasSprite ghostSprite = BuilderView.ghostSprite();
			boolean ghost = builder && BuilderView.shaderPack() && ghostSprite != null && !lookQuads.isEmpty();
			if (ghost) {
				// Under a shader pack: the faded block as a translucent ghost face (see BuilderView#shaderPack).
				out.shape(state, direction, ghostSprite, UNTURNED, WHITE, ChunkSectionLayer.TRANSLUCENT, false);
			}

			// A face that shows its look as it is (the same side, unturned, on a
			// full face) is drawn by the look's own quads, as the look would be
			// drawn here: random variants and rotations apply.
			boolean whole = !ghost && rotation == 0 && lookSide == direction
					&& !(state.getBlock() instanceof DoorBlock) && !(state.getBlock() instanceof TrapdoorBlock);
			if (whole) {
				// With the level and position here, a connected-texture model (Fusion,
				// for one) works out its connections as for the look itself.
				List<BakedQuad> wholeQuads = lookQuads;
				if (level != null) {
					random.setSeed(look.getSeed(pos));
					List<BlockStateModelPart> parts = new ArrayList<>();
					lookModel.collectParts(level, pos, look, random, parts);
					wholeQuads = new ArrayList<>();
					for (BlockStateModelPart part : parts) {
						wholeQuads.addAll(part.getQuads(lookSide));
					}
				}
				for (BakedQuad quad : wholeQuads) {
					int tint = BlockTints.color(look, level, pos, quad.materialInfo().tintIndex());
					out.add(direction, recolored(quad, tint, builder, forced));
				}
			} else if (!ghost) {
				for (BakedQuad quad : lookQuads) {
					BakedQuad.MaterialInfo info = quad.materialInfo();
					int faceColor = BlockTints.color(look, level, pos, info.tintIndex());
					if (builder) {
						// Faded, so the real block drawn inside shows through.
						faceColor = (faceColor & 0x00FF_FFFF) | (BuilderView.DISGUISE_ALPHA << 24);
					}
					out.shape(state, direction, info.sprite(), rotation, faceColor, forced != null ? forced : info.layer(), false);
				}
			}

			TextureAtlasSprite overlay = GogglesView.overlaySprite();
			if (GogglesView.isWearing() && !lookQuads.isEmpty() && overlay != null) {
				out.gogglesOverlay(state, direction, overlay);
			}
			TextureAtlasSprite outline = BuilderView.outlineSprite();
			if (builder && outline != null) {
				out.outline(state, direction, outline, joinsAbove, joinsBelow);
			}
		}
		if (builder) {
			out.realBlock(state, level, pos, random);
		}
		return out;
	}

	/**
	 * Builder's view: the vanilla block this secret block stands in for, with
	 * its own properties, drawn solid inside the faded disguise. Shrunk very
	 * slightly so its full faces do not fight the disguise faces; tinted like
	 * the vanilla block.
	 */
	private void realBlock(BlockState state, @Nullable BlockAndTintGetter level, BlockPos pos, RandomSource random) {
		DisguiseShapes.realStateOf(state).ifPresent(vanilla -> {
			BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(vanilla);
			random.setSeed(vanilla.getSeed(pos));
			List<BlockStateModelPart> parts = new ArrayList<>();
			model.collectParts(random, parts);
			for (int index = 0; index <= 6; index++) {
				Direction face = index < 6 ? Direction.from3DDataValue(index) : null;
				for (BlockStateModelPart part : parts) {
					for (BakedQuad quad : part.getQuads(face)) {
						int tint = BlockTints.color(vanilla, level, pos, quad.materialInfo().tintIndex());
						// In the quad's own layer, as the Fabric renderer draws it.
						add(face, transformed(quad, true, tint, false, null,
								part.useAmbientOcclusion() && quad.materialInfo().ambientOcclusion()));
					}
				}
			}
		});
	}

	/** A model's quads on one side (or none), from every part it draws. */
	private static List<BakedQuad> quadsOf(BlockStateModel model, @Nullable Direction side, RandomSource random) {
		List<BakedQuad> quads = new ArrayList<>();
		List<BlockStateModelPart> parts = new ArrayList<>();
		model.collectParts(random, parts);
		for (BlockStateModelPart part : parts) {
			quads.addAll(part.getQuads(side));
		}
		return quads;
	}

	private static float shrink(float coordinate) {
		return 0.5f + (coordinate - 0.5f) * 0.996f;
	}

	/**
	 * Secret Goggles overlay: one translucent, full-bright copy of the face
	 * shape, textured with the animated rainbow stripes and pushed out 0.001 so
	 * it does not flicker against the disguise underneath.
	 */
	private void gogglesOverlay(BlockState state, Direction direction, TextureAtlasSprite sprite) {
		float dx = direction.getStepX() * 0.001f;
		float dy = direction.getStepY() * 0.001f;
		float dz = direction.getStepZ() * 0.001f;
		DisguiseShapes.shape(state, direction, (face, left, bottom, right, top, depth) -> {
			Square square = Square.of(face, left, bottom, right, top, depth);
			square.offset(dx, dy, dz);
			add(square.cullFace, square.bake(sprite, UNTURNED, WHITE, ChunkSectionLayer.TRANSLUCENT, true));
		});
	}

	/**
	 * Builder's view outline: a thin, bright frame around each disguise face,
	 * out past the Secret Goggles' overlay. Faces the block shares with the
	 * other half of its door get no frame on that edge (and the faces between
	 * the halves none at all), so a door is one panel.
	 */
	private void outline(BlockState state, Direction direction, TextureAtlasSprite sprite, boolean joinsAbove, boolean joinsBelow) {
		if ((direction == Direction.UP && joinsAbove) || (direction == Direction.DOWN && joinsBelow)) {
			return;
		}
		boolean side = direction.getAxis().isHorizontal();
		DisguiseShapes.shape(state, direction, (face, left, bottom, right, top, depth) -> {
			float w = OUTLINE_WIDTH;
			if (!(side && joinsBelow)) {
				outlineStrip(face, left, bottom, right, Math.min(top, bottom + w), depth, sprite);
			}
			if (!(side && joinsAbove)) {
				outlineStrip(face, left, Math.max(bottom, top - w), right, top, depth, sprite);
			}
			outlineStrip(face, left, bottom, Math.min(right, left + w), top, depth, sprite);
			outlineStrip(face, Math.max(left, right - w), bottom, right, top, depth, sprite);
		});
	}

	private void outlineStrip(Direction face, float left, float bottom, float right, float top, float depth, TextureAtlasSprite sprite) {
		Square square = Square.of(face, left, bottom, right, top, depth - OUTLINE_LIFT);
		if (Math.abs(depth) < FaceQuads.CULL_EPSILON) {
			// Lifted off the face, but hidden with it where a neighbour covers it.
			square.cullFace = face;
		}
		add(square.cullFace, square.bake(sprite, UNTURNED, OUTLINE_COLOR, ChunkSectionLayer.SOLID, true));
	}

	/** One disguise face's shape, textured with {@code sprite} fitted to it and turned by {@code rotation}. */
	private void shape(BlockState state, Direction direction, TextureAtlasSprite sprite, int rotation, int color, ChunkSectionLayer layer, boolean fullBright) {
		DisguiseShapes.shape(state, direction, (face, left, bottom, right, top, depth) -> {
			Square square = Square.of(face, left, bottom, right, top, depth);
			add(square.cullFace, square.bake(sprite, rotation, color, layer, fullBright));
		});
	}

	/** A look's own quad, tinted and (in the Builder's view) faded, its tint baked in; in {@code layer} if not null. */
	private static BakedQuad recolored(BakedQuad quad, int tint, boolean faded, @Nullable ChunkSectionLayer layer) {
		return transformed(quad, false, tint, faded, layer, quad.materialInfo().ambientOcclusion());
	}

	/**
	 * {@code quad} with its colours multiplied by {@code tint} (an opaque ARGB
	 * colour), faded to the Builder's Goggles' alpha if {@code faded}, and shrunk
	 * very slightly towards the block's centre if {@code shrink}, and moved to
	 * {@code layer} if not null.
	 */
	private static BakedQuad transformed(BakedQuad quad, boolean shrink, int tint, boolean faded, @Nullable ChunkSectionLayer layer,
			boolean ambientOcclusion) {
		Vector3fc[] positions = new Vector3fc[4];
		int[] colors = new int[4];
		for (int i = 0; i < 4; i++) {
			Vector3fc position = quad.position(i);
			positions[i] = shrink ? new Vector3f(shrink(position.x()), shrink(position.y()), shrink(position.z())) : position;
			int color = FaceQuads.multiply(quad.bakedColors().color(i), tint);
			colors[i] = faded ? (color & 0x00FF_FFFF) | (BuilderView.DISGUISE_ALPHA << 24) : color;
		}
		BakedQuad.MaterialInfo info = quad.materialInfo();
		ChunkSectionLayer drawn = layer != null ? layer : info.layer();
		return new BakedQuad(positions[0], positions[1], positions[2], positions[3],
				quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(),
				material(info.sprite(), drawn, info.shadeDirectionOverride(), info.lightEmission(), ambientOcclusion),
				quad.bakedNormals(), BakedColors.of(colors[0], colors[1], colors[2], colors[3]));
	}

	/**
	 * A quad's material: untinted (tints are baked into the colours), drawn in {@code layer}, shaded as the
	 * face {@code shadeAs} (as its own face for null; {@link Direction#UP}, the fully lit face, for none).
	 */
	private static BakedQuad.MaterialInfo material(TextureAtlasSprite sprite, ChunkSectionLayer layer, @Nullable Direction shadeAs,
			int lightEmission, boolean ambientOcclusion) {
		boolean translucent = layer.translucent();
		return new BakedQuad.MaterialInfo(sprite, layer,
				translucent ? Sheets.translucentBlockItemSheet() : Sheets.cutoutBlockItemSheet(),
				translucent ? Sheets.translucentBlockItemGlintSheet() : Sheets.cutoutBlockItemGlintSheet(),
				translucent ? Sheets.translucentBlockItemGlintSpecialSheet() : Sheets.cutoutBlockItemGlintSpecialSheet(),
				-1, shadeAs, lightEmission, ambientOcclusion);
	}

	/** A face rectangle ({@link FaceQuads#square}) baked into a quad with a sprite fitted to it. */
	private static final class Square {

		final Direction face;
		final float[][] corners;
		@Nullable
		Direction cullFace;

		private Square(Direction face, FaceQuads.Corners corners) {
			this.face = face;
			this.corners = corners.positions();
			this.cullFace = corners.cullFace();
		}

		static Square of(Direction face, float left, float bottom, float right, float top, float depth) {
			return new Square(face, FaceQuads.square(face, left, bottom, right, top, depth));
		}

		void offset(float dx, float dy, float dz) {
			for (float[] corner : corners) {
				corner[0] += dx;
				corner[1] += dy;
				corner[2] += dz;
			}
		}

		BakedQuad bake(TextureAtlasSprite sprite, int rotation, int color, ChunkSectionLayer layer, boolean fullBright) {
			float[][] uv = new float[4][];
			for (int i = 0; i < 4; i++) {
				uv[i] = FaceQuads.lockedUv(face, corners[i]);
			}
			int[] order = DisguiseShapes.uvOrder(rotation, face);
			Vector3fc[] positions = new Vector3fc[4];
			long[] packedUv = new long[4];
			for (int i = 0; i < 4; i++) {
				positions[i] = new Vector3f(corners[i][0], corners[i][1], corners[i][2]);
				float[] texture = uv[order[i]];
				packedUv[i] = UVPair.pack(sprite.getU0() + texture[0] * (sprite.getU1() - sprite.getU0()),
						sprite.getV0() + texture[1] * (sprite.getV1() - sprite.getV0()));
			}
			// Full-bright faces give off full light and skip shading and ambient occlusion, as the Fabric materials do.
			return new BakedQuad(positions[0], positions[1], positions[2], positions[3], packedUv[0], packedUv[1], packedUv[2], packedUv[3],
					face, material(sprite, layer, fullBright ? Direction.UP : null, fullBright ? 15 : 0, !fullBright), BakedNormals.UNSPECIFIED, BakedColors.of(color));
		}
	}
}
