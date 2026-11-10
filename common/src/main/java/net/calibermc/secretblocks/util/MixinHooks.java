package net.calibermc.secretblocks.util;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.LoggerFactory;

/**
 * Checks for the optional injections into other mods' renderers. Sodium (on
 * Fabric and NeoForge) culls faces in its own block renderer's
 * {@code shouldDrawSide}; the occlusion mixins hook it with {@code require = 0},
 * since that code may change. The loaders' mixin plugins check that Sodium has
 * the class ({@link #checkHookClass}: Mixin skips a pseudo mixin whose class is
 * missing without a word) and, after applying the mixin, that the method was
 * hooked ({@link #checkOcclusionHook}), so a version with nothing to hook is
 * reported instead of the Builder's Goggles' view quietly going wrong.
 */
public final class MixinHooks {

	/** The end of every injection handler's descriptor: it takes the callback last and returns nothing. */
	static final String HANDLER_DESCRIPTOR_END = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V";

	private MixinHooks() {
	}

	/** The class of Sodium's block renderer that the occlusion mixins hook, as a path in its jar. */
	public static final String SODIUM_HOOK_CLASS = "net/caffeinemc/mods/sodium/client/render/model/AbstractBlockRenderContext.class";

	/** Warns when an installed {@code mod} lacks the class its occlusion mixin hooks. */
	public static void checkHookClass(String mod, boolean present) {
		if (!present) {
			LoggerFactory.getLogger("secretblocks").warn("This {} version has no {} to hook:"
					+ " in the Builder's Goggles' view, faded secret blocks may show the world behind them",
					mod, SODIUM_HOOK_CLASS.replace('/', '.').replace(".class", ""));
		}
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
