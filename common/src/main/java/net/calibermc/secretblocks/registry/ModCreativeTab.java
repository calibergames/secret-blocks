package net.calibermc.secretblocks.registry;

import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The mod's creative inventory tab. Items join it as they are registered
 * ({@link #add}), so the tab lists them in registration order: the blocks,
 * then the tools.
 */
public final class ModCreativeTab {

	private static final List<Item> ITEMS = new ArrayList<>();

	private ModCreativeTab() {
	}

	static <T extends Item> T add(T item) {
		ITEMS.add(item);
		return item;
	}

	/** After every item is registered. */
	public static void register() {
		Services.PLATFORM.register(BuiltInRegistries.CREATIVE_MODE_TAB, SecretBlocks.id("secret_blocks"), Services.PLATFORM.creativeTab()
				.title(Component.translatable("itemGroup.secretblocks.secret_blocks"))
				.icon(() -> new ItemStack(ModItems.CAMOUFLAGE_PASTE))
				.displayItems((parameters, output) -> ITEMS.forEach(output::accept))
				.build());
	}
}
