# Changelog

## 2.0.0 (Minecraft 1.19.2, Fabric) — beta

The biggest update yet: a full set of secret redstone, containers, pistons and
gates, new tools, and secret blocks that look, sound and behave like the
block they copy. Every block is described in the
[wiki](https://github.com/calibergames/secret-blocks/wiki).

### New blocks

- **Pressure plates:** Secret Pressure Plate, Secret Player Pressure Plate,
  and Secret Light and Heavy Weighted Pressure Plates. Each is a full floor
  block that works as a pressure plate.
- **Redstone:**
  - Secret Redstone Wire, which carries a signal on all six sides;
  - Secret Repeater and Secret Comparator;
  - Secret Observer;
  - Secret Daylight Detector, with day and night modes;
  - Secret Sculk Sensor;
  - Torch Lever.
- **Containers:** Secret Trapped Chest, Secret Dispenser, Secret Dropper and
  Secret Hopper.
- **Pistons and gates:**
  - Secret Piston and Secret Sticky Piston;
  - Secret Gate, which builds a line of disguised blocks while powered
    and takes it away again.
- **Secret Clamber:** a wall you walk into and climb down slowly. It
  replaces the unfinished Secret Ladder.

### New tools

- **Builder's Goggles:** see the real block behind every disguise.
  - A key (B by default) switches the view.
  - Get them from the creative inventory or `/give`.
- **Camouflage Wand:** turns vanilla blocks into secret blocks and back.
  Creative mode or operators only.

### Changes

- **Picking a look:** a secret block copies the block you click to place
  it:
  - glass, stained glass and leaves count as full blocks;
  - partial blocks give stone;
  - clicking another secret block copies the face you clicked.

  See [How disguises work](https://github.com/calibergames/secret-blocks/wiki/How-Disguises-Work).
- **One Way Glass:** sneak while placing to put the glass on the top or
  bottom.
- **Like their look:** secret blocks sound like the block they look like,
  mine like it (tools speed them up as they would), and stand up to
  explosions like it.
  - They always drop themselves when mined.
  - Explosions drop them as vanilla drops blocks.
- **Pistons** push and pull secret blocks, which keep their disguise.
- **Light:** passes through see-through looks such as glass and leaves.
- **Drawing:** each face is drawn with its look's own model.
  - Connected textures work, with Continuity 2.0.x.
  - Leaves follow the Fast and Fancy setting.
  - Stained glass is translucent.
  - Grass gets its biome colour.
- **Secret Iron Door and Secret Iron Trapdoor:** only redstone opens them,
  as for vanilla iron doors.
- **Secret Redstone Dust:** powers the blocks beside it as vanilla dust
  does.
- **Switch Probe and Rotator Probe:** their changes are saved with the
  world.
- **Secret Sculk Sensor:** gives off no light, so it cannot be found in
  the dark.
- **Recipes:**
  - the Secret Chest, Secret Lever, Secret Redstone Dust and Secret Stone
    Button can now be crafted;
  - polished blackstone buttons and pressure plates work in place of
    stone ones.

### Compatibility

- **Requires Minecraft 1.19.2 exactly,** Fabric Loader 0.14.21 or later,
  and Fabric API.
- **Sodium needs Indium.** Without it, the game stops at launch with a
  message saying so.
- **Works with:**
  - Iris shader packs;
  - Continuity connected textures;
  - Carpet.

  See [Compatibility](https://github.com/calibergames/secret-blocks/wiki/Compatibility).

### Worlds from 1.2.0

- Worlds made with 1.2.0 load in 2.0.0. Secret blocks keep their disguises,
  and Secret Chests keep their contents.
- In 1.2.0, hoppers and droppers could push items into plain secret
  blocks, where they were lost. Those items now drop when the block is
  broken.
