package net.calibermc.secretblocks.registry;

import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.blocks.*;
import net.calibermc.secretblocks.items.SecretBlockItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;

public class ModBlocks {

    // Blocks
    public static final Block SOLID_BLOCK = registerBlock("solid_block",
            new SolidBlock(FabricBlockSettings.copy(Blocks.WHITE_WOOL)), SecretBlocks.SECRET_BLOCKS_GROUP);

    public static final Block STAIR_BLOCK = registerBlock("stair_block",
            new StairBlock(SOLID_BLOCK.defaultBlockState(), FabricBlockSettings.copy(SOLID_BLOCK)), SecretBlocks.SECRET_BLOCKS_GROUP);

    public static final Block SLAB_BLOCK = registerBlock("slab_block",
            new SlabBlock(FabricBlockSettings.copy(SOLID_BLOCK)), SecretBlocks.SECRET_BLOCKS_GROUP);

    public static final Block GHOST_BLOCK = registerBlock("ghost_block",
            new GhostBlock(FabricBlockSettings.copy(SOLID_BLOCK).noCollission()), SecretBlocks.SECRET_BLOCKS_GROUP);

    public static final Block ONE_WAY_GLASS = registerBlock("one_way_glass",
            new OneWayGlassBlock(FabricBlockSettings.copy(SOLID_BLOCK).noOcclusion()), SecretBlocks.SECRET_BLOCKS_GROUP);

    public static final Block DOOR_BLOCK = registerBlock("door_block",
            new DoorBlock(FabricBlockSettings.copy(SOLID_BLOCK)), SecretBlocks.SECRET_BLOCKS_GROUP);

    // Iron door and trapdoor: vanilla iron settings. Metal material is what
    // makes 1.19.2's DoorBlock/TrapDoorBlock refuse hand use and play the iron
    // sounds; hardness 5, pickaxe (mineable/pickaxe tag) to drop.
    // Our DoorBlock/TrapdoorBlock classes keep the disguised panel rendering
    // (the unused IronDoorBlock extends vanilla's door and would draw a full cube).
    public static final Block IRON_DOOR_BLOCK = registerBlock("iron_door_block",
            new DoorBlock(FabricBlockSettings.copy(Blocks.IRON_DOOR)), SecretBlocks.SECRET_BLOCKS_GROUP);

    public static final Block TRAPDOOR_BLOCK = registerBlock("trapdoor_block",
            new TrapdoorBlock(FabricBlockSettings.copy(SOLID_BLOCK)), SecretBlocks.SECRET_BLOCKS_GROUP);

    public static final Block IRON_TRAPDOOR_BLOCK = registerBlock("iron_trapdoor_block",
            new TrapdoorBlock(FabricBlockSettings.copy(Blocks.IRON_TRAPDOOR)), SecretBlocks.SECRET_BLOCKS_GROUP);

    public static final Block WOODEN_BUTTON = registerBlock("wooden_button",
            new WoodButton(FabricBlockSettings.copy(SOLID_BLOCK)), SecretBlocks.SECRET_BLOCKS_GROUP);

    public static final Block STONE_BUTTON = registerBlock("stone_button",
            new StoneButton(FabricBlockSettings.copy(SOLID_BLOCK)), SecretBlocks.SECRET_BLOCKS_GROUP);

    public static final Block SECRET_LEVER = registerBlock("secret_lever",
            new SecretLever(FabricBlockSettings.copy(SOLID_BLOCK)), SecretBlocks.SECRET_BLOCKS_GROUP);

    // A climbable Ghost Block: climbable through the vanilla minecraft:climbable
    // block tag (data/minecraft/tags/blocks/climbable.json), like the original
    // Secret Rooms Mod's Secret Clamber.
    public static final Block SECRET_CLAMBER = registerBlock("secret_clamber",
            new GhostBlock(FabricBlockSettings.copy(SOLID_BLOCK).noCollission()), SecretBlocks.SECRET_BLOCKS_GROUP);

//    public static final Block SECRET_LADDER = registerBlock("secret_ladder",
//            new SecretLadder(SOLID_BLOCK.getDefaultState(), FabricBlockSettings.copy(SOLID_BLOCK)), SecretBlocks.SECRET_BLOCKS_GROUP);

    public static final Block SECRET_OBSERVER = registerBlock("secret_observer",
            new SecretObserver(FabricBlockSettings.copy(SOLID_BLOCK)), SecretBlocks.SECRET_BLOCKS_GROUP);

    // Has its own block entity type (a vanilla dispenser block entity), see ModEntities.
    public static final Block SECRET_DISPENSER = registerBlock("secret_dispenser",
            new SecretDispenser(FabricBlockSettings.copy(SOLID_BLOCK)), SecretBlocks.SECRET_BLOCKS_GROUP);

    // Shares the Secret Dispenser's block entity type, see ModEntities.
    public static final Block SECRET_DROPPER = registerBlock("secret_dropper",
            new SecretDropper(FabricBlockSettings.copy(SOLID_BLOCK)), SecretBlocks.SECRET_BLOCKS_GROUP);

    // Has its own block entity type (a vanilla sculk sensor block entity), see ModEntities.
    // Light 1 as vanilla's; not a conductor.
    public static final Block SECRET_SCULK_SENSOR = registerBlock("secret_sculk_sensor",
            new SecretSculkSensor(FabricBlockSettings.copy(SOLID_BLOCK).lightLevel(state -> 1)
                    .isRedstoneConductor((state, level, pos) -> false)),
            SecretBlocks.SECRET_BLOCKS_GROUP);

    // Has its own block entity type (a vanilla hopper block entity), see ModEntities.
    // As tough as a vanilla hopper: hardness 3, blast resistance 4.8, needs a
    // pickaxe to drop (data/minecraft/tags/blocks/mineable/pickaxe.json),
    // metal sound. Still a full, occluding block, unlike the vanilla funnel.
    public static final Block SECRET_HOPPER = registerBlock("secret_hopper",
            new SecretHopper(FabricBlockSettings.copy(SOLID_BLOCK).strength(3.0f, 4.8f).requiresCorrectToolForDrops().sound(SoundType.METAL)),
            SecretBlocks.SECRET_BLOCKS_GROUP);

    public static final Block PRESSURE_PLATE = registerBlock("pressure_plate",
            new PressurePlate(FabricBlockSettings.copy(SOLID_BLOCK), entity -> true), SecretBlocks.SECRET_BLOCKS_GROUP);

    public static final Block PLAYER_PRESSURE_PLATE = registerBlock("player_pressure_plate",
            new PressurePlate(FabricBlockSettings.copy(SOLID_BLOCK), entity -> entity instanceof Player), SecretBlocks.SECRET_BLOCKS_GROUP);

    // A full block, but not a redstone conductor, like vanilla wire.
    public static final Block SECRET_REDSTONE = registerBlock("secret_redstone",
            new SecretRedstone(FabricBlockSettings.copy(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false)),
            SecretBlocks.SECRET_BLOCKS_GROUP);

    // The original mod's six-sided wire; not a conductor.
    public static final Block SECRET_REDSTONE_WIRE = registerBlock("secret_redstone_wire",
            new SecretRedstoneWire(FabricBlockSettings.copy(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false)),
            SecretBlocks.SECRET_BLOCKS_GROUP);

    // A vanilla repeater in a full disguised block; not a conductor.
    public static final Block SECRET_REPEATER = registerBlock("secret_repeater",
            new SecretRepeater(FabricBlockSettings.copy(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false)),
            SecretBlocks.SECRET_BLOCKS_GROUP);

    // A vanilla comparator in a full disguised block; not a conductor.
    public static final Block SECRET_COMPARATOR = registerBlock("secret_comparator",
            new SecretComparator(FabricBlockSettings.copy(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false)),
            SecretBlocks.SECRET_BLOCKS_GROUP);

    public static final Block SECRET_CHEST = registerBlock("secret_chest",
            new SecretChest(FabricBlockSettings.copy(SOLID_BLOCK)), SecretBlocks.SECRET_BLOCKS_GROUP);

    // A Secret Chest that signals how many players look into it; not a conductor, as vanilla.
    public static final Block SECRET_TRAPPED_CHEST = registerBlock("secret_chest_trapped",
            new SecretTrappedChest(FabricBlockSettings.copy(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false)),
            SecretBlocks.SECRET_BLOCKS_GROUP);


    // A daylight detector in a full disguised block; not a conductor.
    public static final Block SECRET_DAYLIGHT_DETECTOR = registerBlock("secret_daylight_detector",
            new SecretDaylightDetector(FabricBlockSettings.copy(SOLID_BLOCK).isRedstoneConductor((state, level, pos) -> false)),
            SecretBlocks.SECRET_BLOCKS_GROUP);

    // Torch Lever: not a secret block. A vanilla torch, floor and wall, that toggles like a
    // lever. One item places both, as with vanilla's torch (the exception to registerBlock's one item per
    // block); the wall block drops what the floor block drops.
    public static final Block TORCH_LEVER = registerBlockWithoutBlockItem("torch_lever",
            new TorchLever(FabricBlockSettings.copy(Blocks.TORCH)));

    // Builds a line of segments while open. The shared block entity type; a conductor, wool-like.
    public static final Block SECRET_GATE = registerBlock("secret_gate",
            new SecretGate(FabricBlockSettings.copy(SOLID_BLOCK)), SecretBlocks.SECRET_BLOCKS_GROUP);

    // Pistons: vanilla toughness and sound, not conductors (as vanilla). The head has no item and drops nothing.
    public static final Block SECRET_PISTON = registerBlock("secret_piston",
            new SecretPiston(false, FabricBlockSettings.copy(SOLID_BLOCK).strength(1.5F).sound(SoundType.STONE).isRedstoneConductor((state, level, pos) -> false)),
            SecretBlocks.SECRET_BLOCKS_GROUP);
    public static final Block SECRET_STICKY_PISTON = registerBlock("secret_sticky_piston",
            new SecretPiston(true, FabricBlockSettings.copy(SOLID_BLOCK).strength(1.5F).sound(SoundType.STONE).isRedstoneConductor((state, level, pos) -> false)),
            SecretBlocks.SECRET_BLOCKS_GROUP);
    public static final Block SECRET_PISTON_HEAD = registerBlockWithoutBlockItem("secret_piston_head",
            new SecretPistonHead(FabricBlockSettings.copy(SOLID_BLOCK).strength(1.5F).sound(SoundType.STONE).isRedstoneConductor((state, level, pos) -> false).noLootTable()));

    // No item: a Secret Gate builds it. Drops nothing; wool-like as the Dummy Block.
    public static final Block SECRET_GATE_SEGMENT = registerBlockWithoutBlockItem("secret_gate_segment",
            new SecretGateSegment(FabricBlockSettings.copy(SOLID_BLOCK).noLootTable()));

    public static final Block WALL_TORCH_LEVER = registerBlockWithoutBlockItem("wall_torch_lever",
            new WallTorchLever(FabricBlockSettings.copy(Blocks.WALL_TORCH).dropsLike(TORCH_LEVER)));

    public static final Item TORCH_LEVER_ITEM = Registry.register(Registry.ITEM, new ResourceLocation(SecretBlocks.MOD_ID, "torch_lever"),
            new StandingAndWallBlockItem(TORCH_LEVER, WALL_TORCH_LEVER, new FabricItemSettings().tab(SecretBlocks.SECRET_BLOCKS_GROUP)));

    // Render Assignment
    public static final Block[] secretBlocksList = {
            SOLID_BLOCK, STAIR_BLOCK, SLAB_BLOCK, GHOST_BLOCK, ONE_WAY_GLASS, DOOR_BLOCK,
            IRON_DOOR_BLOCK, TRAPDOOR_BLOCK, IRON_TRAPDOOR_BLOCK, WOODEN_BUTTON, STONE_BUTTON,
            SECRET_LEVER, SECRET_REDSTONE, SECRET_CHEST, PRESSURE_PLATE, PLAYER_PRESSURE_PLATE, SECRET_OBSERVER, SECRET_CLAMBER, SECRET_DISPENSER, SECRET_HOPPER, SECRET_REDSTONE_WIRE, SECRET_REPEATER, SECRET_COMPARATOR, SECRET_TRAPPED_CHEST, SECRET_DAYLIGHT_DETECTOR, SECRET_DROPPER, SECRET_SCULK_SENSOR, SECRET_GATE_SEGMENT, SECRET_GATE, SECRET_PISTON, SECRET_STICKY_PISTON, SECRET_PISTON_HEAD

    };

    //  DISABLED BLOCKS ^ SECRET_LADDER
    //	public static final Block[] glassBlocksList = {  };  // For future use

    // Registry
    private static Block registerBlockWithoutBlockItem(String name, Block block) {
        return Registry.register(Registry.BLOCK, new ResourceLocation(SecretBlocks.MOD_ID, name), block);
    }

    private static Block registerBlock(String name, Block block, CreativeModeTab group) {
        registerBlockItem(name, block, group);
        return Registry.register(Registry.BLOCK, new ResourceLocation(SecretBlocks.MOD_ID, name), block);
    }

    private static Item registerBlockItem(String name, Block block, CreativeModeTab group) {
        return Registry.register(Registry.ITEM, new ResourceLocation(SecretBlocks.MOD_ID, name),
                new SecretBlockItem(block, new FabricItemSettings().tab(group)));
    }

    // Console Output
    public static void registerBlocks() {
        System.out.println("Registering Blocks for " + SecretBlocks.MOD_ID);
    }


}

