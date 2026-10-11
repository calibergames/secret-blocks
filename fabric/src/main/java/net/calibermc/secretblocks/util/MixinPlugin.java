package net.calibermc.secretblocks.util;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies the mixins that target another mod only when that mod is
 * installed: {@code SodiumOcclusionMixin} needs Sodium,
 * {@code ContinuityConnectionMixin} Continuity 2.0.x.
 * Without this, Mixin looks for Sodium's class and logs a warning on every
 * launch without Sodium. Lives outside the mixin package, which Mixin
 * reserves for mixin classes.
 */
public class MixinPlugin implements IMixinConfigPlugin {

	private static final String SODIUM_MIXIN = "net.calibermc.secretblocks.fabric.mixin.SodiumOcclusionMixin";
	private static final String CONTINUITY_MIXIN = "net.calibermc.secretblocks.fabric.mixin.ContinuityConnectionMixin";

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.equals(SODIUM_MIXIN)) {
			return FabricLoader.getInstance().isModLoaded("sodium");
		}
		if (mixinClassName.equals(CONTINUITY_MIXIN)) {
			// Written against Continuity 2.0.x; another version is left alone rather than risk a failed start.
			return FabricLoader.getInstance().getModContainer("continuity")
					.map(mod -> mod.getMetadata().getVersion().getFriendlyString().startsWith("2.0."))
					.orElse(false);
		}
		return true;
	}

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
