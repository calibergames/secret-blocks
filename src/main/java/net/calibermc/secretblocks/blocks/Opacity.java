package net.calibermc.secretblocks.blocks;

import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Whether a secret block blocks light, from its looks: it lets light through
 * (as glass does) when the look of every face does (glass, stained glass,
 * leaves, ice), and blocks it (as stone) otherwise. The answer is a block
 * state property, {@link #OPAQUE}, since light is worked out away from the
 * block entities; it is set from the disguise whenever the block entity
 * changes, and checked when a chunk loads (worlds saved before it existed).
 *
 * Carried by the secret blocks drawn as full blocks that would block light;
 * not by the doors and trapdoors (thin panels, as vanilla's), One Way Glass,
 * the Ghost Block or the Secret Clamber (which let light through anyway).
 */
public final class Opacity {

	/** True (the default) when the block blocks light like stone. */
	public static final BooleanProperty OPAQUE = BooleanProperty.create("opaque");

	private Opacity() {
	}

	/** Whether {@code block} carries {@link #OPAQUE}. */
	public static boolean carries(Block block) {
		return block instanceof SecretBlock && !(block instanceof DoorBlock) && !(block instanceof TrapdoorBlock)
				&& !(block instanceof OneWayGlassBlock) && !(block instanceof GhostBlock);
	}

	/**
	 * Whether {@code state} lets light through: it carries the property and its
	 * looks are see-through. Asked for every block in the game (light, shading),
	 * so the cheap test goes first.
	 */
	public static boolean seeThrough(BlockState state) {
		return state.getBlock() instanceof SecretBlock && state.hasProperty(OPAQUE) && !state.getValue(OPAQUE);
	}

	/** Whether the look of every face of {@code secret}, the block entity at {@code pos}, lets light through. */
	static boolean looksSeeThrough(DisguisedBlockEntity secret, BlockGetter level, BlockPos pos) {
		for (Direction face : Direction.values()) {
			BlockState look = DisguiseLook.shown(secret, level, pos, face);
			if (look.getLightBlock(level, pos) >= 15) {
				return false;
			}
		}
		return true;
	}

	/** Server: set the secret block at {@code pos} to block light or not, as its looks do. */
	public static void sync(Level level, BlockPos pos) {
		if (level.isClientSide()) {
			return;
		}
		BlockState state = level.getBlockState(pos);
		if (!state.hasProperty(OPAQUE) || !(level.getBlockEntity(pos) instanceof DisguisedBlockEntity secret)) {
			return;
		}
		boolean opaque = !looksSeeThrough(secret, level, pos);
		if (state.getValue(OPAQUE) != opaque) {
			level.setBlock(pos, state.setValue(OPAQUE, opaque), Block.UPDATE_ALL);
		}
	}

	/**
	 * Notes the secret blocks of a loading chunk whose state disagrees with
	 * their looks, to be put right at the end of the tick. Reads the chunk
	 * itself, over a copy of its block entities (reading one may drop a stale
	 * entry from the chunk's own map).
	 */
	public static void checkChunk(ServerLevel level, LevelChunk chunk) {
		for (BlockEntity entity : new ArrayList<>(chunk.getBlockEntities().values())) {
			if (entity instanceof DisguisedBlockEntity secret) {
				BlockState state = entity.getBlockState();
				if (state.hasProperty(OPAQUE) && state.getValue(OPAQUE) == looksSeeThrough(secret, chunk, entity.getBlockPos())) {
					PENDING.computeIfAbsent(level, l -> new HashSet<>()).add(entity.getBlockPos().immutable());
				}
			}
		}
	}

	/** Puts right the secret blocks {@link #checkChunk} noted in {@code level} (the end of each of its ticks). */
	public static void putRight(ServerLevel level) {
		Set<BlockPos> wrong = PENDING.remove(level);
		if (wrong != null) {
			for (BlockPos pos : wrong) {
				if (level.isLoaded(pos)) {
					sync(level, pos);
				}
			}
		}
	}

	/** Secret blocks found wrong as their chunks loaded, put right at the end of the next tick of their level. */
	private static final Map<ServerLevel, Set<BlockPos>> PENDING = new HashMap<>();

	/**
	 * Checks each secret block in a chunk as it loads (reading the chunk
	 * itself: asking the level for a chunk while it loads would wait on it
	 * forever), and puts the wrong ones right after the tick, when the chunk
	 * may change.
	 */
	public static void register() {
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
			checkChunk(level, chunk);
		});
		ServerTickEvents.END_WORLD_TICK.register(Opacity::putRight);
		ServerWorldEvents.UNLOAD.register((server, level) -> PENDING.remove(level));
	}
}
