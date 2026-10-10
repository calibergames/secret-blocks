package net.calibermc.secretblocks.gametest.driver;

import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Development-only command-file test driver (never shipped; part of the
 * game-test mod). Lets test scripts act as the player through the real
 * client code ({@code MultiPlayerGameMode}), with no keyboard or mouse, and
 * report what the client sees.
 *
 * Active only when started with {@code -Dsecretblocks.driver=<dir>} (the
 * {@code runDevClient} Gradle task; the game itself runs in {@code run/}, like
 * {@code runClient}). Each client tick it takes the commands in
 * {@code <dir>/cmd.txt} (then deletes the file; write it atomically, e.g. to a
 * temporary file renamed into place) and appends one result line
 * per command to {@code <dir>/out.txt}:
 * <ul>
 *   <li>{@code break x y z [face]}: hold "attack" on that face of the block
 *   (default the top) until it is gone on the client (survival mining, with
 *   the chips the attack key makes, or an instant creative break);</li>
 *   <li>{@code record on|off}: start or stop recording the sounds the client
 *   asks to play and the block particles it adds ({@link Recorder});</li>
 *   <li>{@code recorded}: report and clear what was recorded: a count line,
 *   then one line per sound and per particle sprite;</li>
 *   <li>{@code sprint on|off}: set the player sprinting (sprint dust);</li>
 *   <li>{@code size w h}: resize the window to w by h pixels (screenshots
 *   at a set size); the reply is the size before the resize, which takes
 *   effect a frame later;</li>
 *   <li>{@code use x y z face}: use the held item on that face (placement);</li>
 *   <li>{@code select n}: select hotbar slot n;</li>
 *   <li>{@code sneak on|off}: hold or release the sneak key (the server
 *   learns of it through the client's usual movement packets);</li>
 *   <li>{@code held}: report the held stack (item, NBT and tooltip) and any
 *   open screen (with its title) on the client and, in singleplayer, the
 *   built-in server's copy of the stack: two lines;</li>
 *   <li>{@code close}: close the open screen, as Escape does;</li>
 *   <li>{@code connect host:port}: at the title screen, join that server, as
 *   the multiplayer screen's Direct Connect does ({@code quit} leaves it);</li>
 *   <li>{@code sdata x y z}: in singleplayer, the built-in server's block
 *   entity data at that position (what is saved, contents included);</li>
 *   <li>{@code sitems r}: in singleplayer, the dropped items the built-in
 *   server has within r blocks of the player: their count, then each stack
 *   and where it lies;</li>
 *   <li>{@code wait n}: wait n ticks;</li>
 *   <li>{@code look x y z}: report the client's block state, block entity and
 *   disguise at that position (without creating a missing block entity);</li>
 *   <li>{@code cmd <command>}: send a slash command as the player (no leading
 *   slash);</li>
 *   <li>{@code slook x y z}: in singleplayer, report what the built-in
 *   server's world holds at that position;</li>
 *   <li>{@code camera first|back|front}: switch the camera (front = third
 *   person looking at the player);</li>
 *   <li>{@code walk dx dz}: move the player by (dx, dz) blocks with the
 *   client's own collision, 0.2 blocks per tick, and report where it ended
 *   (a block the client thinks is solid stops it);</li>
 *   <li>{@code open <save folder>}: at the title screen, open that
 *   singleplayer world (the folder name under {@code run/saves/}); the only
 *   command that runs outside a world, so no menu has to be driven;</li>
 *   <li>{@code quit}: save and quit to the title screen, as the pause menu's
 *   button does (then the client can be stopped safely);</li>
 *   <li>{@code builder on|off}: force the Builder's Goggles view on without
 *   wearing the goggles, or stop forcing it;</li>
 *   <li>{@code shot <name>}: save the game's own screenshot of the last
 *   frame as {@code run/screenshots/<name>.png} (read from the framebuffer,
 *   so it works when the desktop cannot be captured);</li>
 *   <li>{@code render <item id> <size> <name>}: draw the item as the
 *   inventory does into an off-screen buffer and save it, with a transparent
 *   background, as {@code run/icons/<name>.png}.</li>
 * </ul>
 */
public class DevClientDriver implements ClientModInitializer {

	private Path dir;
	private final Deque<String> queue = new ArrayDeque<>();
	private BlockPos breaking;
	private Direction breakFace = Direction.UP;
	private int breakTicks;
	private int waitTicks;
	private Vec3 walkStart;
	private Vec3 walkStep;
	private int walkSteps;

	@Override
	public void onInitializeClient() {
		String property = System.getProperty("secretblocks.driver");
		if (property == null) {
			return;
		}
		dir = Path.of(property);
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			throw new IllegalStateException("cannot create the driver directory " + dir, e);
		}
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
	}

	private void tick(Minecraft mc) {
		readCommands();
		if (mc.level == null) {
			openWorldFromTitle(mc);
			return;
		}
		if (mc.player == null || mc.gameMode == null) {
			return;
		}
		if (waitTicks > 0) {
			waitTicks--;
			return;
		}
		if (breaking != null) {
			continueBreaking(mc);
			return;
		}
		if (walkSteps > 0) {
			mc.player.move(net.minecraft.world.entity.MoverType.SELF, walkStep);
			if (--walkSteps == 0) {
				report("walked from " + fmt(walkStart) + " to " + fmt(mc.player.position()));
			}
			return;
		}
		String command = queue.poll();
		if (command != null) {
			// A malformed line must not crash the client and end the run.
			try {
				run(mc, command.trim().split("\\s+"));
			} catch (RuntimeException e) {
				report("error in '" + command + "': " + e);
			}
		}
	}

	/**
	 * Out of a world only {@code wait}, {@code open} and {@code connect} run
	 * (the last two once the title screen is up and resources are loaded).
	 * Any other command at the head of the queue waits there (and holds up the
	 * rest) until a world is open.
	 */
	private void openWorldFromTitle(Minecraft mc) {
		if (waitTicks > 0) {
			waitTicks--;
			return;
		}
		String next = queue.peek();
		if (next != null && next.startsWith("wait ")) {
			queue.poll();
			waitTicks = Integer.parseInt(next.trim().split("\\s+")[1]);
			return;
		}
		if (next == null || !(next.startsWith("open ") || next.startsWith("connect ")) || mc.getOverlay() != null
				|| !(mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen)) {
			return;
		}
		queue.poll();
		if (next.startsWith("connect ")) {
			String host = next.substring("connect ".length()).trim();
			// A malformed address must not crash the client and end the run.
			try {
				report("connecting to " + host);
				net.minecraft.client.gui.screens.ConnectScreen.startConnecting(mc.screen, mc,
						net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(host),
						new net.minecraft.client.multiplayer.ServerData("Dedicated", host, false));
			} catch (RuntimeException e) {
				report("error in '" + next + "': " + e);
			}
			return;
		}
		String folder = next.substring("open ".length()).trim();
		report("opening " + folder);
		mc.createWorldOpenFlows().loadLevel(mc.screen, folder);
	}

	private void readCommands() {
		Path cmd = dir.resolve("cmd.txt");
		if (!Files.exists(cmd)) {
			return;
		}
		try {
			List<String> lines = Files.readAllLines(cmd, StandardCharsets.UTF_8);
			Files.delete(cmd);
			lines.stream().filter(line -> !line.isBlank()).forEach(queue::add);
		} catch (IOException e) {
			report("error reading commands: " + e);
		}
	}

	private void run(Minecraft mc, String[] a) {
		switch (a[0]) {
			case "break" -> {
				breaking = pos(a, 1);
				breakFace = a.length > 4 ? Direction.byName(a[4]) : Direction.UP;
				breakTicks = 0;
				boolean started = mc.gameMode.startDestroyBlock(breaking, breakFace);
				report("break " + breaking.toShortString() + " started=" + started);
			}
			case "use" -> {
				BlockPos pos = pos(a, 1);
				Direction face = Direction.byName(a[4]);
				Vec3 hit = Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5));
				var result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
				report("use " + pos.toShortString() + " " + face + " -> " + result);
			}
			case "record" -> {
				Recorder.setOn(a[1].equals("on"));
				Recorder.drain();
				report("record " + a[1]);
			}
			case "recorded" -> {
				List<String> lines = Recorder.drain();
				report("recorded " + lines.size());
				lines.forEach(this::report);
			}
			case "size" -> {
				mc.getWindow().setWindowed(Integer.parseInt(a[1]), Integer.parseInt(a[2]));
				report("size " + mc.getWindow().getWidth() + "x" + mc.getWindow().getHeight());
			}
			case "sprint" -> {
				mc.player.setSprinting(a[1].equals("on"));
				report("sprint " + a[1]);
			}
			case "select" -> {
				mc.player.getInventory().selected = Integer.parseInt(a[1]);
				report("select " + a[1]);
			}
			case "sneak" -> {
				mc.options.keyShift.setDown(a[1].equals("on"));
				report("sneak " + a[1]);
			}
			case "held" -> {
				var stack = mc.player.getMainHandItem();
				var tooltip = stack.getTooltipLines(mc.player, net.minecraft.world.item.TooltipFlag.Default.NORMAL)
						.stream().map(net.minecraft.network.chat.Component::getString).toList();
				report("held client " + Registry.ITEM.getKey(stack.getItem()) + " " + stack.getTag() + " tooltip=" + tooltip
						+ " screen=" + (mc.screen == null ? "none" : mc.screen.getClass().getSimpleName() + " title=" + mc.screen.getTitle().getString()));
				var server = mc.getSingleplayerServer();
				var uuid = mc.player.getUUID();
				if (server == null) {
					report("held server none");
				} else {
					server.execute(() -> {
						var player = server.getPlayerList().getPlayer(uuid);
						report("held server " + (player == null ? "no player" : player.getMainHandItem().getTag()));
					});
				}
			}
			case "close" -> {
				mc.player.closeContainer();
				report("close");
			}
			case "sdata" -> {
				BlockPos pos = pos(a, 1);
				var server = mc.getSingleplayerServer();
				if (server == null) {
					report("sdata none");
				} else {
					var level = server.getLevel(mc.level.dimension());
					server.execute(() -> {
						BlockEntity entity = level.getBlockEntity(pos);
						report("sdata " + pos.toShortString() + " " + (entity == null ? "none" : entity.saveWithoutMetadata()));
					});
				}
			}
			case "sitems" -> {
				double r = Double.parseDouble(a[1]);
				var server = mc.getSingleplayerServer();
				var uuid = mc.player.getUUID();
				if (server == null) {
					report("sitems none");
				} else {
					server.execute(() -> {
						var player = server.getPlayerList().getPlayer(uuid);
						var items = player == null ? List.<net.minecraft.world.entity.item.ItemEntity>of()
								: player.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, player.getBoundingBox().inflate(r));
						report("sitems " + items.size() + " " + items.stream().map(e -> e.getItem() + "@" + e.blockPosition().toShortString()).toList());
					});
				}
			}
			case "wait" -> waitTicks = Integer.parseInt(a[1]);
			case "look" -> report("look " + describe(mc, pos(a, 1)));
			case "cmd" -> {
				String command = String.join(" ", java.util.Arrays.copyOfRange(a, 1, a.length));
				report("cmd " + command + " -> sent=" + mc.player.commandUnsigned(command));
			}
			case "slook" -> {
				BlockPos pos = pos(a, 1);
				var server = mc.getSingleplayerServer();
				if (server == null) {
					report("slook " + pos.toShortString() + " no singleplayer server");
				} else {
					var level = server.getLevel(mc.level.dimension());
					// Read on the server thread, then report.
					server.execute(() -> report("slook " + describe(level, pos)));
				}
			}
			case "camera" -> {
				mc.options.setCameraType(switch (a[1]) {
					case "back" -> net.minecraft.client.CameraType.THIRD_PERSON_BACK;
					case "front" -> net.minecraft.client.CameraType.THIRD_PERSON_FRONT;
					default -> net.minecraft.client.CameraType.FIRST_PERSON;
				});
				report("camera " + mc.options.getCameraType());
			}
			case "builder" -> {
				if (a[1].equals("status")) {
					// Read only: whether the view is on (worn goggles or forced).
					report("builder status -> active=" + net.calibermc.secretblocks.render.BuilderView.isActive());
				} else {
					boolean active = net.calibermc.secretblocks.render.BuilderView.setForced(a[1].equals("on"));
					report("builder " + a[1] + " -> active=" + active);
				}
			}
			case "bench" -> bench(mc, Integer.parseInt(a[1]), Integer.parseInt(a[2]), a.length < 4 || !a[3].equals("noflush"));
			case "fps" -> report("fps " + mc.fpsString);
			case "saveoptions" -> {
				// Writes options.txt, which lists every key mapping the Controls screen shows.
				mc.options.save();
				report("saveoptions");
			}
			case "press" -> {
				// As a key press reaches the game: every key mapping bound to that key gets a click.
				com.mojang.blaze3d.platform.InputConstants.Key key = com.mojang.blaze3d.platform.InputConstants.getKey(a[1]);
				net.minecraft.client.KeyMapping.click(key);
				report("press " + key.getName());
			}
			case "hud" -> {
				// F1: hide the HUD and hand for clean screenshots.
				mc.options.hideGui = a[1].equals("off");
				report("hud " + a[1]);
			}
			case "shot" -> Screenshot.grab(mc.gameDirectory, a[1] + ".png", mc.getMainRenderTarget(),
					message -> report("shot " + a[1] + " -> " + message.getString()));
			case "render" -> renderIcon(mc, a[1], Integer.parseInt(a[2]), a[3]);
			case "quit" -> {
				// The pause screen's "Save and Quit to Title" (1.19.2 PauseScreen).
				boolean local = mc.isLocalServer();
				mc.level.disconnect();
				if (local) {
					mc.clearLevel(new net.minecraft.client.gui.screens.GenericDirtMessageScreen(
							net.minecraft.network.chat.Component.translatable("menu.savingLevel")));
				} else {
					mc.clearLevel();
				}
				mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
				report("quit to title");
			}
			case "walk" -> {
				double dx = Double.parseDouble(a[1]);
				double dz = Double.parseDouble(a[2]);
				walkSteps = (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dz)) / 0.2);
				walkStep = new Vec3(dx / walkSteps, 0, dz / walkSteps);
				walkStart = mc.player.position();
			}
			default -> report("unknown command " + String.join(" ", a));
		}
	}

	/**
	 * {@code bench radius rounds}: the per-frame cost of the block-entity
	 * renderer registered on the shared secret block-entity type
	 * ({@code BuilderChestRenderer}). Every secret block entity within
	 * {@code radius} of the player is sent through vanilla's dispatcher
	 * {@code rounds} times, as the level renderer does once per visible block
	 * entity per frame (distance check, light lookup, then the renderer),
	 * ending the batches after each round as a frame does ({@code noflush}
	 * as a third word leaves them, to time the renderer alone). Reports the
	 * count and nanoseconds per call. {@code fps}: the debug screen's frame
	 * rate line. {@code press <key>} (e.g. {@code key.keyboard.b}): one press
	 * of that key for the key mappings bound to it. {@code saveoptions}: write
	 * options.txt now.
	 */
	private void bench(Minecraft mc, int radius, int rounds, boolean flush) {
		List<BlockEntity> entities = new java.util.ArrayList<>();
		BlockPos centre = mc.player.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -radius, -radius), centre.offset(radius, radius, radius))) {
			BlockEntity entity = mc.level.getBlockEntity(pos);
			if (entity != null && entity.getType() == net.calibermc.secretblocks.registry.ModEntities.SECRET_BLOCK_ENTITY) {
				entities.add(entity);
			}
		}
		var dispatcher = mc.getBlockEntityRenderDispatcher();
		var poseStack = new com.mojang.blaze3d.vertex.PoseStack();
		var buffers = mc.renderBuffers().bufferSource();
		for (int warm = 0; warm < 20; warm++) {
			entities.forEach(entity -> dispatcher.render(entity, 0, poseStack, buffers));
			buffers.endBatch();
		}
		long start = System.nanoTime();
		for (int round = 0; round < rounds; round++) {
			for (BlockEntity entity : entities) {
				poseStack.pushPose();
				dispatcher.render(entity, 0, poseStack, buffers);
				poseStack.popPose();
			}
			// One frame's worth: the level renderer ends its batches every frame.
			if (flush) {
				buffers.endBatch();
			}
		}
		long elapsed = System.nanoTime() - start;
		double perCall = entities.isEmpty() ? 0 : (double) elapsed / ((long) rounds * entities.size());
		report(String.format(java.util.Locale.ROOT, "bench at %s entities=%d rounds=%d flush=%s ns_per_call=%.1f ms_per_frame=%.3f builder=%s",
				centre.toShortString(), entities.size(), rounds, flush, perCall, perCall * entities.size() / 1e6, net.calibermc.secretblocks.render.BuilderView.isActive()));
	}

	/**
	 * {@code render <item id> <size> <name>}: draw the item exactly as the
	 * inventory does (the game's own GUI item renderer, 3D blocks included)
	 * into an off-screen {@code size}×{@code size} buffer with a transparent
	 * background, saved as {@code icons/<name>.png} in the game directory.
	 * The projection and model-view are set up as {@code GameRenderer} does
	 * for the GUI, with 16 units across the whole buffer. {@code name} may
	 * hold sub-folders but must stay inside {@code icons/}; an unknown item
	 * id or a size outside 1–4096 is refused. Translucent pixels come out
	 * with premultiplied colour (one pass over a transparent background).
	 */
	private void renderIcon(Minecraft mc, String itemId, int size, String name) {
		var id = new net.minecraft.resources.ResourceLocation(itemId);
		if (!Registry.ITEM.containsKey(id)) {
			report("render " + itemId + " failed: no such item");
			return;
		}
		if (size < 1 || size > 4096) {
			report("render " + itemId + " failed: size " + size + " is outside 1-4096");
			return;
		}
		Path icons = mc.gameDirectory.toPath().resolve("icons").toAbsolutePath().normalize();
		Path out = icons.resolve(name + ".png").normalize();
		if (!out.startsWith(icons)) {
			report("render " + itemId + " failed: '" + name + "' leaves the icons folder");
			return;
		}
		var stack = new net.minecraft.world.item.ItemStack(Registry.ITEM.get(id));
		var target = new com.mojang.blaze3d.pipeline.TextureTarget(size, size, true, Minecraft.ON_OSX);
		try {
			target.setClearColor(0, 0, 0, 0);
			target.clear(Minecraft.ON_OSX);
			target.bindWrite(true);
			com.mojang.blaze3d.systems.RenderSystem.backupProjectionMatrix();
			var modelView = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
			modelView.pushPose();
			try {
				com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(
						com.mojang.math.Matrix4f.orthographic(0, 16, 0, 16, 1000, 3000));
				modelView.setIdentity();
				modelView.translate(0, 0, -2000);
				com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
				mc.getItemRenderer().renderGuiItem(stack, 0, 0);
				try (var image = new com.mojang.blaze3d.platform.NativeImage(size, size, false)) {
					com.mojang.blaze3d.systems.RenderSystem.bindTexture(target.getColorTextureId());
					image.downloadTexture(0, false);
					image.flipY();
					Files.createDirectories(out.getParent());
					image.writeToFile(out);
				}
				report("render " + itemId + " " + size + " -> " + out);
			} finally {
				// Undone only once they were set up, so a failure above cannot be masked here.
				modelView.popPose();
				com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
				com.mojang.blaze3d.systems.RenderSystem.restoreProjectionMatrix();
			}
		} catch (IOException e) {
			report("render " + itemId + " failed: " + e);
		} finally {
			target.unbindWrite();
			mc.getMainRenderTarget().bindWrite(true);
			target.destroyBuffers();
		}
	}

	private void continueBreaking(Minecraft mc) {
		breakTicks++;
		if (mc.level.getBlockState(breaking).isAir() || breakTicks > 200) {
			report("broken " + breaking.toShortString() + " after " + breakTicks + " ticks; client now " + describe(mc, breaking));
			mc.gameMode.stopDestroyBlock();
			breaking = null;
			return;
		}
		// As the attack key does: chips fly off the face being mined.
		if (mc.gameMode.continueDestroyBlock(breaking, breakFace)) {
			mc.particleEngine.crack(breaking, breakFace);
		}
		mc.player.swing(InteractionHand.MAIN_HAND);
	}

	private static String describe(Minecraft mc, BlockPos pos) {
		return describe(mc.level, pos);
	}

	private static String describe(net.minecraft.world.level.Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		// CHECK, not Level.getBlockEntity: looking must not create a missing
		// block entity (vanilla creates one on main-thread access).
		BlockEntity entity = level.getChunkAt(pos).getBlockEntity(pos, net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK);
		StringBuilder out = new StringBuilder(pos.toShortString()).append(" state=")
				.append(Registry.BLOCK.getKey(state.getBlock())).append(state.getValues().isEmpty() ? "" : state.getValues().toString())
				.append(" blockEntity=").append(entity == null ? "none" : entity.getClass().getSimpleName());
		if (entity instanceof DisguisedBlockEntity disguised) {
			out.append(" north=").append(Registry.BLOCK.getKey(disguised.getState(Direction.NORTH).getBlock()))
					.append(" up=").append(Registry.BLOCK.getKey(disguised.getState(Direction.UP).getBlock()));
		}
		return out.toString();
	}

	private static String fmt(Vec3 v) {
		return String.format(java.util.Locale.ROOT, "%.2f %.2f %.2f", v.x, v.y, v.z);
	}

	private static BlockPos pos(String[] a, int from) {
		return new BlockPos(Integer.parseInt(a[from]), Integer.parseInt(a[from + 1]), Integer.parseInt(a[from + 2]));
	}

	private void report(String line) {
		try {
			Files.writeString(dir.resolve("out.txt"), line + System.lineSeparator(), StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			// Nothing else to report to; the test script will see no output.
		}
	}
}
