package net.calibermc.secretblocks.util;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.LoggerFactory;

/**
 * Checks for the optional injections into other mods' renderers. Sodium (on
 * Fabric) and Embeddium (on NeoForge and Forge) cull faces in their own
 * {@code BlockOcclusionCache.shouldDrawSide}; the occlusion mixins hook it
 * with {@code require = 0}, since those mods' code may change. The loaders'
 * mixin plugins call {@link #checkOcclusionHook} after applying them, so a
 * version with nothing to hook is reported instead of the Builder's Goggles'
 * view quietly going wrong. (If a future version moves the class itself,
 * Mixin skips the pseudo mixin without a word; the tested versions are in the
 * wiki's Compatibility page.)
 */
public final class MixinHooks {

	/** The end of every injection handler's descriptor: it takes the callback last and returns nothing. */
	static final String HANDLER_DESCRIPTOR_END = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V";

	private MixinHooks() {
	}

	/** Warns when {@code mod}'s occlusion class came through the mixin without the hook. */
	public static void checkOcclusionHook(String mod, ClassNode targetClass) {
		if (!hooked(targetClass, "shouldDrawSide", "secretFaces")) {
			LoggerFactory.getLogger("secretblocks").warn("This {} version has no {}.shouldDrawSide to hook:"
					+ " in the Builder's Goggles' view, faded secret blocks may show the world behind them",
					mod, targetClass.name.replace('/', '.'));
		}
	}

	/**
	 * Whether a method named {@code method} calls the merged injection handler:
	 * a method of the same class whose name ends with {@code handler} and that
	 * takes a {@code CallbackInfoReturnable} last.
	 */
	static boolean hooked(ClassNode targetClass, String method, String handler) {
		for (MethodNode node : targetClass.methods) {
			if (!node.name.equals(method)) {
				continue;
			}
			for (AbstractInsnNode insn : node.instructions) {
				if (insn instanceof MethodInsnNode call && call.owner.equals(targetClass.name)
						&& call.name.endsWith(handler) && call.desc.endsWith(HANDLER_DESCRIPTOR_END)) {
					return true;
				}
			}
		}
		return false;
	}
}
