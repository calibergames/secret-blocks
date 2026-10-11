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
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A secret block's disguise as baked quads, sorted by render layer and cull
 * face, for the NeoForge/Forge model. The same faces as the Fabric renderer
 * draws: each face shows its look (the look's own quads where the face shows
 * it as it is, else the look's texture fitted to the face and turned by its
 * rotation), tinted and in the look's layer; the Secret Goggles' overlay; and
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
	private static final int FULL_BRIGHT = 0x00F0_00F0;
	/** No turn at all (the overlay, the outline and the ghost face): not even rotation 0's corner order. */
	private static final int UNTURNED = -1;

	/** Quads by layer, then by cull face (index 6: none). */
	private final Map<RenderType, List<List<BakedQuad>>> byLayer = new LinkedHashMap<>();

	private DisguiseQuads() {
	}

	ChunkRenderTypeSet layers() {
		return ChunkRenderTypeSet.of(byLayer.keySet());
	}

	/** The quads for one layer (or all, for {@code null}) on one cull face (or none). */
	List<BakedQuad> quads(@Nullable RenderType layer, @Nullable Direction side) {
		int index = side == null ? 6 : side.get3DDataValue();
		if (layer != null) {
			List<List<BakedQuad>> lists = byLayer.get(layer);
			return lists == null ? List.of() : lists.get(index);
		}
		List<BakedQuad> all = new ArrayList<>();
		for (List<List<BakedQuad>> lists : byLayer.values()) {
			all.addAll(lists.get(index));
		}
		return all;
	}

	private void add(RenderType layer, @Nullable Direction cullFace, BakedQuad quad) {
		byLayer.computeIfAbsent(layer, key -> {
			List<List<BakedQuad>> lists = new ArrayList<>(7);
			for (int i = 0; i < 7; i++) {
				lists.add(new ArrayList<>());
			}
			return lists;
		}).get(cullFace == null ? 6 : cullFace.get3DDataValue()).add(quad);
	}

	/** Builds a disguise's quads; {@code level} may be null (no tints from the world). */
	static DisguiseQuads build(Disguise disguise, @Nullable BlockAndTintGetter level, BlockState state, BlockPos pos, RandomSource random) {
		DisguiseQuads out = new DisguiseQuads();
		Minecraft minecraft = Minecraft.getInstance();
		BlockColors colors = minecraft.getBlockColors();
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
				look = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound(key + "State"));
			}
			if (tag.contains(key + "Direction")) {
				lookSide = Disguise.byName(tag.getString(key + "Direction"));
			}
			if (tag.contains(key + "Rotation")) {
				rotation = tag.getInt(key + "Rotation");
			}
			if (level != null && !DisguiseLook.showsItself(look, level, pos, lookSide)) {
				look = Blocks.STONE.defaultBlockState();
			}
			// In the look's own layer: stained glass translucent, leaves as the graphics setting has them.
			RenderType layer = ItemBlockRenderTypes.getChunkRenderType(look);

			BakedModel lookModel = minecraft.getBlockRenderer().getBlockModel(look);
			random.setSeed(look.getSeed(pos));
			List<BakedQuad> lookQuads = lookModel.getQuads(look, lookSide, random);
			int color = 0xFF00_0000 | colors.getColor(look, level, pos, 0xFF);

			TextureAtlasSprite ghostSprite = BuilderView.ghostSprite();
			boolean ghost = builder && BuilderView.shaderPack() && ghostSprite != null && !lookQuads.isEmpty();
			if (ghost) {
				// Under a shader pack: the faded block as a translucent ghost face (see BuilderView#shaderPack).
				out.shape(state, direction, ghostSprite, UNTURNED, WHITE, RenderType.translucent(), false);
			}

			// A face that shows its look as it is (the same side, unturned, on a
			// full face) is drawn by the look's own quads, as the look would be
			// drawn here: random variants and rotations apply.
			boolean whole = !ghost && rotation == 0 && lookSide == direction
					&& !(state.getBlock() instanceof DoorBlock) && !(state.getBlock() instanceof TrapdoorBlock);
			if (whole) {
				for (BakedQuad quad : lookQuads) {
					int tint = quad.isTinted() ? 0xFF00_0000 | colors.getColor(look, level, pos, quad.getTintIndex()) : WHITE;
					out.add(builder ? RenderType.translucent() : layer, direction, recolored(quad, tint, builder));
				}
			} else if (!ghost) {
				for (BakedQuad quad : lookQuads) {
					int faceColor = quad.isTinted() ? color : WHITE;
					if (builder) {
						// Faded, so the real block drawn inside shows through.
						faceColor = (faceColor & 0x00FF_FFFF) | (BuilderView.DISGUISE_ALPHA << 24);
					}
					out.shape(state, direction, quad.getSprite(), rotation, faceColor, builder ? RenderType.translucent() : layer, false);
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
			BlockColors colors = Minecraft.getInstance().getBlockColors();
			BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(vanilla);
			for (int index = 0; index <= 6; index++) {
				Direction face = index < 6 ? Direction.from3DDataValue(index) : null;
				random.setSeed(vanilla.getSeed(pos));
				for (BakedQuad quad : model.getQuads(vanilla, face, random)) {
					int tint = quad.isTinted() ? 0xFF00_0000 | colors.getColor(vanilla, level, pos, quad.getTintIndex()) : WHITE;
					int[] vertices = quad.getVertices().clone();
					for (int i = 0; i < 4; i++) {
						int base = i * STRIDE;
						for (int axis = 0; axis < 3; axis++) {
							vertices[base + axis] = Float.floatToRawIntBits(shrink(Float.intBitsToFloat(vertices[base + axis])));
						}
						vertices[base + COLOR] = FaceQuads.swapRedBlue(FaceQuads.multiply(FaceQuads.swapRedBlue(vertices[base + COLOR]), tint));
					}
					BakedQuad shrunk = new BakedQuad(vertices, -1, quad.getDirection(), quad.getSprite(), quad.isShade(),
							model.useAmbientOcclusion() && quad.hasAmbientOcclusion());
					// In the secret block's own layer, as the Fabric renderer's standard material draws it.
					add(RenderType.cutout(), face, shrunk);
				}
			}
		});
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
			add(RenderType.translucent(), square.cullFace, square.bake(sprite, UNTURNED, WHITE, true));
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
		add(RenderType.solid(), square.cullFace, square.bake(sprite, UNTURNED, OUTLINE_COLOR, true));
	}

	/** One disguise face's shape, textured with {@code sprite} fitted to it and turned by {@code rotation}. */
	private void shape(BlockState state, Direction direction, TextureAtlasSprite sprite, int rotation, int color, RenderType layer, boolean fullBright) {
		DisguiseShapes.shape(state, direction, (face, left, bottom, right, top, depth) -> {
			Square square = Square.of(face, left, bottom, right, top, depth);
			add(layer, square.cullFace, square.bake(sprite, rotation, color, fullBright));
		});
	}

	/** A look's own quad, tinted and (in the Builder's view) faded, its tint baked in. */
	private static BakedQuad recolored(BakedQuad quad, int tint, boolean faded) {
		int[] vertices = quad.getVertices().clone();
		for (int i = 0; i < 4; i++) {
			int color = FaceQuads.multiply(FaceQuads.swapRedBlue(vertices[i * STRIDE + COLOR]), tint);
			if (faded) {
				color = (color & 0x00FF_FFFF) | (BuilderView.DISGUISE_ALPHA << 24);
			}
			vertices[i * STRIDE + COLOR] = FaceQuads.swapRedBlue(color);
		}
		return new BakedQuad(vertices, -1, quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion());
	}

	// The block vertex format: position (3 floats), colour (RGBA bytes), texture (2 floats),
	// light (2 shorts), normal (3 bytes and padding): 8 ints a vertex.
	private static final int STRIDE = 8;
	private static final int COLOR = 3;
	private static final int UV = 4;
	private static final int LIGHT = 6;
	private static final int NORMAL = 7;

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

		BakedQuad bake(TextureAtlasSprite sprite, int rotation, int color, boolean fullBright) {
			float[][] uv = new float[4][];
			for (int i = 0; i < 4; i++) {
				uv[i] = FaceQuads.lockedUv(face, corners[i]);
			}
			int[] order = DisguiseShapes.uvOrder(rotation, face);
			int normal = (((byte) (face.getStepX() * 127)) & 0xFF) | ((((byte) (face.getStepY() * 127)) & 0xFF) << 8)
					| ((((byte) (face.getStepZ() * 127)) & 0xFF) << 16);
			int[] vertices = new int[4 * STRIDE];
			for (int i = 0; i < 4; i++) {
				int base = i * STRIDE;
				vertices[base] = Float.floatToRawIntBits(corners[i][0]);
				vertices[base + 1] = Float.floatToRawIntBits(corners[i][1]);
				vertices[base + 2] = Float.floatToRawIntBits(corners[i][2]);
				vertices[base + COLOR] = FaceQuads.swapRedBlue(color);
				float[] texture = uv[order[i]];
				vertices[base + UV] = Float.floatToRawIntBits(sprite.getU0() + texture[0] * (sprite.getU1() - sprite.getU0()));
				vertices[base + UV + 1] = Float.floatToRawIntBits(sprite.getV0() + texture[1] * (sprite.getV1() - sprite.getV0()));
				vertices[base + LIGHT] = fullBright ? FULL_BRIGHT : 0;
				vertices[base + NORMAL] = normal;
			}
			// Full-bright faces also skip shading and ambient occlusion, as the Fabric materials do.
			return new BakedQuad(vertices, -1, face, sprite, !fullBright, !fullBright);
		}
	}
}
