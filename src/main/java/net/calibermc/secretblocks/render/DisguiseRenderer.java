package net.calibermc.secretblocks.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.calibermc.secretblocks.blocks.DoorBlock;
import net.calibermc.secretblocks.blocks.TrapdoorBlock;
import net.calibermc.secretblocks.registry.Counterparts;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.Half;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.calibermc.secretblocks.blocks.entity.Disguise;

/**
 * Client-only disguise rendering: turns a secret block's per-face disguise
 * (state, drawn side, rotation) into quads with the Fabric Renderer API.
 * Kept out of {@code SecretBlockEntity}, so the block entity holds only data
 * and loads on a dedicated server.
 */
@Environment(EnvType.CLIENT)
public final class DisguiseRenderer {

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
				tempState = NbtUtils.readBlockState(tag.getCompound(direction.name().toLowerCase() + "State"));
			}

			if (tag.contains(direction.name().toLowerCase() + "Direction")) {
				tempDirection = Disguise.byName(tag.getString(direction.name().toLowerCase() + "Direction"));
			}

			if (tag.contains(direction.name().toLowerCase() + "Rotation")) {
				tempRotation = tag.getInt(direction.name().toLowerCase() + "Rotation");
			}

			if (!tempState.isFaceSturdy(blockView, pos, tempDirection)) {
				tempState = Blocks.STONE.defaultBlockState();
			}

			BakedModel fullModel = minecraft.getBlockRenderer().getBlockModel(tempState);
			List<BakedQuad> abqList = fullModel.getQuads(tempState, tempDirection,  randomSupplier.get());

			BlockColors colors = minecraft.getBlockColors();
			int color = 0xFF00_0000 | colors.getColor(tempState, blockView, pos, 0xFF);

			for (BakedQuad quad : abqList) {

				TextureAtlasSprite quadSprite = quad.getSprite();

				emitShape(emitter, state, direction);

				emitter.spriteBake(0, quadSprite, 4);

				rotateAndFlip(emitter, tempRotation, direction);

				int faceColor = quad.isTinted() ? color : 0xFFFF_FFFF;
				if (builder) {
					// Faded, so the real block drawn below shows through.
					faceColor = (faceColor & 0x00FF_FFFF) | (BuilderView.DISGUISE_ALPHA << 24);
					emitter.material(BuilderView.fadeMaterial());
				}
				emitter.colorIndex(quad.isTinted() ? 0 : -1);
				emitter.spriteColor(0, faceColor, faceColor, faceColor, faceColor);
				emitter.emit();
			}

			if (GogglesView.isWearing() && !abqList.isEmpty()
					&& GogglesView.overlaySprite() != null && GogglesView.overlayMaterial() != null) {
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
		realStateOf(state).ifPresent(vanilla -> {
			BlockColors colors = Minecraft.getInstance().getBlockColors();
			BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(vanilla);
			QuadEmitter emitter = context.getEmitter();
			RenderMaterial material = BuilderView.realMaterial(model.useAmbientOcclusion());
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

	/**
	 * The vanilla block drawn inside: the counterpart, and for the Secret
	 * Piston's head (which has none: the wand never converts it) vanilla's
	 * piston head, so an extended piston shows its head.
	 */
	static Optional<BlockState> realStateOf(BlockState state) {
		if (state.is(ModBlocks.SECRET_PISTON_HEAD)) {
			return Optional.of(Counterparts.copyShared(state, Blocks.PISTON_HEAD.defaultBlockState()));
		}
		return Counterparts.vanillaStateOf(state);
	}

	private static float shrink(float coordinate) {
		return 0.5f + (coordinate - 0.5f) * 0.996f;
	}

	/** One face rectangle, as {@link QuadEmitter#square} takes it. */
	@FunctionalInterface
	private interface Square {
		void at(Direction face, float left, float bottom, float right, float top, float depth);
	}

	/** The shape of one disguise face: a full square, or a door or trapdoor panel face. */
	private static void emitShape(QuadEmitter emitter, BlockState state, Direction direction) {
		shape(state, direction, emitter::square);
	}

	private static void shape(BlockState state, Direction direction, Square square) {
		if (state.getBlock() instanceof DoorBlock) {
			renderDoor(square, state, direction);
		} else if (state.getBlock() instanceof TrapdoorBlock) {
			renderTrapdoor(square, state, direction);
		} else {
			square.at(direction, 0.0f, 0.0f, 1.0f, 1.0f, 0);
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
		emitter.material(GogglesView.overlayMaterial());
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
		shape(state, direction, (face, left, bottom, right, top, depth) -> {
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
		emitter.material(BuilderView.outlineMaterial());
		emitter.emit();
	}

	private static void renderDoorCuboid(Square square, Direction faceDirection, Direction cuboidDirection) {
		switch (cuboidDirection) {

		case NORTH:

			if (faceDirection == Direction.NORTH) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0f);
			} else if (faceDirection == Direction.SOUTH) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0.8125f);
			} else if (faceDirection == Direction.EAST) {
				square.at(faceDirection, 0.8125f, 0.0f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.WEST) {
				square.at(faceDirection, 0.0f, 0.0f, 0.1875f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.UP) {
				square.at(faceDirection, 0.0f, 0.8125f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.DOWN) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 0.1875f, 0.0f);
			}
			break;

		case EAST:

			if (faceDirection == Direction.SOUTH) {
				square.at(faceDirection, 0.8125f, 0.0f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.NORTH) {
				square.at(faceDirection, 0.0f, 0.0f, 0.1875f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.EAST) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0f);
			} else if (faceDirection == Direction.WEST) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0.8125f);
			} else if (faceDirection == Direction.UP) {
				square.at(faceDirection, 0.8125f, 0.0f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.DOWN) {
				square.at(faceDirection, 0.8125f, 0.0f, 1.0f, 1.0f, 0.0f);
			}
			break;

		case SOUTH:

			if (faceDirection == Direction.SOUTH) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0f);
			} else if (faceDirection == Direction.NORTH) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0.8125f);
			} else if (faceDirection == Direction.WEST) {
				square.at(faceDirection, 0.8125f, 0.0f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.EAST) {
				square.at(faceDirection, 0.0f, 0.0f, 0.1875f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.DOWN) {
				square.at(faceDirection, 0.0f, 0.8125f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.UP) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 0.1875f, 0.0f);
			}
			break;

		case WEST:

			if (faceDirection == Direction.NORTH) {
				square.at(faceDirection, 0.8125f, 0.0f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.SOUTH) {
				square.at(faceDirection, 0.0f, 0.0f, 0.1875f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.WEST) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0f);
			} else if (faceDirection == Direction.EAST) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0.8125f);
			} else if (faceDirection == Direction.UP) {
				square.at(faceDirection, 0.0f, 0.0f, 0.1875f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.DOWN) {
				square.at(faceDirection, 0.0f, 0.0f, 0.1875f, 1.0f, 0.0f);
			}
			break;

		case UP:

			if (faceDirection == Direction.SOUTH) {
				square.at(faceDirection, 0.0f, 0.8125f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.NORTH) {
				square.at(faceDirection, 0.0f, 0.8125f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.WEST) {
				square.at(faceDirection, 0.0f, 0.8125f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.EAST) {
				square.at(faceDirection, 0.0f, 0.8125f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.UP) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.DOWN) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0.8125f);
			}
			break;

		case DOWN:

			if (faceDirection == Direction.SOUTH) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 0.1875f, 0.0f);
			} else if (faceDirection == Direction.NORTH) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 0.1875f, 0.0f);
			} else if (faceDirection == Direction.WEST) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 0.1875f, 0.0f);
			} else if (faceDirection == Direction.EAST) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 0.1875f, 0.0f);
			} else if (faceDirection == Direction.DOWN) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.UP) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0.8125f);
			}
			break;
		}
	}

	private static void renderDoor(Square square, BlockState state, Direction direction) {

		Direction renderDirection = Direction.EAST;

		if ((state.getValue(DoorBlock.FACING) == Direction.NORTH && !state.getValue(DoorBlock.OPEN)) || (state.getValue(DoorBlock.FACING) == Direction.EAST && state.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT && state.getValue(DoorBlock.OPEN)) || (state.getValue(DoorBlock.FACING) == Direction.WEST && state.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT && state.getValue(DoorBlock.OPEN))) {
			renderDirection = Direction.SOUTH;
		} else if ((state.getValue(DoorBlock.FACING) == Direction.EAST && !state.getValue(DoorBlock.OPEN)) || (state.getValue(DoorBlock.FACING) == Direction.SOUTH && state.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT && state.getValue(DoorBlock.OPEN)) || (state.getValue(DoorBlock.FACING) == Direction.NORTH && state.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT && state.getValue(DoorBlock.OPEN))) {
			renderDirection = Direction.WEST;
		} else if ((state.getValue(DoorBlock.FACING) == Direction.SOUTH && !state.getValue(DoorBlock.OPEN)) || (state.getValue(DoorBlock.FACING) == Direction.WEST && state.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT && state.getValue(DoorBlock.OPEN)) || (state.getValue(DoorBlock.FACING) == Direction.EAST && state.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT && state.getValue(DoorBlock.OPEN))) {
			renderDirection = Direction.NORTH;
		} else if ((state.getValue(DoorBlock.FACING) == Direction.WEST && !state.getValue(DoorBlock.OPEN)) || (state.getValue(DoorBlock.FACING) == Direction.NORTH && state.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT && state.getValue(DoorBlock.OPEN)) || (state.getValue(DoorBlock.FACING) == Direction.SOUTH && state.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT && state.getValue(DoorBlock.OPEN))) {
			renderDirection = Direction.EAST;
		} else {
			renderDirection = Direction.NORTH;
		}
		renderDoorCuboid(square, direction, renderDirection);
	}

	private static void renderTrapdoor(Square square, BlockState state, Direction direction) {
		if (state.getValue(TrapdoorBlock.OPEN)) {
			renderDoorCuboid(square, direction, state.getValue(TrapdoorBlock.FACING).getOpposite());
		} else {
			if (state.getValue(TrapdoorBlock.HALF) == Half.TOP) {
				renderDoorCuboid(square, direction, Direction.UP);
			} else if (state.getValue(TrapdoorBlock.HALF) == Half.BOTTOM) {
				renderDoorCuboid(square, direction, Direction.DOWN);
			}
		}
	}

	private static void rotateAndFlip(QuadEmitter emitter, int rotation, Direction dir) {

		float u0 = emitter.spriteU(0, 0);
		float v0 = emitter.spriteV(0, 0);

		float u1 = emitter.spriteU(1, 0);
		float v1 = emitter.spriteV(1, 0);

		float u2 = emitter.spriteU(2, 0);
		float v2 = emitter.spriteV(2, 0);

		float u3 = emitter.spriteU(3, 0);
		float v3 = emitter.spriteV(3, 0);

		switch (rotation) {
		case 0:
			if (dir.equals(Direction.UP)) {
				emitter.sprite(0, 0, u3, v3);
				emitter.sprite(1, 0, u2, v2);
				emitter.sprite(2, 0, u1, v1);
				emitter.sprite(3, 0, u0, v0);
			} else {
				emitter.sprite(0, 0, u0, v0);
				emitter.sprite(1, 0, u1, v1);
				emitter.sprite(2, 0, u2, v2);
				emitter.sprite(3, 0, u3, v3);
			}
			break;
		case 90:
			if (dir.equals(Direction.UP)) {
				emitter.sprite(0, 0, u0, v0);
				emitter.sprite(1, 0, u3, v3);
				emitter.sprite(2, 0, u2, v2);
				emitter.sprite(3, 0, u1, v1);
			} else {
				emitter.sprite(0, 0, u1, v1);
				emitter.sprite(1, 0, u2, v2);
				emitter.sprite(2, 0, u3, v3);
				emitter.sprite(3, 0, u0, v0);
			}
			break;
		case 180:
			if (dir.equals(Direction.UP)) {
				emitter.sprite(0, 0, u1, v1);
				emitter.sprite(1, 0, u0, v0);
				emitter.sprite(2, 0, u3, v3);
				emitter.sprite(3, 0, u2, v2);
			} else {
				emitter.sprite(0, 0, u2, v2);
				emitter.sprite(1, 0, u3, v3);
				emitter.sprite(2, 0, u0, v0);
				emitter.sprite(3, 0, u1, v1);
			}

			break;
		case 270:
			if (dir.equals(Direction.UP)) {
				emitter.sprite(0, 0, u2, v2);
				emitter.sprite(1, 0, u1, v1);
				emitter.sprite(2, 0, u0, v0);
				emitter.sprite(3, 0, u3, v3);
			} else {
				emitter.sprite(0, 0, u3, v3);
				emitter.sprite(1, 0, u0, v0);
				emitter.sprite(2, 0, u1, v1);
				emitter.sprite(3, 0, u2, v2);
			}

			break;
		}
	}
}
