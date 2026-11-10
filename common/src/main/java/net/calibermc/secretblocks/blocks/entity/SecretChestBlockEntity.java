package net.calibermc.secretblocks.blocks.entity;

import net.calibermc.secretblocks.blocks.SecretTrappedChest;
import net.calibermc.secretblocks.screen.SecretChestScreenHandler;
import net.calibermc.secretblocks.util.ImplementedInventory;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.Containers;
import net.minecraft.world.LockCode;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

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
public class SecretChestBlockEntity extends SecretBlockEntity implements ImplementedInventory, Nameable {

	/** A name given with an anvil or by command; shown as the screen's title. Saved as vanilla's {@code CustomName}. */
	@Nullable
	private Component customName;

	/** A vanilla container lock ({@code Lock}): the chest opens only for a player holding an item with that name. */
	private LockCode lockKey = LockCode.NO_LOCK;

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
		public boolean isOwnContainer(Player player) {
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

	@Nullable
	@Override
	public Component getCustomName() {
		return customName;
	}

	/** The custom name, or the block's own; a named chest drops a named item ({@link #collectImplicitComponents}). */
	@Override
	public Component getName() {
		return customName != null ? customName : getBlockState().getBlock().getName();
	}

	public void setCustomName(@Nullable Component name) {
		customName = name;
		setChanged();
	}

	public LockCode getLockKey() {
		return lockKey;
	}

	/** Name and lock use vanilla's keys, so they carry over when the Camouflage Wand converts a chest. */
	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.storeNullable("CustomName", ComponentSerialization.CODEC, customName);
		lockKey.addToTag(output);
	}

	@Override
	protected void loadUpgraded(ValueInput input) {
		super.loadUpgraded(input);
		customName = parseCustomNameSafe(input, "CustomName");
		lockKey = LockCode.fromTag(input);
	}

	/** The custom name, for the item a broken chest drops (its loot table copies {@code minecraft:custom_name}). */
	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		components.set(DataComponents.CUSTOM_NAME, customName);
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
	public void startOpen(ContainerUser user) {
		if (!remove && !user.getLivingEntity().isSpectator()) {
			openers.incrementOpeners(user.getLivingEntity(), getLevel(), getBlockPos(), getBlockState(), user.getContainerInteractionRange());
		}
	}

	@Override
	public void stopOpen(ContainerUser user) {
		if (!remove && !user.getLivingEntity().isSpectator()) {
			openers.decrementOpeners(user.getLivingEntity(), getLevel(), getBlockPos(), getBlockState());
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
