package net.calibermc.secretblocks.blocks;

import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.calibermc.secretblocks.blocks.entity.SecretBlockEntity;
import net.calibermc.secretblocks.blocks.entity.SecretGateSegmentBlockEntity;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.IntFunction;

/**
 * Secret Gate (the original mod's design, our own code). A disguised block
 * that faces the side it was placed on. While open it builds a line of up to
 * {@link GateSegmentOwner#MAX_LENGTH} segments ({@link SecretGateSegment}) in
 * that direction, each drawn as the gate's disguise; closed, it removes them.
 *
 * A gate is open while it is powered, or after another gate opened it: a
 * line that meets a gate (at its end, or beside one of its segments) opens
 * that gate too, and closing closes it again unless it is powered itself.
 * Each hop waits one tick, and a gate that is already open (or closed) is
 * left alone, so gates that meet each other cannot loop.
 *
 * Building stops at the first position that is not loaded (it never loads a
 * chunk), not replaceable, another gate, or another gate's segment. Where an
 * entity, an unloaded chunk or another gate's segment is in the way it tries
 * again each second while open, as it does after one of its segments
 * was broken. Closing removes only its own segments.
 *
 * Known limits: a gate opened by two others closes when the first of them
 * closes (there is no count of openers); its own power keeps it open.
 * Closing walks over gaps, so it can close a gate past an entity that the
 * line never reached; and a retry that was pending when the gate closed
 * repeats the closing up to a second later.
 */
public class SecretGate extends Block implements EntityBlock, SecretBlock, GateSegmentOwner {

	public static final DirectionProperty FACING = DirectionalBlock.FACING;
	/** Its own redstone input. */
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	/** Its line is wanted: powered, or opened by another gate. */
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

	/**
	 * Ticks before a gate acts on being placed powered, or on being opened or
	 * closed by another gate (one hop per tick). Minecraft does not schedule a
	 * second tick for a block that has one pending, so a hop to a gate that is
	 * waiting to retry is acted on at the retry, up to a second later.
	 */
	public static final int HOP_DELAY = 1;
	/** Ticks before trying again where an entity was in the way, a chunk was not loaded or a segment was broken. */
	public static final int RETRY_DELAY = 20;

	/** What a position on the line holds, for building. */
	public enum Spot {
		/** Replaceable and unobstructed: a segment goes here. */
		FREE,
		/** Already one of this gate's segments. */
		OWN,
		/** Replaceable, but an entity is in the way. */
		OCCUPIED,
		/** Another Secret Gate. */
		GATE,
		/** Not loaded. */
		UNLOADED,
		/** Another gate's segment: it may go, so the gate tries again later. */
		FOREIGN,
		/** Anything else: a solid block, outside the world. */
		BLOCKED
	}

	/**
	 * The line as far as it can be built: {@code length} positions (from 1)
	 * that are or become segments, and what stopped it. A full line stops with
	 * {@link Spot#GATE} when a gate stands just past its end (it opens that gate,
	 * as the original did), else with {@code null}.
	 */
	public record Line(int length, Spot stop) {
	}

	public SecretGate(Properties settings) {
		super(settings);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(OPEN, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, POWERED, OPEN);
	}

	/** It faces the side it was placed on, so the line grows away from the block it was placed against. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getClickedFace());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SecretBlockEntity(pos, state);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public boolean skipRendering(BlockState state, BlockState stateFrom, Direction direction) {
		return stateFrom.getBlock() instanceof SecretBlock || super.skipRendering(state, stateFrom, direction);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	/**
	 * Placed powered: build on the next tick, once the placement has given it
	 * its disguise (the segments copy it). Placed unpowered it does nothing: a
	 * closed gate must not close a gate in front of it that another holds open.
	 */
	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
		if (!level.isClientSide() && !oldState.is(this) && level.hasNeighborSignal(pos)) {
			level.scheduleTick(pos, this, HOP_DELAY);
		}
	}

	/**
	 * Its own power is acted on at once (a scheduled tick could wait behind a
	 * pending retry, which Minecraft does not double). A gate still being
	 * placed, with no disguise yet, waits a tick, so its segments copy the
	 * disguise the placement is about to give it.
	 */
	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moved) {
		if (!level.isClientSide() && level.hasNeighborSignal(pos) != state.getValue(POWERED)) {
			if (level.getBlockEntity(pos) instanceof DisguisedBlockEntity gate && gate.getState(Direction.NORTH).isAir()) {
				level.scheduleTick(pos, this, HOP_DELAY);
			} else {
				update((ServerLevel) level, pos, state);
			}
		}
	}

	/** Opened or closed by another gate, placed powered, or a retry. */
	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		update(level, pos, state);
	}

	/** Take up its own power, then build its line if open or remove it if closed. */
	private void update(ServerLevel level, BlockPos pos, BlockState state) {
		boolean powered = level.hasNeighborSignal(pos);
		if (powered != state.getValue(POWERED)) {
			// Its own power decides: on opens it, off closes it even if another gate had opened it.
			state = state.setValue(POWERED, powered).setValue(OPEN, powered);
			level.setBlock(pos, state, Block.UPDATE_ALL);
		}
		if (state.getValue(OPEN)) {
			build(level, pos, state);
		} else {
			removeLine(level, pos, state, true);
		}
	}

	/** Broken or replaced: its segments go, and, if it was open, the gates it opened close. */
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		if (!level.isClientSide() && !state.is(newState.getBlock())) {
			removeLine(level, pos, state, state.getValue(OPEN));
		}
		super.onRemove(state, level, pos, newState, moved);
	}

	/**
	 * It keeps a segment while open and on its line, with nothing between them
	 * that stops the line: a solid block, a gate or another gate's segment (so a
	 * copy placed past an obstruction is not kept). A gap it will build again (a
	 * broken segment, an entity in the way) does not count, so breaking one
	 * segment leaves the rest standing.
	 */
	@Override
	public boolean keepsSegment(BlockState ownerState, Level level, BlockPos ownerPos, BlockPos segmentPos) {
		if (!ownerState.is(this) || !ownerState.getValue(OPEN)) {
			return false;
		}
		Direction facing = ownerState.getValue(FACING);
		for (int i = 1; i <= MAX_LENGTH; i++) {
			BlockPos pos = ownerPos.relative(facing, i);
			if (pos.equals(segmentPos)) {
				return true;
			}
			if (level.isOutsideBuildHeight(pos) || stopsTheLine(level, pos, ownerPos)) {
				return false;
			}
		}
		return false;
	}

	/** A loaded position that ends the line: a gate, another gate's segment, or a block that is not replaceable. */
	private static boolean stopsTheLine(Level level, BlockPos pos, BlockPos gatePos) {
		if (!level.isLoaded(pos)) {
			return false;
		}
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof SecretGate) {
			return true;
		}
		if (state.is(ModBlocks.SECRET_GATE_SEGMENT)) {
			return !isOwnSegment(level, pos, gatePos);
		}
		return !state.canBeReplaced();
	}

	/** A segment broken while open is built again a second later. */
	@Override
	public void segmentRemoved(BlockState ownerState, Level level, BlockPos ownerPos, BlockPos segmentPos) {
		if (ownerState.is(this) && ownerState.getValue(OPEN)) {
			level.scheduleTick(ownerPos, this, RETRY_DELAY);
		}
	}

	/** How far the line can be built, from what each position (1 to 9, and 10 for a gate past the end) holds. */
	public static Line plan(IntFunction<Spot> spotAt) {
		for (int i = 1; i <= MAX_LENGTH; i++) {
			Spot spot = spotAt.apply(i);
			if (spot != Spot.FREE && spot != Spot.OWN) {
				return new Line(i - 1, spot);
			}
		}
		return new Line(MAX_LENGTH, spotAt.apply(MAX_LENGTH + 1) == Spot.GATE ? Spot.GATE : null);
	}

	/** What {@code pos} holds for the gate at {@code gatePos}. */
	public static Spot spotAt(Level level, BlockPos pos, BlockPos gatePos) {
		if (level.isOutsideBuildHeight(pos)) {
			return Spot.BLOCKED;
		}
		if (!level.isLoaded(pos)) {
			return Spot.UNLOADED;
		}
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof SecretGate) {
			return Spot.GATE;
		}
		if (state.is(ModBlocks.SECRET_GATE_SEGMENT)) {
			return isOwnSegment(level, pos, gatePos) ? Spot.OWN : Spot.FOREIGN;
		}
		if (!state.canBeReplaced()) {
			return Spot.BLOCKED;
		}
		// The rule a player's block placement follows: no living entity, vehicle or the like inside.
		return level.isUnobstructed(ModBlocks.SECRET_GATE_SEGMENT.defaultBlockState(), pos, CollisionContext.empty()) ? Spot.FREE : Spot.OCCUPIED;
	}

	private static boolean isOwnSegment(Level level, BlockPos pos, BlockPos gatePos) {
		return level.getBlockEntity(pos) instanceof SecretGateSegmentBlockEntity segment && gatePos.equals(segment.getOwner());
	}

	/**
	 * Build what is missing of the line, open the gates it meets, and come back
	 * a second later if an entity, an unloaded chunk or another gate's segment
	 * was in the way.
	 */
	private void build(ServerLevel level, BlockPos gatePos, BlockState gateState) {
		Direction facing = gateState.getValue(FACING);
		Line line = plan(i -> spotAt(level, gatePos.relative(facing, i), gatePos));
		CompoundTag disguise = level.getBlockEntity(gatePos) instanceof DisguisedBlockEntity gate
				? gate.getDisguise().save(new CompoundTag()) : null;
		boolean placed = false;
		for (int i = 1; i <= line.length(); i++) {
			BlockPos pos = gatePos.relative(facing, i);
			if (!isOwnSegment(level, pos, gatePos)) {
				placed = true;
				level.setBlock(pos, ModBlocks.SECRET_GATE_SEGMENT.defaultBlockState(), Block.UPDATE_ALL);
				if (level.getBlockEntity(pos) instanceof SecretGateSegmentBlockEntity segment) {
					// In the same tick as the block, before the segment's first check.
					segment.setOwner(gatePos);
					if (disguise != null) {
						segment.getDisguise().load(disguise);
						segment.markDisguiseChanged();
					}
				}
				// Only where a segment is new: a retry must not reopen a gate that closed by its own power.
				setBeside(level, pos, facing, true);
			}
		}
		if (line.stop() == Spot.GATE && (placed || line.length() == 0)) {
			setGate(level, gatePos.relative(facing, line.length() + 1), true);
		} else if (line.stop() == Spot.OCCUPIED || line.stop() == Spot.UNLOADED || line.stop() == Spot.FOREIGN) {
			level.scheduleTick(gatePos, this, RETRY_DELAY);
		}
	}

	/**
	 * Remove its own segments along the line and (if {@code chain}) close the
	 * gates the line meets: beside it, at its end, and just past a full line. It
	 * walks over gaps (a broken segment, an entity in the way), so a gap cannot
	 * leave gates further on open, and stops where the line would stop: a gate,
	 * another gate's segment, a block that is not replaceable, a position not
	 * loaded.
	 */
	private void removeLine(Level level, BlockPos gatePos, BlockState gateState, boolean chain) {
		Direction facing = gateState.getValue(FACING);
		for (int i = 1; i <= MAX_LENGTH + 1; i++) {
			BlockPos pos = gatePos.relative(facing, i);
			if (level.isOutsideBuildHeight(pos) || !level.isLoaded(pos)) {
				return;
			}
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof SecretGate) {
				if (chain) {
					setGate(level, pos, false);
				}
				return;
			}
			if (i > MAX_LENGTH) {
				return;
			}
			if (isOwnSegment(level, pos, gatePos)) {
				level.removeBlock(pos, false);
			} else if (state.is(ModBlocks.SECRET_GATE_SEGMENT) || !state.canBeReplaced()) {
				return;
			}
			if (chain) {
				setBeside(level, pos, facing, false);
			}
		}
	}

	/** Open or close the gates beside {@code pos}, across the line. */
	private static void setBeside(Level level, BlockPos pos, Direction facing, boolean open) {
		for (Direction side : Direction.values()) {
			if (side.getAxis() != facing.getAxis()) {
				BlockPos beside = pos.relative(side);
				if (level.isLoaded(beside) && level.getBlockState(beside).getBlock() instanceof SecretGate) {
					setGate(level, beside, open);
				}
			}
		}
	}

	/**
	 * Open or close another gate, one tick later. A gate already in that state
	 * is left alone (this is what stops loops), and a powered gate stays open.
	 */
	private static void setGate(Level level, BlockPos pos, boolean open) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof SecretGate gate) || state.getValue(OPEN) == open || (!open && state.getValue(POWERED))) {
			return;
		}
		level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_ALL);
		level.scheduleTick(pos, gate, HOP_DELAY);
	}
}
