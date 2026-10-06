package net.calibermc.secretblocks.blocks.entity;

import net.calibermc.secretblocks.mixin.BlockEntityTypeAccessor;
import net.calibermc.secretblocks.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Secret Sculk Sensor's block entity: a vanilla sculk sensor block entity
 * (vanilla's sensor code looks for exactly that class; it holds the vibration
 * listener, the vibration in flight and the last frequency), plus the
 * {@link Disguise}.
 *
 * 1.19.2's {@code SculkSensorBlockEntity} constructor hard-wires the vanilla
 * type. This one sets its own type ({@link ModEntities#SECRET_SCULK_SENSOR})
 * through {@link BlockEntityTypeAccessor}, as the Secret Hopper does, so it
 * saves and loads as a Secret Sculk Sensor (keeping its disguise and the
 * vibration in flight) and the block's ticker runs it.
 */
public class SecretSculkSensorBlockEntity extends SculkSensorBlockEntity implements DisguisedBlockEntity {

	private final Disguise disguise = new Disguise();

	public SecretSculkSensorBlockEntity(BlockPos pos, BlockState state) {
		super(pos, state);
		((BlockEntityTypeAccessor) (Object) this).secretblocks$setType(ModEntities.SECRET_SCULK_SENSOR);
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
	// update(); vanilla marks the sensor changed for every vibration.

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** What clients receive: the disguise only, never the vibration listener. */
	@Override
	public CompoundTag getUpdateTag() {
		return disguise.saveForClients(new CompoundTag());
	}
}
