package net.calibermc.secretblocks.blocks.entity;

import net.calibermc.secretblocks.mixin.BlockEntityTypeAccessor;
import net.calibermc.secretblocks.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Secret Comparator's block entity: a vanilla comparator block entity
 * (vanilla's comparator code looks for exactly that class and keeps the
 * output signal in it), plus the {@link Disguise}.
 *
 * 1.19.2's {@code ComparatorBlockEntity} constructor hard-wires the vanilla
 * type. This one sets its own type ({@link ModEntities#SECRET_COMPARATOR})
 * through {@link BlockEntityTypeAccessor}, as the Secret Hopper does, so it
 * saves and loads as a Secret Comparator and keeps its disguise.
 */
public class SecretComparatorBlockEntity extends ComparatorBlockEntity implements DisguisedBlockEntity {

	private final Disguise disguise = new Disguise();

	public SecretComparatorBlockEntity(BlockPos pos, BlockState state) {
		super(pos, state);
		((BlockEntityTypeAccessor) (Object) this).secretblocks$setType(ModEntities.SECRET_COMPARATOR);
	}

	@Override
	public Disguise getDisguise() {
		return disguise;
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		disguise.save(tag);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		disguise.load(tag);
		DisguisedBlockEntity.redrawAfterLoad(this);
	}

	// No setChanged() override: disguise changes reach clients through
	// update(); the output signal changes often and clients never need it.

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** What clients receive: the disguise only, never the output signal. */
	@Override
	public CompoundTag getUpdateTag() {
		return disguise.save(new CompoundTag());
	}
}
