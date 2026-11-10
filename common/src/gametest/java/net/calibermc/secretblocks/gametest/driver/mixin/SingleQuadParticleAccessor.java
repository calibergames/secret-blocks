package net.calibermc.secretblocks.gametest.driver.mixin;

import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SingleQuadParticle.class)
public interface SingleQuadParticleAccessor {

	@Accessor("sprite")
	TextureAtlasSprite secretblocksTest$getSprite();
}
