package net.calibermc.secretblocks.items;

import net.minecraft.world.item.Item;

/**
 * Builder's Goggles: worn in the helmet slot, they show the wearer every
 * secret block faded with its real block inside ({@code render/BuilderView}).
 * A builder's tool: no armour, they never wear out, no recipe and no loot
 * (creative inventory and {@code /give} only). They look like the Secret
 * Goggles when worn.
 */
public class BuildersGoggles extends Item {

	/**
	 * Worn like the Secret Goggles (see {@link SecretGoggles#helmet}). Not
	 * enchantable, and nothing repairs them: they have no durability, so they
	 * never wear out.
	 */
	public BuildersGoggles(Properties properties) {
		super(SecretGoggles.helmet(properties));
	}
}
