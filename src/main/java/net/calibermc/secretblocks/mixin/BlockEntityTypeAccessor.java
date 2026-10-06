package net.calibermc.secretblocks.mixin;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets a subclass of a vanilla block entity whose constructor hard-wires the
 * vanilla type (1.19.2's {@code HopperBlockEntity}) carry its own type, so it
 * saves and loads as ours and our block's ticker accepts it. Used only in
 * constructors ({@link net.calibermc.secretblocks.blocks.entity.SecretHopperBlockEntity}).
 */
@Mixin(BlockEntity.class)
public interface BlockEntityTypeAccessor {

	@Mutable
	@Accessor("type")
	void secretblocks$setType(BlockEntityType<?> type);
}
