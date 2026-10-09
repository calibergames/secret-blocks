package net.calibermc.secretblocks.mixin;

import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The centre of an explosion (1.19.2 has no getter for it). */
@Mixin(Explosion.class)
public interface ExplosionAccessor {

	@Accessor("x")
	double secretblocks$getX();

	@Accessor("y")
	double secretblocks$getY();

	@Accessor("z")
	double secretblocks$getZ();
}
