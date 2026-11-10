package net.calibermc.secretblocks.blocks.entity;

import net.calibermc.secretblocks.blocks.GateSegmentOwner;
import net.calibermc.secretblocks.registry.ModEntities;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		disguise.save(tag);
		if (owner != null) {
			tag.put(OWNER, writePos(owner));
		}
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		disguise.load(tag);
		owner = tag.get(OWNER) instanceof CompoundTag ? readPos(tag.getCompoundOrEmpty(OWNER)) : null;
		DisguisedBlockEntity.redrawAfterLoad(this);
	}

	/** The owner's position as {@code {X, Y, Z}}, the format worlds saved by earlier versions hold. */
	private static CompoundTag writePos(BlockPos pos) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("X", pos.getX());
		tag.putInt("Y", pos.getY());
		tag.putInt("Z", pos.getZ());
		return tag;
	}

	private static BlockPos readPos(CompoundTag tag) {
		return new BlockPos(tag.getIntOr("X", 0), tag.getIntOr("Y", 0), tag.getIntOr("Z", 0));
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** What clients receive: the disguise only, never the owner's position. */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return disguise.saveForClients(new CompoundTag());
	}

	/**
	 * The segment is being replaced by another block: its gate takes it out of
	 * the line (the gate's own removal of its segments comes through here too).
	 */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null && owner != null && level.isLoaded(owner)) {
			BlockState ownerState = level.getBlockState(owner);
			if (ownerState.getBlock() instanceof GateSegmentOwner gate) {
				gate.segmentRemoved(ownerState, level, owner, pos);
			}
		}
	}
}
