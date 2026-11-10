package net.calibermc.secretblocks.items;

import net.calibermc.secretblocks.SecretBlocks;
import org.jetbrains.annotations.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Secret Goggles: worn in the helmet slot, no armor value. While worn, secret
 * blocks show an overlay on the wearer's client (render/GogglesView). They
 * wear out: once a second there is a 20% chance to lose 1 of 900 durability;
 * they break at 0. Repaired by combining two or with a prismarine shard at an
 * anvil; the enchantments for durability items apply (they are in
 * {@code #minecraft:enchantable/durability}), never from the enchanting table
 * (they are not enchantable there).
 */
public class SecretGoggles extends Item {

	public static final int DURABILITY = 900;
	public static final int WEAR_INTERVAL_TICKS = 20;
	public static final float WEAR_CHANCE = 0.2f;

	/**
	 * How both pairs of goggles look when worn:
	 * {@code assets/secretblocks/equipment/goggles.json}, which names the texture
	 * {@code assets/secretblocks/textures/entity/equipment/humanoid/goggles.png}.
	 */
	public static final ResourceKey<EquipmentAsset> EQUIPMENT_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID,
			SecretBlocks.id("goggles"));

	public SecretGoggles(Properties properties) {
		super(helmet(properties).durability(DURABILITY).repairable(Items.PRISMARINE_SHARD));
	}

	/**
	 * Worn on the head, with the generic equip sound and the goggles' look. No
	 * armor, toughness or other attribute modifiers at all (not even "+0
	 * Armor"), and no enchantability, so the enchanting table takes neither
	 * pair. Use, a dispenser and damage while worn work as for any helmet.
	 */
	static Properties helmet(Properties properties) {
		return properties.attributes(ItemAttributeModifiers.EMPTY)
				.component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD)
						.setEquipSound(SoundEvents.ARMOR_EQUIP_GENERIC).setAsset(EQUIPMENT_ASSET).build());
	}

	/**
	 * Wear while worn: called for every stack in a player's inventory, so act
	 * only for the stack that is in the head slot (armor and main-inventory
	 * slot indices overlap). Server only.
	 */
	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
		if (entity instanceof LivingEntity wearer
				&& wearer.getItemBySlot(EquipmentSlot.HEAD) == stack
				&& wearer.tickCount % WEAR_INTERVAL_TICKS == 0
				&& level.getRandom().nextFloat() < WEAR_CHANCE) {
			stack.hurtAndBreak(1, wearer, EquipmentSlot.HEAD);
		}
	}
}
