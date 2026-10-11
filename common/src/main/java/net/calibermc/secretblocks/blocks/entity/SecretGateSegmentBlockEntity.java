package net.calibermc.secretblocks.blocks.entity;

import net.calibermc.secretblocks.blocks.GateSegmentOwner;
import net.calibermc.secretblocks.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * A Secret Gate segment's block entity (its own type,
 * {@code secretblocks:secret_gate_segment}): the {@link Disguise}, and the
 * position of the gate that owns it. The owner is saved with the block
 * (added NBT key {@value #OWNER}) and never sent to clients, so a client
 * cannot find the gate from its segments.
 */
public class SecretGateSegmentBlockEntity extends BlockEntity implements DisguisedBlockEntity {

	/** The NBT key of the owner's position. */
	public static final String OWNER = "owner";

	/** How often a segment asks its owner again, in ticks. */
	public static final int CHECK_INTERVAL = 20;

	private final Disguise disguise = new Disguise();
	@Nullable
	private BlockPos owner;
	/** Ticks until the next check; the first tick after placing or loading checks. Not saved. */
	private int untilCheck = 1;

	public SecretGateSegmentBlockEntity(BlockPos pos, BlockState state) {
		super(ModEntities.SECRET_GATE_SEGMENT, pos, state);
	}

	@Override
	public Disguise getDisguise() {
		return disguise;
	}

	@Nullable
	public BlockPos getOwner() {
		return owner;
	}

	/** Set by the gate that builds this segment. */
	public void setOwner(@Nullable BlockPos owner) {
		this.owner = owner == null ? null : owner.immutable();
		setChanged();
	}

	/** Whether this segment is still wanted (see {@link #isKept(BlockPos, BlockPos, Predicate, Predicate)}). */
	public boolean isKept(Level level, BlockPos pos) {
		return isKept(owner, pos, level::isLoaded, ownerPos -> {
			BlockState ownerState = level.getBlockState(ownerPos);
			return ownerState.getBlock() instanceof GateSegmentOwner gate && gate.keepsSegment(ownerState, level, ownerPos, pos);
		});
	}

	/**
	 * The decision, apart from the world so it can be tested on its own. A
	 * segment at {@code pos} is kept when its {@code owner} could have built it
	 * (in a straight line, at most {@link GateSegmentOwner#MAX_LENGTH} away) and
	 * either the owner's chunk is not loaded (nothing can be known, so it
	 * stays; the check never loads a chunk) or the owner keeps it. An owner
	 * that could never have built it (a structure or a paste from far away)
	 * is not waited for.
	 */
	public static boolean isKept(@Nullable BlockPos owner, BlockPos pos, Predicate<BlockPos> isLoaded, Predicate<BlockPos> ownerKeeps) {
		if (owner == null || !inReach(owner, pos)) {
			return false;
		}
		return !isLoaded.test(owner) || ownerKeeps.test(owner);
	}

	/** {@code pos} lies on one of the six lines out of {@code owner}, 1 to {@link GateSegmentOwner#MAX_LENGTH} blocks away. */
	public static boolean inReach(BlockPos owner, BlockPos pos) {
		int dx = Math.abs(pos.getX() - owner.getX());
		int dy = Math.abs(pos.getY() - owner.getY());
		int dz = Math.abs(pos.getZ() - owner.getZ());
		int axes = (dx > 0 ? 1 : 0) + (dy > 0 ? 1 : 0) + (dz > 0 ? 1 : 0);
		return axes == 1 && dx + dy + dz <= GateSegmentOwner.MAX_LENGTH;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, SecretGateSegmentBlockEntity segment) {
		if (--segment.untilCheck > 0) {
			return;
		}
		segment.untilCheck = CHECK_INTERVAL;
		if (!segment.isKept(level, pos)) {
			// Drops nothing (no loot table) and leaves no block entity behind.
			level.removeBlock(pos, false);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		disguise.save(tag);
		if (owner != null) {
			tag.put(OWNER, NbtUtils.writeBlockPos(owner));
		}
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		disguise.load(tag);
		owner = tag.contains(OWNER, CompoundTag.TAG_COMPOUND) ? NbtUtils.readBlockPos(tag.getCompound(OWNER)) : null;
		DisguisedBlockEntity.redrawAfterLoad(this);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** What clients receive: the disguise only, never the owner's position. */
	@Override
	public CompoundTag getUpdateTag() {
		return disguise.saveForClients(new CompoundTag());
	}
}
