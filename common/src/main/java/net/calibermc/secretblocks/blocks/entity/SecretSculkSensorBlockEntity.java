package net.calibermc.secretblocks.blocks.entity;

import net.calibermc.secretblocks.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import org.jetbrains.annotations.Nullable;

/**
 * The Secret Sculk Sensor's block entity: a vanilla sculk sensor block entity
 * (vanilla's sensor code looks for exactly that class; it holds the vibration
 * listener, the vibration in flight and the last frequency), plus the
 * {@link Disguise}.
 *
 * It has its own type ({@link ModEntities#SECRET_SCULK_SENSOR}), through the
 * constructor vanilla's calibrated sensor uses, so it saves and loads as a
 * Secret Sculk Sensor (keeping its disguise and the vibration in flight) and
 * the block's ticker runs it.
 */
public class SecretSculkSensorBlockEntity extends SculkSensorBlockEntity implements DisguisedBlockEntity {

	private final Disguise disguise = new Disguise();

	public SecretSculkSensorBlockEntity(BlockPos pos, BlockState state) {
		super(ModEntities.SECRET_SCULK_SENSOR, pos, state);
	}

	@Override
	public VibrationSystem.User createVibrationUser() {
		return new SecretVibrationUser(getBlockPos());
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

	/**
	 * Vanilla's sensor is half a block tall, so a mob standing on it is in
	 * the sensor's own block, and its steps and landing give full power. This
	 * sensor is a full block, so the mob stands in the block above: such a
	 * vibration counts as at the sensor, for the same power. Anything else
	 * in the block above (a block placed on top) is one block away, as for
	 * vanilla's.
	 */
	protected class SecretVibrationUser extends SculkSensorBlockEntity.VibrationUser {

		public SecretVibrationUser(BlockPos pos) {
			super(pos);
		}

		@Override
		public void onReceiveVibration(ServerLevel level, BlockPos pos, GameEvent gameEvent, @Nullable Entity entity,
				@Nullable Entity playerEntity, float distance) {
			boolean standingOn = entity != null && pos.equals(blockPos.above()) && entity.getOnPos().equals(blockPos);
			super.onReceiveVibration(level, pos, gameEvent, entity, playerEntity, standingOn ? 0.0F : distance);
		}
	}
}
