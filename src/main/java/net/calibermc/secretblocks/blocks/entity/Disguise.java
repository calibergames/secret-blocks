package net.calibermc.secretblocks.blocks.entity;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * The disguise of one secret block: for each of its six faces, the block
 * state it shows, which face of that state is drawn, and a rotation in
 * degrees. Shared by every block entity that carries a disguise
 * ({@link DisguisedBlockEntity}), so blocks that need a vanilla block entity
 * of their own (the dispenser) keep the same data.
 *
 * NBT uses the keys the mod has always written, so existing worlds keep
 * their disguises: {@code <face>State}, {@code <face>Direction},
 * {@code <face>Rotation} for each face, plus {@code age}.
 */
public final class Disguise {

	private static final Direction[] FACES = {
			Direction.UP, Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

	private final Map<Direction, BlockState> states = new EnumMap<>(Direction.class);
	private final Map<Direction, Direction> directions = new EnumMap<>(Direction.class);
	private final Map<Direction, Integer> rotations = new EnumMap<>(Direction.class);
	private int age = 0;

	public Disguise() {
		for (Direction face : FACES) {
			states.put(face, Blocks.AIR.defaultBlockState());
			directions.put(face, face);
			rotations.put(face, 0);
		}
	}

	public BlockState getState(Direction face) {
		return states.get(face);
	}

	public void setState(Direction face, BlockState state) {
		states.put(face, state);
	}

	public Direction getDirection(Direction face) {
		return directions.get(face);
	}

	public void setDirection(Direction face, Direction direction) {
		directions.put(face, direction);
	}

	public int getRotation(Direction face) {
		return rotations.get(face);
	}

	public void setRotation(Direction face, int rotation) {
		rotations.put(face, rotation);
	}

	public void tickAge() {
		age++;
	}

	/** Write the disguise into {@code tag} and return it. */
	public CompoundTag save(CompoundTag tag) {
		tag.putInt("age", age);
		for (Direction face : FACES) {
			tag.put(face.getName() + "State", NbtUtils.writeBlockState(states.get(face)));
		}
		for (Direction face : FACES) {
			tag.putString(face.getName() + "Direction", directions.get(face).getName());
		}
		for (Direction face : FACES) {
			tag.putInt(face.getName() + "Rotation", rotations.get(face));
		}
		return tag;
	}

	/** Read the disguise from {@code tag}; keys that are missing leave that value unchanged. */
	public void load(CompoundTag tag) {
		if (tag.contains("age")) {
			age = tag.getInt("age");
		}
		for (Direction face : FACES) {
			String name = face.getName();
			if (tag.contains(name + "State")) {
				states.put(face, NbtUtils.readBlockState(tag.getCompound(name + "State")));
			}
			if (tag.contains(name + "Direction")) {
				directions.put(face, byName(tag.getString(name + "Direction")));
			}
			if (tag.contains(name + "Rotation")) {
				rotations.put(face, tag.getInt(name + "Rotation"));
			}
		}
	}

	public static Direction byName(String name) {
		return Direction.byName(name.toLowerCase(Locale.ROOT));
	}
}
