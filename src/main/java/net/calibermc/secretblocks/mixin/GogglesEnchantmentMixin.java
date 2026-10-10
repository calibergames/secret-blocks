package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.DamageEnchantment;
import net.minecraft.world.item.enchantment.DigDurabilityEnchantment;
import net.minecraft.world.item.enchantment.DiggingEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ThornsEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Secret Goggles accept only Unbreaking and Mending; the Builder's Goggles
 * accept nothing (as armour they would take Protection, Thorns, Respiration
 * and the curses). {@code canEnchant} is what the anvil (books) and
 * {@code /enchant} check; the enchanting table offers nothing because the
 * goggles' enchantability is 0. 1.20.1 has no loader hook for this; later
 * versions can use enchantment tags.
 *
 * Targets the classes that declare {@code canEnchant(ItemStack)} in 1.20.1.
 * Thorns and Unbreaking return true for armor before reaching the base
 * method, so the base alone is not enough (a mutation run confirmed it for
 * Thorns); the other two are covered for uniformity. Binding Curse's
 * override calls the base method, so the base covers it.
 */
@Mixin({Enchantment.class, ThornsEnchantment.class, DamageEnchantment.class, DiggingEnchantment.class, DigDurabilityEnchantment.class})
public abstract class GogglesEnchantmentMixin {

	@Inject(method = "canEnchant", at = @At("HEAD"), cancellable = true)
	private void secretblocks$onlyUnbreakingAndMendingOnGoggles(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (stack.is(ModItems.SECRET_GOGGLES)) {
			Object self = this;
			cir.setReturnValue(self == Enchantments.UNBREAKING || self == Enchantments.MENDING);
		} else if (stack.is(ModItems.BUILDERS_GOGGLES)) {
			cir.setReturnValue(false);
		}
	}
}
