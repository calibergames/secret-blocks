package net.calibermc.secretblocks.gametest;

import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.core.registries.Registries;
import net.calibermc.secretblocks.gametest.support.GameTestPlatform;
import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.calibermc.secretblocks.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.calibermc.secretblocks.gametest.support.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
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
public class RecipeGameTests {

	/**
	 * Data files kept in the sources for content that is not registered yet
	 * (the wall ladder, dropped for now). The build leaves them out of every
	 * jar, so they log no parse errors.
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

	/** A mod data file: its name without folder or extension, and whether its load conditions (if any) accept it. */
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
						files.add(new DataFile(name, GameTestPlatform.INSTANCE.conditionsPass(GsonHelper.parse(reader))));
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
		files.sort(Comparator.comparing(DataFile::name));
		return files;
	}

	/** Every file loads; the parked ones must not be in the jar at all. */
	private static void checkLoads(GameTestHelper h, String kind, List<DataFile> files, Predicate<String> parked, Predicate<String> loaded) {
		check(h, files.size() >= 20, "expected the mod's " + kind + " files, found " + files);
		for (DataFile file : files) {
			String name = file.name();
			check(h, !parked.test(name), kind + " " + name + " is parked and must not be in this jar");
			check(h, file.conditionsPass(), kind + " " + name + " is rejected by its load conditions");
			check(h, loaded.test(name), kind + " " + name + " failed to load");
		}
	}

	/** Every crafting recipe the server has loaded. */
	private static List<RecipeHolder<?>> craftingRecipes(GameTestHelper h) {
		return h.getLevel().getServer().getRecipeManager().getRecipes().stream()
				.filter(holder -> holder.value().getType() == RecipeType.CRAFTING).toList();
	}

	/** Every recipe file loads, except the parked ones. */
	@GameTest
	public void everyRecipeLoads(GameTestHelper h) {
		MinecraftServer server = h.getLevel().getServer();
		checkLoads(h, "recipe", dataFiles(server, "recipe"), PARKED::contains,
				name -> server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(SecretBlocks.MOD_ID, name))).isPresent());
		h.succeed();
	}

	/** Every loot table file loads, except the parked ones. */
	@GameTest
	public void everyLootTableLoads(GameTestHelper h) {
		MinecraftServer server = h.getLevel().getServer();
		checkLoads(h, "loot table", dataFiles(server, "loot_table"), name -> PARKED.stream().anyMatch(p -> name.equals("blocks/" + p)),
				name -> TestSupport.lootTableIds(server).contains(Identifier.fromNamespaceAndPath(SecretBlocks.MOD_ID, name)));
		h.succeed();
	}

	/** Every registered block and item has a crafting recipe that makes it. */
	@GameTest
	public void everyItemHasARecipe(GameTestHelper h) {
		List<RecipeHolder<?>> recipes = craftingRecipes(h);
		int items = 0;
		for (Item item : BuiltInRegistries.ITEM) {
			if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(SecretBlocks.MOD_ID) || NO_RECIPE.contains(item)) {
				continue;
			}
			items++;
			check(h, recipes.stream().anyMatch(r -> makes(r.value(), h.getLevel(), item)), "no crafting recipe makes " + BuiltInRegistries.ITEM.getKey(item));
		}
		check(h, items >= 20, "expected the mod's items, found " + items);
		h.succeed();
	}

	/**
	 * Every mod recipe crafts its result from its own ingredients in a real
	 * crafting grid (the first item each ingredient accepts; shaped recipes
	 * laid out in their pattern).
	 */
	@GameTest
	public void everyRecipeCraftsFromItsIngredients(GameTestHelper h) {
		int crafted = 0;
		for (RecipeHolder<?> holder : craftingRecipes(h)) {
			if (!holder.id().identifier().getNamespace().equals(SecretBlocks.MOD_ID)) {
				continue;
			}
			CraftingRecipe recipe = (CraftingRecipe) holder.value();
			crafted++;
			int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
			ItemStack[] grid = new ItemStack[9];
			Arrays.fill(grid, ItemStack.EMPTY);
			// A shaped recipe's slots in pattern order (empty where the pattern has a gap); a shapeless one's ingredients.
			List<Optional<Ingredient>> ingredients = recipe instanceof ShapedRecipe shaped ? shaped.getIngredients()
					: recipe.placementInfo().ingredients().stream().map(Optional::of).toList();
			for (int i = 0; i < ingredients.size(); i++) {
				List<Holder<Item>> accepted = ingredients.get(i).map(ingredient -> ingredient.items().toList()).orElse(List.of());
				check(h, ingredients.get(i).isEmpty() || !accepted.isEmpty(), holder.id() + " ingredient " + i + " accepts no item");
				grid[(i / width) * 3 + i % width] = accepted.isEmpty() ? ItemStack.EMPTY : new ItemStack(accepted.get(0));
			}
			Optional<ItemStack> made = craft(h, grid);
			List<ItemStack> results = results(recipe, h.getLevel());
			check(h, made.isPresent() && results.stream().anyMatch(result -> made.get().is(result.getItem())),
					holder.id() + " should make " + results + " from its ingredients, got " + made);
		}
		check(h, crafted >= 20, "expected the mod's crafting recipes, found " + crafted);
		h.succeed();
	}

	/** The four fixed recipes (chest, lever, redstone, stone button), with the vanilla items a player would use. */
	@GameTest
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
		h.succeed();
	}

	/**
	 * The polished blackstone button and plate are vanilla's stone button
	 * and stone plate under another look, so they make the same secret blocks.
	 */
	@GameTest
	public void polishedBlackstoneCountsAsStone(GameTestHelper h) {
		Item dummy = ModBlocks.SOLID_BLOCK.asItem();
		for (Item[] c : new Item[][] {
				{Items.STONE_BUTTON, ModBlocks.STONE_BUTTON.asItem()},
				{Items.POLISHED_BLACKSTONE_BUTTON, ModBlocks.STONE_BUTTON.asItem()},
				{Items.STONE_PRESSURE_PLATE, ModBlocks.PLAYER_PRESSURE_PLATE.asItem()},
				{Items.POLISHED_BLACKSTONE_PRESSURE_PLATE, ModBlocks.PLAYER_PRESSURE_PLATE.asItem()}}) {
			Optional<ItemStack> made = craft(h, dummy, c[0]);
			check(h, made.isPresent() && made.get().is(c[1]), "Dummy Block + " + c[0] + " should make " + c[1] + ", got " + made);
		}
		h.succeed();
	}

	/** The Dummy Block alone makes nothing. */
	@GameTest
	public void theDummyBlockAloneMakesNothing(GameTestHelper h) {
		Item dummy = ModBlocks.SOLID_BLOCK.asItem();
		check(h, craft(h, dummy).isEmpty(), "the Dummy Block alone should make nothing");
		h.succeed();
	}
}
