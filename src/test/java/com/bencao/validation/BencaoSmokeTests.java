package com.bencao.validation;

import com.bencao.Bencao;
import com.bencao.HerbCropBlock;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

/** A separate test-only mod; never included in the distributable Bencao JAR. */
@Mod("bencao_smoke_tests")
public final class BencaoSmokeTests {
    private static final Logger LOG = LogUtils.getLogger();

    public BencaoSmokeTests() {
        NeoForge.EVENT_BUS.addListener(this::run);
    }

    private void run(ServerStartedEvent event) {
        try {
            var level = event.getServer().overworld();
            long recipeCount = level.getRecipeManager().getRecipes().stream()
                    .filter(recipe -> recipe.id().getNamespace().equals("bencao")).count();
            check(recipeCount == 20, "All 20 recipes decode during a real server resource reload");
            for (String herb : List.of("mint", "mugwort", "ginseng")) {
                checkProcessing(level, herb);
                checkCrop(level, herb);
            }
            checkDrinking(level, "mint_drink", MobEffects.MOVEMENT_SPEED, 1800);
            checkDrinking(level, "mugwort_drink", MobEffects.DAMAGE_RESISTANCE, 900);
            checkDrinking(level, "ginseng_drink", MobEffects.REGENERATION, 200);
            LOG.info("BENCAO_SMOKE_PASS: recipe matching, mortar wear, buckets, crop loot, drinks and bowls");
        } catch (Throwable failure) {
            LOG.error("BENCAO_SMOKE_FAIL", failure);
        } finally {
            event.getServer().halt(false);
        }
    }

    private static void checkProcessing(ServerLevel level, String herb) {
        var recipes = level.getRecipeManager();
        for (String[] step : List.of(new String[]{herb, "dried_" + herb},
                new String[]{"raw_" + herb + "_drink", herb + "_drink"})) {
            var input = new SingleRecipeInput(new ItemStack(item(step[0])));
            var furnace = recipes.getRecipeFor(RecipeType.SMELTING, input, level).orElseThrow();
            var campfire = recipes.getRecipeFor(RecipeType.CAMPFIRE_COOKING, input, level).orElseThrow();
            check(furnace.value().assemble(input, level.registryAccess()).is(item(step[1])), "furnace output");
            check(campfire.value().assemble(input, level.registryAccess()).is(item(step[1])), "campfire output");
        }

        // Exercise the recipe manager's remainder path, including the last tool use.
        ItemStack mortar = new ItemStack(Bencao.MORTAR.get());
        for (int uses = 0; uses < 64; uses++) {
            int initialDamage = mortar.getDamageValue();
            var input = CraftingInput.of(2, 1, List.of(new ItemStack(item("dried_" + herb)), mortar));
            var recipe = recipes.getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
            check(recipe.value().assemble(input, level.registryAccess()).is(item(herb + "_powder")), "powder output");
            var remainders = recipes.getRemainingItemsFor(RecipeType.CRAFTING, input, level);
            check(mortar.getDamageValue() == initialDamage, "recipe remainder must not mutate input");
            mortar = remainders.get(1);
            check(uses == 63 ? mortar.isEmpty() : mortar.getDamageValue() == uses + 1, "mortar durability");
        }

        var ingredients = new ArrayList<ItemStack>(List.of(new ItemStack(item(herb + "_powder")),
                new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.BOWL)));
        if (herb.equals("ginseng")) {
            ingredients.add(new ItemStack(item("ginseng_powder")));
            ingredients.add(new ItemStack(Items.HONEYCOMB));
        }
        while (ingredients.size() < 9) ingredients.add(ItemStack.EMPTY);
        var input = CraftingInput.of(3, 3, ingredients);
        var recipe = recipes.getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
        check(recipe.value().assemble(input, level.registryAccess()).is(item("raw_" + herb + "_drink")), "mixture output");
        var remainders = recipes.getRemainingItemsFor(RecipeType.CRAFTING, input, level);
        check(remainders.stream().filter(stack -> stack.is(Items.BUCKET)).count() == 1, "water bucket return");
    }

    private static void checkCrop(ServerLevel level, String herb) {
        HerbCropBlock crop = (HerbCropBlock) BuiltInRegistries.BLOCK.get(id(herb + "_crop"));
        for (int age : List.of(0, 6, 7)) {
            List<ItemStack> loot = Block.getDrops(crop.getStateForAge(age), level, BlockPos.ZERO, null);
            int herbs = loot.stream().filter(stack -> stack.is(item(herb))).mapToInt(ItemStack::getCount).sum();
            int seeds = loot.stream().filter(stack -> stack.is(item(herb + "_seeds"))).mapToInt(ItemStack::getCount).sum();
            check(age == 7 ? herbs >= 1 && herbs <= 2 : herbs == 0, "crop maturity controls produce");
            check(age == 7 ? seeds >= 1 && seeds <= 3 : seeds == 1, "crop returns viable seeds");
        }
    }

    private static void checkDrinking(ServerLevel level, String name, Holder<MobEffect> effect, int duration) {
        var player = FakePlayerFactory.getMinecraft(level);
        player.getAbilities().instabuild = false;
        for (int count : List.of(1, 16)) {
            player.getInventory().clearContent();
            player.removeAllEffects();
            player.getFoodData().setFoodLevel(20);
            ItemStack drink = new ItemStack(item(name), count);
            player.setItemInHand(InteractionHand.MAIN_HAND, drink);
            var use = drink.use(level, player, InteractionHand.MAIN_HAND);
            check(use.getResult().consumesAction(), "drink usable at full hunger");
            ItemStack result = drink.finishUsingItem(level, player);
            if (count == 1) {
                check(result.is(Items.BOWL), "last drink becomes a bowl");
            } else {
                check(result.is(item(name)) && result.getCount() == 15, "drink stack decrements once");
                check(player.getInventory().countItem(Items.BOWL) == 1, "stacked drink returns exactly one bowl");
            }
            check(player.hasEffect(effect) && player.getEffect(effect).getDuration() == duration,
                    "drink applies intended effect duration");
            player.stopUsingItem();
        }
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath("bencao", name);
    }

    private static Item item(String name) {
        Item value = BuiltInRegistries.ITEM.get(id(name));
        check(value != Items.AIR, "registered item " + name);
        return value;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
