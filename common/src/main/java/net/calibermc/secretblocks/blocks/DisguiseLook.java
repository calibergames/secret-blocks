package net.calibermc.secretblocks.blocks;

import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * What a secret block looks like from outside, for everything that should
 * give it away no more than its looks do: its sounds, its particles and how
 * long it takes to mine or blow up. Each face shows its disguise, or stone
 * (no disguise, or a disguise that is not a full block on that face), as the
 * renderer draws it.
 */
public final class DisguiseLook {

	private static final BlockState STONE = Blocks.STONE.defaultBlockState();

	private DisguiseLook() {
	}

	/**
	 * Whether a face shows its disguise rather than stone: a face is copied
	 * from full blocks only. A block whose face is sturdy (stone, glass) or
	 * whose shape is a full block (leaves, whose faces are not sturdy) shows
	 * itself; anything partial (a torch, a slab's side) shows stone.
	 */
	public static boolean showsItself(BlockState disguise, BlockGetter level, BlockPos pos, Direction face) {
		return disguise.isFaceSturdy(level, pos, face) || disguise.isCollisionShapeFullBlock(level, pos);
	}

	/** The block the secret block at {@code pos} shows on {@code face}: its disguise there, or stone. */
	public static BlockState shown(BlockGetter level, BlockPos pos, Direction face) {
		return level.getBlockEntity(pos) instanceof DisguisedBlockEntity secret ? shown(secret, level, pos, face) : STONE;
	}

	/** What {@code secret}, a secret block's block entity at {@code pos}, shows on {@code face}. */
	public static BlockState shown(DisguisedBlockEntity secret, BlockGetter level, BlockPos pos, Direction face) {
		BlockState state = secret.getState(face);
		if (state != null && showsItself(state, level, pos, secret.getDisguise().getDirection(face))) {
			return state;
		}
		return STONE;
	}

	/**
	 * {@code state} as it looks on {@code face}: the face's look for a secret
	 * block, {@code state} itself for any other block.
	 */
	public static BlockState looks(BlockGetter level, BlockPos pos, BlockState state, Direction face) {
		return state.getBlock() instanceof SecretBlock ? shown(level, pos, face) : state;
	}

	/**
	 * The face of the block at {@code pos} that {@code entity} deals with:
	 * the face a player is mining, else the face toward the entity, else
	 * the top.
	 */
	public static Direction faceFor(@Nullable Entity entity, BlockPos pos) {
		if (entity instanceof Player player) {
			Direction mining = ((MiningFace) player).secretblocks$getMiningFace();
			if (mining != null) {
				return mining;
			}
		}
		if (entity != null) {
			return toward(pos, entity.getEyePosition());
		}
		return Direction.UP;
	}

	/** How far behind a face's plane the server may see the eyes of a player hitting it (they move between ticks). */
	private static final double FACE_TOLERANCE = 0.3;

	/**
	 * Whether {@code eye} is on the outer side of {@code face} of the block at
	 * {@code pos}, give or take {@link #FACE_TOLERANCE}: the only faces a
	 * player there can be hitting.
	 */
	public static boolean faces(BlockPos pos, Direction face, Vec3 eye) {
		double t = FACE_TOLERANCE;
		return switch (face) {
			case DOWN -> eye.y < pos.getY() + t;
			case UP -> eye.y > pos.getY() + 1 - t;
			case NORTH -> eye.z < pos.getZ() + t;
			case SOUTH -> eye.z > pos.getZ() + 1 - t;
			case WEST -> eye.x < pos.getX() + t;
			case EAST -> eye.x > pos.getX() + 1 - t;
		};
	}

	/** The face of the block at {@code pos} that faces {@code point}. */
	public static Direction toward(BlockPos pos, Vec3 point) {
		Vec3 d = point.subtract(Vec3.atCenterOf(pos));
		return Direction.getNearest(d.x, d.y, d.z);
	}

	/**
	 * A look as hard to break as its block, except that an unbreakable look
	 * (bedrock, barrier) counts as stone: no look makes a secret block
	 * unbreakable or blast-proof.
	 */
	public static BlockState breakable(BlockState look, BlockGetter level, BlockPos pos) {
		return look.getDestroySpeed(level, pos) < 0 ? STONE : look;
	}

	/**
	 * Vanilla's mining progress per tick, for the look of the face being
	 * mined: the right tool speeds it up, and a look that needs a tool for
	 * its drops is slower without one. (The secret block itself always drops,
	 * whatever the tool.)
	 */
	public static float destroyProgress(Player player, BlockGetter level, BlockPos pos) {
		BlockState look = breakable(shown(level, pos, faceFor(player, pos)), level, pos);
		float hardness = look.getDestroySpeed(level, pos);
		int divisor = player.hasCorrectToolForDrops(look) ? 30 : 100;
		return player.getDestroySpeed(look) / hardness / divisor;
	}

	/** The blast resistance of the face toward an explosion at {@code center}. */
	public static float explosionResistance(BlockGetter level, BlockPos pos, Vec3 center) {
		return breakable(shown(level, pos, toward(pos, center)), level, pos).getBlock().getExplosionResistance();
	}
}
