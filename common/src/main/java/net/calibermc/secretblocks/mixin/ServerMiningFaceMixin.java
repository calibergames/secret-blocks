package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.DisguiseLook;
import net.calibermc.secretblocks.blocks.MiningFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The server keeps the face named in the dig action that starts mining a block. */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerMiningFaceMixin {

	@Shadow
	@Final
	protected ServerPlayer player;

	/**
	 * Where a start is accepted (past the reach and permission checks: a
	 * creative break, or survival mining), so a refused one keeps the face. A face the player's eyes are
	 * not in front of cannot be the one being hit (a modified client could
	 * name the face with the quickest look): the face toward them counts.
	 */
	@Inject(method = "handleBlockBreakAction", at = {
			@At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/server/level/ServerPlayerGameMode;destroyAndAck(Lnet/minecraft/core/BlockPos;ILjava/lang/String;)V"),
			@At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerPlayerGameMode;destroyProgressStart:I", opcode = Opcodes.PUTFIELD)})
	private void secretblocks$keepFace(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction face,
			int maxBuildHeight, int sequence, CallbackInfo ci) {
		Vec3 eye = player.getEyePosition();
		((MiningFace) player).secretblocks$setMiningFace(DisguiseLook.faces(pos, face, eye) ? face : DisguiseLook.toward(pos, eye));
	}
}
