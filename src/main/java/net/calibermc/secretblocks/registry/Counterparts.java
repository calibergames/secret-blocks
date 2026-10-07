package net.calibermc.secretblocks.registry;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Which vanilla block each secret block stands in for. The Builder's Goggles
 * draw a secret block as its counterpart, and the Camouflage Wand converts
 * between the two. Every secret block has a row here or is listed as having
 * no counterpart; a game test checks that nothing is missing.
 *
 * States convert by copying every property the two blocks share. The secret
 * classes mostly extend the vanilla ones, so they use the same property
 * objects (facing, half, hinge, open, powered, power, ...).
 */
public final class Counterparts {

	/**
	 * Secret block → the vanilla block it stands in for. Lossy rows are
	 * marked. When two secret blocks share a vanilla block (Secret Redstone
	 * Dust and Wire both stand in for redstone wire), the reverse
	 * lookup returns the one inserted first.
	 */
	private static final Map<Block, Block> VANILLA = new LinkedHashMap<>();

	/**
	 * Blocks that stand in for no vanilla block: the plain disguised blocks; the
	 * Secret Gate and its segment; the Secret Piston head (no item); and the
	 * Torch Lever, which
	 * is not disguised at all (the goggles and the wand leave it alone).
	 */
	private static final Set<Block> NONE = Set.of(
			ModBlocks.SOLID_BLOCK, ModBlocks.GHOST_BLOCK, ModBlocks.ONE_WAY_GLASS, ModBlocks.SECRET_CLAMBER,
			ModBlocks.TORCH_LEVER, ModBlocks.WALL_TORCH_LEVER, ModBlocks.SECRET_GATE, ModBlocks.SECRET_GATE_SEGMENT,
			ModBlocks.SECRET_PISTON_HEAD);

	/**
	 * Vanilla block families that convert to one secret block (the wand's
	 * direction). Checked after the exact pairs.
	 */
	private static final List<Map.Entry<TagKey<Block>, Block>> FAMILIES = List.of(
			Map.entry(BlockTags.STAIRS, ModBlocks.STAIR_BLOCK),
			Map.entry(BlockTags.SLABS, ModBlocks.SLAB_BLOCK),
			Map.entry(BlockTags.WOODEN_DOORS, ModBlocks.DOOR_BLOCK),
			Map.entry(BlockTags.WOODEN_TRAPDOORS, ModBlocks.TRAPDOOR_BLOCK),
			Map.entry(BlockTags.WOODEN_BUTTONS, ModBlocks.WOODEN_BUTTON),
			Map.entry(BlockTags.WOODEN_PRESSURE_PLATES, ModBlocks.PRESSURE_PLATE));

	static {
		// Lossy: the secret block does not know which wood (or stone) it was.
		VANILLA.put(ModBlocks.STAIR_BLOCK, Blocks.OAK_STAIRS);
		VANILLA.put(ModBlocks.SLAB_BLOCK, Blocks.OAK_SLAB);
		VANILLA.put(ModBlocks.DOOR_BLOCK, Blocks.OAK_DOOR);
		VANILLA.put(ModBlocks.TRAPDOOR_BLOCK, Blocks.OAK_TRAPDOOR);
		VANILLA.put(ModBlocks.WOODEN_BUTTON, Blocks.OAK_BUTTON);
		VANILLA.put(ModBlocks.PRESSURE_PLATE, Blocks.OAK_PRESSURE_PLATE);
		// Lossy: vanilla has no player-only plate.
		VANILLA.put(ModBlocks.PLAYER_PRESSURE_PLATE, Blocks.STONE_PRESSURE_PLATE);
		// Exact.
		VANILLA.put(ModBlocks.IRON_DOOR_BLOCK, Blocks.IRON_DOOR);
		VANILLA.put(ModBlocks.IRON_TRAPDOOR_BLOCK, Blocks.IRON_TRAPDOOR);
		VANILLA.put(ModBlocks.STONE_BUTTON, Blocks.STONE_BUTTON);
		VANILLA.put(ModBlocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE);
		VANILLA.put(ModBlocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE);
		VANILLA.put(ModBlocks.SECRET_LEVER, Blocks.LEVER);
		VANILLA.put(ModBlocks.SECRET_REDSTONE, Blocks.REDSTONE_WIRE);
		// Lossy: the Secret Chest has no facing, type or waterlogging, so it
		// shows as a north-facing single chest and converts to one.
		VANILLA.put(ModBlocks.SECRET_CHEST, Blocks.CHEST);
		VANILLA.put(ModBlocks.SECRET_TRAPPED_CHEST, Blocks.TRAPPED_CHEST);
		VANILLA.put(ModBlocks.SECRET_OBSERVER, Blocks.OBSERVER);
		VANILLA.put(ModBlocks.SECRET_DISPENSER, Blocks.DISPENSER);
		VANILLA.put(ModBlocks.SECRET_DROPPER, Blocks.DROPPER);
		VANILLA.put(ModBlocks.SECRET_SCULK_SENSOR, Blocks.SCULK_SENSOR);
		VANILLA.put(ModBlocks.SECRET_PISTON, Blocks.PISTON);
		VANILLA.put(ModBlocks.SECRET_STICKY_PISTON, Blocks.STICKY_PISTON);
		VANILLA.put(ModBlocks.SECRET_HOPPER, Blocks.HOPPER);
		VANILLA.put(ModBlocks.SECRET_REPEATER, Blocks.REPEATER);
		VANILLA.put(ModBlocks.SECRET_COMPARATOR, Blocks.COMPARATOR);
		VANILLA.put(ModBlocks.SECRET_DAYLIGHT_DETECTOR, Blocks.DAYLIGHT_DETECTOR);
		// Lossy: vanilla has no six-sided wire. Listed after the Dust, so
		// redstone wire converts back to the Dust.
		VANILLA.put(ModBlocks.SECRET_REDSTONE_WIRE, Blocks.REDSTONE_WIRE);
	}

	private Counterparts() {
	}

	/** True when {@code secret} is a block with no vanilla counterpart (a plain disguised block, or the Torch Lever). */
	public static boolean hasNone(Block secret) {
		return NONE.contains(secret);
	}

	/** Every secret block listed here, with or without a counterpart. */
	public static boolean isListed(Block secret) {
		return VANILLA.containsKey(secret) || NONE.contains(secret);
	}

	/** The vanilla block {@code secret} stands in for, if any. */
	public static Optional<Block> vanillaOf(Block secret) {
		return Optional.ofNullable(VANILLA.get(secret));
	}

	/** {@code secret} as its vanilla counterpart, with every shared property copied. */
	public static Optional<BlockState> vanillaStateOf(BlockState secret) {
		return vanillaOf(secret.getBlock()).map(vanilla -> copyShared(secret, vanilla.defaultBlockState()));
	}

	/** The secret block that stands in for {@code vanilla}: an exact pair first, then a family. */
	public static Optional<Block> secretOf(BlockState vanilla) {
		for (Map.Entry<Block, Block> pair : VANILLA.entrySet()) {
			if (vanilla.is(pair.getValue())) {
				return Optional.of(pair.getKey());
			}
		}
		for (Map.Entry<TagKey<Block>, Block> family : FAMILIES) {
			if (vanilla.is(family.getKey())) {
				return Optional.of(family.getValue());
			}
		}
		return Optional.empty();
	}

	/** {@code vanilla} as its secret counterpart, with every shared property copied. */
	public static Optional<BlockState> secretStateOf(BlockState vanilla) {
		return secretOf(vanilla).map(secret -> copyShared(vanilla, secret.defaultBlockState()));
	}

	/** {@code to} with every property it shares with {@code from} set to {@code from}'s value. */
	public static BlockState copyShared(BlockState from, BlockState to) {
		BlockState result = to;
		for (Property<?> property : to.getProperties()) {
			if (from.hasProperty(property)) {
				result = copy(from, result, property);
			}
		}
		return result;
	}

	private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> property) {
		return to.setValue(property, from.getValue(property));
	}
}
