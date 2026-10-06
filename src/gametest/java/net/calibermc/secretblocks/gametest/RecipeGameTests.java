package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.calibermc.secretblocks.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * Every block and item except the creative/op tools in {@code NO_RECIPE}
 * is craftable in survival, and no mod recipe or loot table fails to load.
 */
public class RecipeGameTests implements FabricGameTest {

	/**
	 * Data files kept for content that is not registered yet (the wall ladder,
	 * dropped for now). A Fabric load condition keeps them from loading, so
	 * they log no errors.
	 */
	private static final Set<String> PARKED = Set.of("secret_ladder");

	/**
	 * The allowlist of items with no recipe on purpose, each with its reason.
	 * The Rotator Probe is the Switch Probe switched by sneak-use. The
	 * Builder's Goggles and the Camouflage Wand are creative/op tools; the
	 * Secret Gate's segments have no item.
	 */
	private static final Set<Item> NO_RECIPE = Set.of(ModItems.SWITCH_PROBE_ROTATION_MODE, ModItems.BUILDERS_GOGGLES,
			ModItems.CAMOUFLAGE_WAND);

	/** A mod data file: its name without folder or extension, and whether its Fabric load conditions (if any) accept it. */
	private record DataFile(String name, boolean conditionsPass) {
	}

	/** The mod's data files under {@code folder}, read from the server's resources. */
	private static List<DataFile> dataFiles(MinecraftServer server, String folder) {
		String prefix = folder + "/";
		List<DataFile> files = new ArrayList<>();
		server.getResourceManager()
				.listResources(folder, id -> id.getNamespace().equals(SecretBlocks.MOD_ID) && id.getPath().endsWith(".json"))
				.forEach((id, resource) -> {
					String name = id.getPath().substring(prefix.length(), id.getPath().length() - ".json".length());
					try (Reader reader = resource.openAsReader()) {
						files.add(new DataFile(name, ResourceConditions.objectMatchesConditions(GsonHelper.parse(reader))));
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
		files.sort(Comparator.comparing(DataFile::name));
		return files;
	}

	/**
	 * Every file loads, except the parked ones: those must be rejected by their
	 * load condition (not merely fail to parse, which logs an error) and stay
	 * unloaded.
	 */
	private static void checkLoads(GameTestHelper h, String kind, List<DataFile> files, Predicate<String> parked, Predicate<String> loaded) {
		check(h, files.size() >= 20, "expected the mod's " + kind + " files, found " + files);
		for (DataFile file : files) {
			String name = file.name();
			if (parked.test(name)) {
				check(h, !file.conditionsPass(), kind + " " + name + " is parked: its load condition must reject it");
				check(h, !loaded.test(name), kind + " " + name + " is parked and must not load");
			} else {
				check(h, file.conditionsPass(), kind + " " + name + " is rejected by its load conditions");
				check(h, loaded.test(name), kind + " " + name + " failed to load");
			}
		}
	}

	/** Every recipe file loads, except the parked ones. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void everyRecipeLoads(GameTestHelper h) {
		MinecraftServer server = h.getLevel().getServer();
		checkLoads(h, "recipe", dataFiles(server, "recipes"), PARKED::contains,
				name -> server.getRecipeManager().byKey(new ResourceLocation(SecretBlocks.MOD_ID, name)).isPresent());
		h.succeed();
	}

	/** Every loot table file loads, except the parked ones. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void everyLootTableLoads(GameTestHelper h) {
		MinecraftServer server = h.getLevel().getServer();
		checkLoads(h, "loot table", dataFiles(server, "loot_tables"), name -> PARKED.stream().anyMatch(p -> name.equals("blocks/" + p)),
				name -> server.getLootTables().getIds().contains(new ResourceLocation(SecretBlocks.MOD_ID, name)));
		h.succeed();
	}

	/** Every registered block and item has a crafting recipe that makes it. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void everyItemHasARecipe(GameTestHelper h) {
		List<CraftingRecipe> recipes = h.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING);
		int items = 0;
		for (Item item : Registry.ITEM) {
			if (!Registry.ITEM.getKey(item).getNamespace().equals(SecretBlocks.MOD_ID) || NO_RECIPE.contains(item)) {
				continue;
			}
			items++;
			check(h, recipes.stream().anyMatch(r -> r.getResultItem().is(item)), "no crafting recipe makes " + Registry.ITEM.getKey(item));
		}
		check(h, items >= 20, "expected the mod's items, found " + items);
		h.succeed();
	}

	/**
	 * Every mod recipe crafts its result from its own ingredients in a real
	 * crafting grid (the first item each ingredient accepts; shaped recipes
	 * laid out in their pattern).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void everyRecipeCraftsFromItsIngredients(GameTestHelper h) {
		int crafted = 0;
		for (CraftingRecipe recipe : h.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
			if (!recipe.getId().getNamespace().equals(SecretBlocks.MOD_ID)) {
				continue;
			}
			crafted++;
			int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
			ItemStack[] grid = new ItemStack[9];
			Arrays.fill(grid, ItemStack.EMPTY);
			List<Ingredient> ingredients = recipe.getIngredients();
			for (int i = 0; i < ingredients.size(); i++) {
				ItemStack[] accepted = ingredients.get(i).getItems();
				check(h, ingredients.get(i).isEmpty() || accepted.length > 0, recipe.getId() + " ingredient " + i + " accepts no item");
				grid[(i / width) * 3 + i % width] = accepted.length > 0 ? accepted[0].copy() : ItemStack.EMPTY;
			}
			Optional<ItemStack> made = craft(h, grid);
			check(h, made.isPresent() && made.get().is(recipe.getResultItem().getItem()),
					recipe.getId() + " should make " + recipe.getResultItem() + " from its ingredients, got " + made);
		}
		check(h, crafted >= 20, "expected the mod's crafting recipes, found " + crafted);
		h.succeed();
	}

	/** The four fixed recipes (chest, lever, redstone, stone button), with the vanilla items a player would use. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void fixedRecipes(GameTestHelper h) {
		Item dummy = ModBlocks.SOLID_BLOCK.asItem();
		record Case(Item ingredient, Block expected) {
		}
		for (Case c : List.of(
				new Case(Items.CHEST, ModBlocks.SECRET_CHEST),
				new Case(Items.LEVER, ModBlocks.SECRET_LEVER),
				new Case(Items.REDSTONE, ModBlocks.SECRET_REDSTONE),
				new Case(Items.STONE_BUTTON, ModBlocks.STONE_BUTTON))) {
			Optional<ItemStack> made = craft(h, dummy, c.ingredient());
			check(h, made.isPresent() && made.get().is(c.expected().asItem()),
					"Dummy Block + " + c.ingredient() + " should make " + c.expected().asItem() + ", got " + made);
		}
		// The Dummy Block alone makes nothing.
		check(h, craft(h, dummy).isEmpty(), "the Dummy Block alone should make nothing");
		h.succeed();
	}
}
