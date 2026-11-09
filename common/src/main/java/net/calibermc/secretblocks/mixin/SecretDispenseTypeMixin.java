package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.SecretBlock;
import net.calibermc.secretblocks.registry.ModEntities;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Vanilla's dispenser and dropper look up the block entity they dispense from
 * by vanilla's own type, and skip any other. The Secret Dispenser and Secret
 * Dropper keep vanilla's dispensing and look up their own type instead.
 */
@Mixin({DispenserBlock.class, DropperBlock.class})
public abstract class SecretDispenseTypeMixin {

	@ModifyArg(method = "dispenseFrom", index = 1,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntityType;)Ljava/util/Optional;"))
	private BlockEntityType<?> secretblocks$ownType(BlockEntityType<?> type) {
		return (Object) this instanceof SecretBlock ? ModEntities.SECRET_DISPENSER : type;
	}
}
