package net.calibermc.secretblocks.blocks.entity;

import net.calibermc.secretblocks.mixin.BlockEntityTypeAccessor;
import net.calibermc.secretblocks.registry.ModEntities;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Secret Hopper's block entity: a vanilla hopper block entity, so item
 * transfer (including the hopper-to-hopper cooldowns redstone builds rely
 * on), the hopper screen, comparators and dropping the contents are all
 * vanilla, plus the {@link Disguise}.
 *
 * 1.20.1's {@code HopperBlockEntity} constructor hard-wires the vanilla type.
 * This one sets its own type ({@link ModEntities#SECRET_HOPPER}) through
 * {@link BlockEntityTypeAccessor}, so it saves and loads as a Secret Hopper
 * (keeping its disguise) and the Secret Hopper block's ticker runs it.
 */
public class SecretHopperBlockEntity extends HopperBlockEntity implements DisguisedBlockEntity {

	private final Disguise disguise = new Disguise();

	public SecretHopperBlockEntity(BlockPos pos, BlockState state) {
		super(pos, state);
		((BlockEntityTypeAccessor) (Object) this).secretblocks$setType(ModEntities.SECRET_HOPPER);
	}

	/**
	 * Valid where a Secret Hopper is. Vanilla's constructor checks the state against
	 * the type before this class sets its own, so this answers for that type.
	 */
	@Override
	public boolean isValidBlockState(BlockState state) {
		return ModEntities.SECRET_HOPPER.isValid(state);
	}

	@Override
	public Disguise getDisguise() {
		return disguise;
	}

	/** The screen title: "Secret Hopper", as the other secret containers show their own names. */
	@Override
	protected Component getDefaultName() {
		return Component.translatable(getBlockState().getBlock().getDescriptionId());
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		disguise.save(tag);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		LegacyData.upgradeItems(tag);
		super.loadAdditional(tag, registries);
		disguise.load(tag);
		DisguisedBlockEntity.redrawAfterLoad(this);
	}

	// No setChanged() override: disguise changes reach clients through
	// update(); resending on every transfer would make clients redraw the
	// block each time an item moves.

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** What clients receive: the disguise only, never the contents. */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return disguise.saveForClients(new CompoundTag());
	}
}
