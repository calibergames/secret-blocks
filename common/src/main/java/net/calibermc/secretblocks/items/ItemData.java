package net.calibermc.secretblocks.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/**
 * The data the Camouflage Wand and the Switch Probe keep on their item, in
 * vanilla's {@code minecraft:custom_data} component under the same keys as
 * the item tag of older versions: upgrading a world moves an old item's tag
 * there, so wands and probes made before keep what they hold.
 */
public final class ItemData {

	private ItemData() {
	}

	/** Whether the item holds any data. */
	public static boolean has(ItemStack stack) {
		return stack.has(DataComponents.CUSTOM_DATA);
	}

	/** A copy of the item's data; empty when it holds none. Changing it changes nothing on the item. */
	public static CompoundTag read(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
	}

	/** Changes the item's data; an item whose data ends up empty holds none. */
	public static void update(ItemStack stack, Consumer<CompoundTag> change) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, change);
	}
}
