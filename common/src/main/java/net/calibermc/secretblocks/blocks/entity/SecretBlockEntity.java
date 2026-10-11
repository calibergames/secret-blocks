package net.calibermc.secretblocks.blocks.entity;

import net.calibermc.secretblocks.blocks.SecretPiston;
import net.calibermc.secretblocks.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The block entity of most secret blocks: the {@link Disguise}, plus a
 * 27-slot item list that every secret block has always saved.
 *
 * Only the Secret Chest's block entity ({@link SecretChestBlockEntity}, the
 * same type) is a container. Every other secret block is not, as its vanilla
 * counterpart is not: hoppers, droppers and hopper minecarts treat it like
 * stone (before 2.0.0 they could push items into it, where they were lost).
 * Items that got in that way stay saved in the list and drop when the block
 * is broken ({@link #dropStoredItems}).
 */
public class SecretBlockEntity extends BlockEntity implements DisguisedBlockEntity, BlockEntityTicker<SecretBlockEntity> {

	private final NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);  // 9 -> 27
	private final Disguise disguise = new Disguise();

	public SecretBlockEntity(BlockPos pos, BlockState state) {
		super(ModEntities.SECRET_BLOCK_ENTITY, pos, state);
	}

	@Override
	public Disguise getDisguise() {
		return disguise;
	}

	@Override
	public void tick(Level world, BlockPos pos, BlockState state, SecretBlockEntity blockEntity) {
		// Never called: the one block that gives this type a ticker (the Secret Daylight Detector)
		// uses its own, which does not call this. Kept so `age` stays in the NBT.
		disguise.tickAge();
	}

	/** A Secret Piston's head follows its base's disguise while extended. */
	@Override
	public void markDisguiseChanged() {
		DisguisedBlockEntity.super.markDisguiseChanged();
		SecretPiston.disguiseChanged(this);
	}

	/** The saved item list: the Secret Chest's slots, or items stuck in another block before 2.0.0. */
	protected NonNullList<ItemStack> items() {
		return items;
	}

	/**
	 * Drop the items stuck inside a block that is not a container (put there by
	 * a hopper before 2.0.0). Called when the block is removed; the Secret
	 * Chest drops its contents itself.
	 */
	public void dropStoredItems(Level level, BlockPos pos) {
		if (!items.stream().allMatch(ItemStack::isEmpty)) {
			Containers.dropContents(level, pos, items);
			items.clear();
		}
	}

	@Override
	public void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		disguise.save(tag);
		ContainerHelper.saveAllItems(tag, this.items);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		disguise.load(tag);
		ContainerHelper.loadAllItems(tag, this.items);
		DisguisedBlockEntity.redrawAfterLoad(this);
	}

	@Override
	public void setChanged() {
		super.setChanged();
		DisguisedBlockEntity.sendOnChange(this);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/**
	 * What clients receive: the disguise only. A Secret Chest's contents reach
	 * a player through its menu when they open it, as with a vanilla chest,
	 * not to every client in range.
	 */
	@Override
	public CompoundTag getUpdateTag() {
		return disguise.saveForClients(new CompoundTag());
	}
}
