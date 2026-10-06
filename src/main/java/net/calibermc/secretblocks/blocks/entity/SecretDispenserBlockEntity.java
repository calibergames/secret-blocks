package net.calibermc.secretblocks.blocks.entity;

import net.calibermc.secretblocks.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Secret Dispenser's block entity: a vanilla dispenser block entity, so
 * vanilla dispensing, the dispenser screen, comparators and dropping the
 * contents all work unchanged, plus the {@link Disguise} (same NBT keys as
 * every other secret block). The Secret Dropper uses it too, as
 * vanilla's dropper uses a dispenser block entity with another name.
 */
public class SecretDispenserBlockEntity extends DispenserBlockEntity implements DisguisedBlockEntity {

	private final Disguise disguise = new Disguise();

	public SecretDispenserBlockEntity(BlockPos pos, BlockState state) {
		super(ModEntities.SECRET_DISPENSER, pos, state);
	}

	@Override
	public Disguise getDisguise() {
		return disguise;
	}

	/** The screen title: the block's own name ("Secret Dispenser", "Secret Dropper"), as the Secret Chest shows its own. */
	@Override
	protected Component getDefaultName() {
		return Component.translatable(getBlockState().getBlock().getDescriptionId());
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
	// update(); resending on every inventory change would make clients
	// redraw the block each time an item moves.

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** What clients receive: the disguise only, never the contents. */
	@Override
	public CompoundTag getUpdateTag() {
		return disguise.saveForClients(new CompoundTag());
	}
}
