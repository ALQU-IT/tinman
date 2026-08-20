package dev.alqu.tinman.recipe;

import com.mojang.serialization.Codec;
import dev.alqu.tinman.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A shaped 3x3 recipe that only the Assembler can run.
 *
 * <p>Loaded from {@code data/<namespace>/recipe/*.json} with {@code "type": "tinman:assembling"},
 * so server admins and datapacks can add their own without touching the mod. The shape syntax is
 * deliberately identical to {@code minecraft:crafting_shaped} — pattern plus key — so it is
 * familiar, and it reuses vanilla's {@link ShapedRecipePattern} for matching (which handles
 * shifting the pattern around the grid).
 *
 * <p>{@code power_cost} is the number of Voltite Ingots consumed from the power cell slot per
 * craft, defaulting to 1.
 */
public class AssemblingRecipe implements Recipe<CraftingInput> {
	public static final int DEFAULT_POWER_COST = 1;

	private final Recipe.CommonInfo commonInfo;
	private final String group;
	private final ShapedRecipePattern pattern;
	private final ItemStackTemplate result;
	private final int powerCost;

	private @Nullable PlacementInfo placementInfo;

	public AssemblingRecipe(Recipe.CommonInfo commonInfo, String group, ShapedRecipePattern pattern, ItemStackTemplate result, int powerCost) {
		this.commonInfo = commonInfo;
		this.group = group;
		this.pattern = pattern;
		this.result = result;
		this.powerCost = powerCost;
	}

	public static final MapCodec<AssemblingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
		i -> i.group(
				Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
				Codec.STRING.optionalFieldOf("group", "").forGetter(o -> o.group),
				ShapedRecipePattern.MAP_CODEC.forGetter(o -> o.pattern),
				ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result),
				Codec.INT.optionalFieldOf("power_cost", DEFAULT_POWER_COST).forGetter(o -> o.powerCost)
			)
			.apply(i, AssemblingRecipe::new)
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, AssemblingRecipe> STREAM_CODEC = StreamCodec.composite(
		Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo,
		ByteBufCodecs.STRING_UTF8, o -> o.group,
		ShapedRecipePattern.STREAM_CODEC, o -> o.pattern,
		ItemStackTemplate.STREAM_CODEC, o -> o.result,
		ByteBufCodecs.VAR_INT, o -> o.powerCost,
		AssemblingRecipe::new
	);

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return this.pattern.matches(input);
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		return this.result.create();
	}

	/** Result preview for the GUI; never handed to the player directly. */
	public ItemStack previewResult() {
		return this.result.create();
	}

	public int powerCost() {
		return this.powerCost;
	}

	@Override
	public boolean showNotification() {
		return this.commonInfo.showNotification();
	}

	@Override
	public String group() {
		return this.group;
	}

	@Override
	public RecipeSerializer<AssemblingRecipe> getSerializer() {
		return ModRecipes.ASSEMBLING_SERIALIZER;
	}

	@Override
	public RecipeType<AssemblingRecipe> getType() {
		return ModRecipes.ASSEMBLING;
	}

	@Override
	public PlacementInfo placementInfo() {
		if (this.placementInfo == null) {
			this.placementInfo = PlacementInfo.createFromOptionals(this.pattern.ingredients());
		}

		return this.placementInfo;
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		return ModRecipes.ASSEMBLING_CATEGORY;
	}

	/**
	 * What the recipe book draws for this recipe. Without this the book has nothing to render and
	 * the recipe simply never appears, even once unlocked.
	 *
	 * <p>The station icon is the Assembler rather than a crafting table, so the book makes it
	 * obvious where the recipe has to be made.
	 */
	@Override
	public List<RecipeDisplay> display() {
		return List.of(new ShapedCraftingRecipeDisplay(
			this.pattern.width(),
			this.pattern.height(),
			this.pattern.ingredients().stream()
				.map(ingredient -> ingredient.map(Ingredient::display).orElse(SlotDisplay.Empty.INSTANCE))
				.toList(),
			new SlotDisplay.ItemStackSlotDisplay(this.result),
			new SlotDisplay.ItemSlotDisplay(ModBlocks.ASSEMBLER.asItem())
		));
	}
}
