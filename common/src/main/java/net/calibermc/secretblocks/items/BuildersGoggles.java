package net.calibermc.secretblocks.items;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Builder's Goggles: worn in the helmet slot, they show the wearer every
 * secret block faded with its real block inside ({@code render/BuilderView}).
 * A builder's tool: no armour, they never wear out, no recipe and no loot
 * (creative inventory and {@code /give} only). They look like the Secret
 * Goggles when worn.
 */
public class BuildersGoggles extends ArmorItem {

	/**
	 * Their armour material: no protection, not enchantable, nothing repairs
	 * them; worn, they use the Secret Goggles' texture. They have no
	 * durability (no maximum damage on the item), so they never wear out.
	 */
	public static final Holder<ArmorMaterial> MATERIAL = SecretGoggles.material(() -> Ingredient.EMPTY);

	public BuildersGoggles(Properties properties) {
		super(MATERIAL, ArmorItem.Type.HELMET, properties);
	}

	/** No armour or toughness modifiers at all (not even "+0 Armor"). */
	@Override
	public ItemAttributeModifiers getDefaultAttributeModifiers() {
		return ItemAttributeModifiers.EMPTY;
	}
}
