package net.calibermc.secretblocks.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.TagValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The tag a tag-backed value input reads from. */
@Mixin(TagValueInput.class)
public interface TagValueInputAccessor {

	@Accessor("input")
	CompoundTag secretblocks$input();
}
