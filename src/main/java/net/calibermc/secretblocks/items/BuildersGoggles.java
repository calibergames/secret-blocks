package net.calibermc.secretblocks.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Builder's Goggles: worn in the helmet slot, they show the wearer every
 * secret block faded with its real block inside ({@code render/BuilderView}).
 * A builder's tool: no armour, they never wear out, no recipe and no loot
 * (creative inventory and {@code /give} only). They look like the Secret
 * Goggles when worn.
 */
public class BuildersGoggles extends ArmorItem {

	public BuildersGoggles(Properties properties) {
		super(Material.INSTANCE, EquipmentSlot.HEAD, properties);
	}

	/** No armour or toughness modifiers at all (not even "+0 Armor"). */
	@Override
	public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
		return ImmutableMultimap.of();
	}

	/** Their armour material: no durability (they never wear out), no protection, not enchantable. */
	public enum Material implements ArmorMaterial {
		INSTANCE;

		@Override
		public int getDurabilityForSlot(EquipmentSlot slot) {
			return 0;
		}

		@Override
		public int getDefenseForSlot(EquipmentSlot slot) {
			return 0;
		}

		@Override
		public int getEnchantmentValue() {
			return 0;
		}

		@Override
		public SoundEvent getEquipSound() {
			return SoundEvents.ARMOR_EQUIP_GENERIC;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return Ingredient.EMPTY;
		}

		/** Worn, they use the Secret Goggles' texture ({@code secretblocks_goggles_layer_1.png}). */
		@Override
		public String getName() {
			return "secretblocks_goggles";
		}

		@Override
		public float getToughness() {
			return 0;
		}

		@Override
		public float getKnockbackResistance() {
			return 0;
		}
	}
}
