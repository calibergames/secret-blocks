package net.calibermc.secretblocks.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

/**
 * Secret Goggles: worn in the helmet slot, no armor value. While worn, secret
 * blocks show an overlay on the wearer's client (render/GogglesView). They
 * wear out: once a second there is a 20% chance to lose 1 of 900 durability;
 * they break at 0. Repaired by combining two or with a prismarine shard at an
 * anvil; only Unbreaking and Mending apply (mixin/GogglesEnchantmentMixin),
 * never from the enchanting table (enchantability 0).
 */
public class SecretGoggles extends ArmorItem {

	public static final int DURABILITY = 900;
	public static final int WEAR_INTERVAL_TICKS = 20;
	public static final float WEAR_CHANCE = 0.2f;

	public SecretGoggles(Properties properties) {
		super(Material.INSTANCE, ArmorItem.Type.HELMET, properties);
	}

	/** No armor or toughness modifiers at all (not even "+0 Armor"). */
	@Override
	public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
		return ImmutableMultimap.of();
	}

	/**
	 * Wear while worn: called for every stack in a player's inventory, so act
	 * only for the stack that is in the head slot (armor and main-inventory
	 * slot indices overlap). Server only.
	 */
	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		if (!level.isClientSide() && entity instanceof LivingEntity wearer
				&& wearer.getItemBySlot(EquipmentSlot.HEAD) == stack
				&& wearer.tickCount % WEAR_INTERVAL_TICKS == 0
				&& level.getRandom().nextFloat() < WEAR_CHANCE) {
			stack.hurtAndBreak(1, wearer, broken -> broken.broadcastBreakEvent(EquipmentSlot.HEAD));
		}
	}

	/** The goggles' armor material: durability 900, no protection, not enchantable at a table. */
	public enum Material implements ArmorMaterial {
		INSTANCE;

		@Override
		public int getDurabilityForType(ArmorItem.Type type) {
			return DURABILITY;
		}

		@Override
		public int getDefenseForType(ArmorItem.Type type) {
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
			return Ingredient.of(Items.PRISMARINE_SHARD);
		}

		/**
		 * Armor textures are looked up in the {@code minecraft} namespace from
		 * this name: {@code assets/minecraft/textures/models/armor/secretblocks_goggles_layer_1.png}.
		 */
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
