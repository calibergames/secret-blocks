package net.calibermc.secretblocks.util;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The occlusion hook check: it finds the merged handler call only in the hooked method. */
class MixinHooksTest {

	private static final String HANDLER_DESC = "(Ljava/lang/Object;" + MixinHooks.HANDLER_DESCRIPTOR_END;

	private static ClassNode target(String method, String calls) {
		return target(method, "Target", calls, HANDLER_DESC);
	}

	private static ClassNode target(String method, String owner, String calls, String desc) {
		ClassNode node = new ClassNode();
		node.name = "Target";
		MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, method, "()Z", null, null);
		if (calls != null) {
			m.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, owner, calls, desc, false));
		}
		m.instructions.add(new InsnNode(Opcodes.ICONST_1));
		m.instructions.add(new InsnNode(Opcodes.IRETURN));
		node.methods.add(m);
		return node;
	}

	@Test
	void findsTheMergedHandlerCall() {
		assertTrue(MixinHooks.hooked(target("shouldDrawSide", "handler$zza000$secretblocks$secretFaces"),
				"shouldDrawSide", "secretFaces"));
	}

	@Test
	void reportsAMethodWithoutTheCall() {
		assertFalse(MixinHooks.hooked(target("shouldDrawSide", "somethingElse"), "shouldDrawSide", "secretFaces"));
		assertFalse(MixinHooks.hooked(target("shouldDrawSide", null), "shouldDrawSide", "secretFaces"));
	}

	@Test
	void ignoresALookalikeCall() {
		assertFalse(MixinHooks.hooked(target("shouldDrawSide", "Other", "handler$zza000$secretblocks$secretFaces", HANDLER_DESC),
				"shouldDrawSide", "secretFaces"), "another class's method");
		assertFalse(MixinHooks.hooked(target("shouldDrawSide", "Target", "cacheSecretFaces", "()V"),
				"shouldDrawSide", "secretFaces"), "a method that is no injection handler");
	}

	@Test
	void reportsAMissingMethod() {
		assertFalse(MixinHooks.hooked(target("renamedInANewVersion", "handler$zza000$secretblocks$secretFaces"),
				"shouldDrawSide", "secretFaces"));
	}
}
