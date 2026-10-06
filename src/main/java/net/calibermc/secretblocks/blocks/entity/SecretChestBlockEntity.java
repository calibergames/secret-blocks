package net.calibermc.secretblocks.blocks.entity;

import net.calibermc.secretblocks.blocks.SecretTrappedChest;
import net.calibermc.secretblocks.screen.SecretChestScreenHandler;
import net.calibermc.secretblocks.util.ImplementedInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The block entity of the Secret Chest and the Secret Trapped Chest: the
 * shared {@link SecretBlockEntity} (same type and NBT) whose 27 slots are
 * a real container, so hoppers, droppers, hopper minecarts and comparators
 * use it as they use a vanilla chest, and its screen opens on it.
 *
 * It counts the players looking into it with vanilla's
 * {@link ContainerOpenersCounter}, as a vanilla chest does: the
 * menu starts and ends a viewing, and a recheck every 5 ticks while it is
 * open drops players who left without closing the screen. The Secret Trapped
 * Chest turns the count into a redstone signal; this class tells the
 * neighbours when it changes. Unlike a vanilla chest it makes no sound: a
 * secret chest that creaks would give itself away.
 *
 * It is deliberately not a {@code MenuProvider}: the chest's menu type can
 * only be opened with the extended factory from
 * {@code SecretChest.getMenuProvider} (handing the block entity to
 * {@code openMenu} is what made the server refuse a spectator's click).
 */
public class SecretChestBlockEntity extends SecretBlockEntity implements ImplementedInventory {

	private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
		@Override
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
		}

		@Override
		protected void onClose(Level level, BlockPos pos, BlockState state) {
		}

		/** As vanilla's trapped chest: the chest's neighbours, and those of the block below (strong power). */
		@Override
		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int before, int now) {
			if (before != now && state.getBlock() instanceof SecretTrappedChest) {
				level.updateNeighborsAt(pos, state.getBlock());
				level.updateNeighborsAt(pos.below(), state.getBlock());
			}
		}

		@Override
		protected boolean isOwnContainer(Player player) {
			return player.containerMenu instanceof SecretChestScreenHandler menu && menu.getContainer() == SecretChestBlockEntity.this;
		}
	};

	public SecretChestBlockEntity(BlockPos pos, BlockState state) {
		super(pos, state);
	}

	@Override
	public NonNullList<ItemStack> getItems() {
		return items();
	}

	/** Like a vanilla container: a changed slot is saved and comparators hear of it. */
	@Override
	public void setItem(int slot, ItemStack stack) {
		ImplementedInventory.super.setItem(slot, stack);
		setChanged();
	}

	/** The screen closes when the chest is gone or the player is more than 8 blocks away, as vanilla. */
	@Override
	public boolean stillValid(Player player) {
		return level != null && level.getBlockEntity(worldPosition) == this
				&& player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
	}

	@Override
	public void startOpen(Player player) {
		if (!remove && !player.isSpectator()) {
			openers.incrementOpeners(player, getLevel(), getBlockPos(), getBlockState());
		}
	}

	@Override
	public void stopOpen(Player player) {
		if (!remove && !player.isSpectator()) {
			openers.decrementOpeners(player, getLevel(), getBlockPos(), getBlockState());
		}
	}

	/** The scheduled recheck (the block's tick): count again who really has it open. */
	public void recheckOpen() {
		if (!remove) {
			openers.recheckOpeners(getLevel(), getBlockPos(), getBlockState());
		}
	}

	/** How many players have this chest open. */
	public int getOpenerCount() {
		return openers.getOpenerCount();
	}

	/** A removed chest drops its contents once and tells comparators, as vanilla's chest does. */
	@Override
	public void dropStoredItems(Level level, BlockPos pos) {
		Containers.dropContents(level, pos, this);
		level.updateNeighbourForOutputSignal(pos, getBlockState().getBlock());
	}
}
