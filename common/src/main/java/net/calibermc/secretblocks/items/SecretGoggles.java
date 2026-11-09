package net.calibermc.secretblocks.items;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Secret Goggles: worn in the helmet slot, no armor value. While worn, secret
 * blocks show an overlay on the wearer's client (render/GogglesView). They
 * wear out: once a second there is a 20% chance to lose 1 of 900 durability;
 * they break at 0. Repaired by combining two or with a prismarine shard at an
 * anvil; the enchantments for durability items apply (they are in
 * {@code #minecraft:enchantable/durability}), never from the enchanting table
 * (enchantability 0).
 */
public class SecretGoggles extends ArmorItem {

	public static final int DURABILITY = 900;
	public static final int WEAR_INTERVAL_TICKS = 20;
	public static final float WEAR_CHANCE = 0.2f;

	/**
	 * The goggles' armor material: no protection, not enchantable at a table.
	 * Armor textures are looked up from the layer's name:
	 * {@code assets/minecraft/textures/models/armor/secretblocks_goggles_layer_1.png}.
	 * A direct holder, not a registered material: the goggles are not
	 * trimmable, and nothing else looks their material up by name.
	 */
	public static final Holder<ArmorMaterial> MATERIAL = material(() -> Ingredient.of(Items.PRISMARINE_SHARD));

	public SecretGoggles(Properties properties) {
		super(MATERIAL, ArmorItem.Type.HELMET, properties.durability(DURABILITY));
	}

	/** An armor material with no protection, the generic equip sound and the goggles' texture. */
	static Holder<ArmorMaterial> material(Supplier<Ingredient> repair) {
		Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
		for (ArmorItem.Type type : ArmorItem.Type.values()) {
			defense.put(type, 0);
		}
		return Holder.direct(new ArmorMaterial(defense, 0, SoundEvents.ARMOR_EQUIP_GENERIC, repair,
				List.of(new ArmorMaterial.Layer(ResourceLocation.withDefaultNamespace("secretblocks_goggles"))), 0, 0));
	}

	/** No armor or toughness modifiers at all (not even "+0 Armor"). */
	@Override
	public ItemAttributeModifiers getDefaultAttributeModifiers() {
		return ItemAttributeModifiers.EMPTY;
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
			stack.hurtAndBreak(1, wearer, EquipmentSlot.HEAD);
		}
	}
}
