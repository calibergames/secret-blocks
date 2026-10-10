package net.calibermc.secretblocks.registry;

import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.blocks.*;
import net.calibermc.secretblocks.items.SecretBlockItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class ModBlocks {

    // Blocks
    // Wool settings: drops without a tool. How long a secret block takes to
    // mine, how it sounds and how it stands up to a blast follow its look
    // (blocks/DisguiseLook), not these settings.
    public static final Block SOLID_BLOCK = registerBlock("solid_block",
            new SolidBlock(FabricBlockSettings.copyOf(Blocks.WHITE_WOOL)));

    public static final Block STAIR_BLOCK = registerBlock("stair_block",
            new StairBlock(SOLID_BLOCK.defaultBlockState(), FabricBlockSettings.copyOf(SOLID_BLOCK)));

    public static final Block SLAB_BLOCK = registerBlock("slab_block",
            new SlabBlock(FabricBlockSettings.copyOf(SOLID_BLOCK)));

    public static final Block GHOST_BLOCK = registerBlock("ghost_block",
            new GhostBlock(FabricBlockSettings.copyOf(SOLID_BLOCK).noCollission()));

    public static final Block ONE_WAY_GLASS = registerBlock("one_way_glass",
            new OneWayGlassBlock(FabricBlockSettings.copyOf(SOLID_BLOCK).noOcclusion()));

    // Doors break when a piston pushes them, as vanilla's.
    public static final Block DOOR_BLOCK = registerBlock("door_block",
            new DoorBlock(FabricBlockSettings.copyOf(SOLID_BLOCK).pushReaction(PushReaction.DESTROY), BlockSetType.OAK));

    // Iron door and trapdoor: vanilla iron settings but for the tool needed to
    // drop (a secret block always drops; mining time, sounds and blast
    // resistance follow its look). The iron block set type is what makes
    // DoorBlock/TrapDoorBlock refuse hand use and play the iron door sounds.
    // Our DoorBlock/TrapdoorBlock classes keep the disguised panel rendering
    // (the unused IronDoorBlock extends vanilla's door and would draw a full cube).
    public static final Block IRON_DOOR_BLOCK = registerBlock("iron_door_block",
            new DoorBlock(FabricBlockSettings.create().mapColor(MapColor.METAL).strength(5.0F).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.DESTROY), BlockSetType.IRON));

    public static final Block TRAPDOOR_BLOCK = registerBlock("trapdoor_block",
            new TrapdoorBlock(FabricBlockSettings.copyOf(SOLID_BLOCK), BlockSetType.OAK));

    public static final Block IRON_TRAPDOOR_BLOCK = registerBlock("iron_trapdoor_block",
            new TrapdoorBlock(FabricBlockSettings.create().mapColor(MapColor.METAL).strength(5.0F).sound(SoundType.METAL).noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false), BlockSetType.IRON));

    // Buttons and the lever are not conductors, like the vanilla ones they stand in for.
    public static final Block WOODEN_BUTTON = registerBlock("wooden_button",
            new WoodButton(FabricBlockSettings.copyOf(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false)));

    public static final Block STONE_BUTTON = registerBlock("stone_button",
            new StoneButton(FabricBlockSettings.copyOf(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false)));

    public static final Block SECRET_LEVER = registerBlock("secret_lever",
            new SecretLever(FabricBlockSettings.copyOf(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false)));

    // A climbable Ghost Block: climbable through the vanilla minecraft:climbable
    // block tag (data/minecraft/tags/blocks/climbable.json), like the original
    // Secret Rooms Mod's Secret Clamber.
    public static final Block SECRET_CLAMBER = registerBlock("secret_clamber",
            new GhostBlock(FabricBlockSettings.copyOf(SOLID_BLOCK).noCollission()));

//    public static final Block SECRET_LADDER = registerBlock("secret_ladder",
//            new SecretLadder(SOLID_BLOCK.getDefaultState(), FabricBlockSettings.copyOf(SOLID_BLOCK)));

    // Not a conductor, as vanilla's observer.
    public static final Block SECRET_OBSERVER = registerBlock("secret_observer",
            new SecretObserver(FabricBlockSettings.copyOf(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false)));

    // Has its own block entity type (a vanilla dispenser block entity), see ModEntities.
    public static final Block SECRET_DISPENSER = registerBlock("secret_dispenser",
            new SecretDispenser(FabricBlockSettings.copyOf(SOLID_BLOCK)));

    // Shares the Secret Dispenser's block entity type, see ModEntities.
    public static final Block SECRET_DROPPER = registerBlock("secret_dropper",
            new SecretDropper(FabricBlockSettings.copyOf(SOLID_BLOCK)));

    // Has its own block entity type (a vanilla sculk sensor block entity), see ModEntities.
    // No light (vanilla's sensor gives 1, which would show where it is hidden); not a conductor.
    public static final Block SECRET_SCULK_SENSOR = registerBlock("secret_sculk_sensor",
            new SecretSculkSensor(FabricBlockSettings.copyOf(SOLID_BLOCK).lightLevel(state -> 0)
                    .isRedstoneConductor((state, level, pos) -> false)));

    // Has its own block entity type (a vanilla hopper block entity), see ModEntities.
    // A full, occluding block, unlike the vanilla funnel, but not a conductor,
    // as vanilla's.
    public static final Block SECRET_HOPPER = registerBlock("secret_hopper",
            new SecretHopper(FabricBlockSettings.copyOf(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false)));

    // Not conductors, like the vanilla plates they stand in for.
    public static final Block PRESSURE_PLATE = registerBlock("pressure_plate",
            new PressurePlate(FabricBlockSettings.copyOf(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false), entity -> true));

    public static final Block PLAYER_PRESSURE_PLATE = registerBlock("player_pressure_plate",
            new PressurePlate(FabricBlockSettings.copyOf(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false),
                    entity -> entity instanceof Player));

    // Vanilla's weighted plates: full signal at 15 entities (light) or 150 (heavy).
    // Not conductors either.
    public static final Block LIGHT_WEIGHTED_PRESSURE_PLATE = registerBlock("light_weighted_pressure_plate",
            new WeightedPressurePlate(FabricBlockSettings.copyOf(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false), 15));

    public static final Block HEAVY_WEIGHTED_PRESSURE_PLATE = registerBlock("heavy_weighted_pressure_plate",
            new WeightedPressurePlate(FabricBlockSettings.copyOf(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false), 150));

    // A full block, but not a redstone conductor, like vanilla wire.
    public static final Block SECRET_REDSTONE = registerBlock("secret_redstone",
            new SecretRedstone(FabricBlockSettings.copyOf(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false)));

    // The original mod's six-sided wire; not a conductor.
    public static final Block SECRET_REDSTONE_WIRE = registerBlock("secret_redstone_wire",
            new SecretRedstoneWire(FabricBlockSettings.copyOf(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false)));

    // A vanilla repeater in a full disguised block; not a conductor.
    public static final Block SECRET_REPEATER = registerBlock("secret_repeater",
            new SecretRepeater(FabricBlockSettings.copyOf(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false)));

    // A vanilla comparator in a full disguised block; not a conductor.
    public static final Block SECRET_COMPARATOR = registerBlock("secret_comparator",
            new SecretComparator(FabricBlockSettings.copyOf(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false)));

    // Not a conductor, as vanilla's chest.
    public static final Block SECRET_CHEST = registerBlock("secret_chest",
            new SecretChest(FabricBlockSettings.copyOf(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false)));

    // A Secret Chest that signals how many players look into it; not a conductor, as vanilla.
    public static final Block SECRET_TRAPPED_CHEST = registerBlock("secret_chest_trapped",
            new SecretTrappedChest(FabricBlockSettings.copyOf(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false)));


    // A daylight detector in a full disguised block; not a conductor.
    public static final Block SECRET_DAYLIGHT_DETECTOR = registerBlock("secret_daylight_detector",
            new SecretDaylightDetector(FabricBlockSettings.copyOf(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false)));

    // Torch Lever: not a secret block. A vanilla torch, floor and wall, that toggles like a
    // lever. One item places both, as with vanilla's torch (the exception to registerBlock's one item per
    // block); the wall block drops what the floor block drops.
    public static final Block TORCH_LEVER = registerBlockWithoutBlockItem("torch_lever",
            new TorchLever(FabricBlockSettings.copyOf(Blocks.TORCH)));

    // Builds a line of segments while open. The shared block entity type; wool-like.
    // The gate and its segments are not conductors, like the other redstone blocks.
    public static final Block SECRET_GATE = registerBlock("secret_gate",
            new SecretGate(FabricBlockSettings.copyOf(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false)));

    // Pistons: not conductors (as vanilla). The head has no item and drops nothing.
    public static final Block SECRET_PISTON = registerBlock("secret_piston",
            new SecretPiston(false, FabricBlockSettings.copyOf(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false)));
    public static final Block SECRET_STICKY_PISTON = registerBlock("secret_sticky_piston",
            new SecretPiston(true, FabricBlockSettings.copyOf(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false)));
    public static final Block SECRET_PISTON_HEAD = registerBlockWithoutBlockItem("secret_piston_head",
            new SecretPistonHead(FabricBlockSettings.copyOf(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false).noLootTable()));

    // No item: a Secret Gate builds it. Drops nothing; wool-like as the Dummy Block.
    public static final Block SECRET_GATE_SEGMENT = registerBlockWithoutBlockItem("secret_gate_segment",
            new SecretGateSegment(FabricBlockSettings.copyOf(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false).noLootTable()));

    public static final Block WALL_TORCH_LEVER = registerBlockWithoutBlockItem("wall_torch_lever",
            new WallTorchLever(FabricBlockSettings.copyOf(Blocks.WALL_TORCH).dropsLike(TORCH_LEVER)));

    public static final Item TORCH_LEVER_ITEM = ModCreativeTab.add(Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(SecretBlocks.MOD_ID, "torch_lever"),
            new StandingAndWallBlockItem(TORCH_LEVER, WALL_TORCH_LEVER, new FabricItemSettings(), Direction.DOWN)));

    // Render Assignment
    public static final Block[] secretBlocksList = {
            SOLID_BLOCK, STAIR_BLOCK, SLAB_BLOCK, GHOST_BLOCK, ONE_WAY_GLASS, DOOR_BLOCK,
            IRON_DOOR_BLOCK, TRAPDOOR_BLOCK, IRON_TRAPDOOR_BLOCK, WOODEN_BUTTON, STONE_BUTTON,
            SECRET_LEVER, SECRET_REDSTONE, SECRET_CHEST, PRESSURE_PLATE, PLAYER_PRESSURE_PLATE, LIGHT_WEIGHTED_PRESSURE_PLATE, HEAVY_WEIGHTED_PRESSURE_PLATE, SECRET_OBSERVER, SECRET_CLAMBER, SECRET_DISPENSER, SECRET_HOPPER, SECRET_REDSTONE_WIRE, SECRET_REPEATER, SECRET_COMPARATOR, SECRET_TRAPPED_CHEST, SECRET_DAYLIGHT_DETECTOR, SECRET_DROPPER, SECRET_SCULK_SENSOR, SECRET_GATE_SEGMENT, SECRET_GATE, SECRET_PISTON, SECRET_STICKY_PISTON, SECRET_PISTON_HEAD

    };

    //  DISABLED BLOCKS ^ SECRET_LADDER
    //	public static final Block[] glassBlocksList = {  };  // For future use

    // Registry
    private static Block registerBlockWithoutBlockItem(String name, Block block) {
        return Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(SecretBlocks.MOD_ID, name), block);
    }

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(SecretBlocks.MOD_ID, name), block);
    }

    private static Item registerBlockItem(String name, Block block) {
        return ModCreativeTab.add(Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(SecretBlocks.MOD_ID, name),
                new SecretBlockItem(block, new FabricItemSettings())));
    }

    // Console Output
    public static void registerBlocks() {
        System.out.println("Registering Blocks for " + SecretBlocks.MOD_ID);
    }


}

