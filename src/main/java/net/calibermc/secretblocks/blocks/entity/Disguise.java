package net.calibermc.secretblocks.blocks.entity;

import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

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
 *
 * A block the Camouflage Wand made from a vanilla block of another kind than
 * its usual counterpart (a spruce door, a stone brick stair) remembers that
 * block under {@value #ORIGINAL}, so turning it back restores it. That key is
 * kept on the server, like a container's contents: clients get
 * {@link #saveForClients}.
 */
public final class Disguise {

	private static final Direction[] FACES = {
			Direction.UP, Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

	private final Map<Direction, BlockState> states = new EnumMap<>(Direction.class);
	private final Map<Direction, Direction> directions = new EnumMap<>(Direction.class);
	private final Map<Direction, Integer> rotations = new EnumMap<>(Direction.class);
	private int age = 0;
	@Nullable
	private Block original;

	/** The vanilla block the Camouflage Wand made this secret block from, when it was not the usual counterpart. */
	public static final String ORIGINAL = "secretblocks:original";

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

	@Nullable
	public Block getOriginal() {
		return original;
	}

	public void setOriginal(@Nullable Block original) {
		this.original = original;
	}

	/** Write the disguise, and the remembered original block, into {@code tag} and return it. */
	public CompoundTag save(CompoundTag tag) {
		saveForClients(tag);
		if (original != null) {
			tag.putString(ORIGINAL, Registry.BLOCK.getKey(original).toString());
		}
		return tag;
	}

	/** What clients need to draw the block: the disguise without the remembered original block. */
	public CompoundTag saveForClients(CompoundTag tag) {
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
		if (tag.contains(ORIGINAL, Tag.TAG_STRING)) {
			ResourceLocation id = ResourceLocation.tryParse(tag.getString(ORIGINAL));
			original = id != null && Registry.BLOCK.containsKey(id) ? Registry.BLOCK.get(id) : null;
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

	/** Remove every disguise key (and the remembered original block) from {@code tag}: what a vanilla block entity is given. */
	public static void stripFrom(CompoundTag tag) {
		tag.remove("age");
		tag.remove(ORIGINAL);
		for (Direction face : FACES) {
			tag.remove(face.getName() + "State");
			tag.remove(face.getName() + "Direction");
			tag.remove(face.getName() + "Rotation");
		}
	}

	public static Direction byName(String name) {
		return Direction.byName(name.toLowerCase(Locale.ROOT));
	}
}
