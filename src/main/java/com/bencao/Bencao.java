package com.bencao;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(Bencao.MOD_ID)
public final class Bencao {
    public static final String MOD_ID = "bencao";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredBlock<HerbCropBlock> MINT_CROP = crop("mint");
    public static final DeferredBlock<HerbCropBlock> MUGWORT_CROP = crop("mugwort");
    public static final DeferredBlock<HerbCropBlock> GINSENG_CROP = crop("ginseng");

    public static final DeferredItem<ItemNameBlockItem> MINT_SEEDS = seeds("mint", MINT_CROP);
    public static final DeferredItem<ItemNameBlockItem> MUGWORT_SEEDS = seeds("mugwort", MUGWORT_CROP);
    public static final DeferredItem<ItemNameBlockItem> GINSENG_SEEDS = seeds("ginseng", GINSENG_CROP);

    public static final DeferredItem<Item> MINT = ITEMS.registerSimpleItem("mint");
    public static final DeferredItem<Item> MUGWORT = ITEMS.registerSimpleItem("mugwort");
    public static final DeferredItem<Item> GINSENG = ITEMS.registerSimpleItem("ginseng");
    public static final DeferredItem<Item> DRIED_MINT = ITEMS.registerSimpleItem("dried_mint");
    public static final DeferredItem<Item> DRIED_MUGWORT = ITEMS.registerSimpleItem("dried_mugwort");
    public static final DeferredItem<Item> DRIED_GINSENG = ITEMS.registerSimpleItem("dried_ginseng");
    public static final DeferredItem<Item> MINT_POWDER = ITEMS.registerSimpleItem("mint_powder");
    public static final DeferredItem<Item> MUGWORT_POWDER = ITEMS.registerSimpleItem("mugwort_powder");
    public static final DeferredItem<Item> GINSENG_POWDER = ITEMS.registerSimpleItem("ginseng_powder");

    public static final DeferredItem<MortarItem> MORTAR = ITEMS.registerItem("mortar", MortarItem::new,
            new Item.Properties().durability(64));

    public static final DeferredItem<Item> RAW_MINT_DRINK = mixture("raw_mint_drink");
    public static final DeferredItem<Item> RAW_MUGWORT_DRINK = mixture("raw_mugwort_drink");
    public static final DeferredItem<Item> RAW_GINSENG_DRINK = mixture("raw_ginseng_drink");

    public static final DeferredItem<HerbalDrinkItem> MINT_DRINK = ITEMS.registerItem("mint_drink",
            props -> new HerbalDrinkItem(props, "tooltip.bencao.mint_drink"),
            new Item.Properties().stacksTo(16).food(drinkFood()
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1800), 1.0F).build()));
    public static final DeferredItem<HerbalDrinkItem> MUGWORT_DRINK = ITEMS.registerItem("mugwort_drink",
            props -> new HerbalDrinkItem(props, "tooltip.bencao.mugwort_drink"),
            new Item.Properties().stacksTo(16).food(drinkFood()
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 900), 1.0F).build()));
    public static final DeferredItem<HerbalDrinkItem> GINSENG_DRINK = ITEMS.registerItem("ginseng_drink",
            props -> new HerbalDrinkItem(props, "tooltip.bencao.ginseng_drink"),
            new Item.Properties().stacksTo(16).food(drinkFood()
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 200), 1.0F).build()));

    public static final DeferredItem<HerbalManualItem> HERBAL_MANUAL = ITEMS.registerItem("herbal_manual",
            HerbalManualItem::new, new Item.Properties().stacksTo(1).component(DataComponents.WRITTEN_BOOK_CONTENT,
                    new WrittenBookContent(Filterable.passThrough("本草手札"), "本草", 0, List.of(
                            page("book.bencao.gather"), page("book.bencao.grow"),
                            page("book.bencao.process"), page("book.bencao.mortar"),
                            page("book.bencao.brew"), page("book.bencao.boil"),
                            page("book.bencao.effects")), true)));

    static {
        TABS.register("bencao", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.bencao"))
                .icon(() -> GINSENG.get().getDefaultInstance())
                .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get())))
                .build());
    }

    public Bencao(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
        NeoForge.EVENT_BUS.addListener(HerbGathering::onBlockDrops);
    }

    private static DeferredBlock<HerbCropBlock> crop(String herb) {
        return BLOCKS.registerBlock(herb + "_crop", properties -> new HerbCropBlock(properties, herb + "_seeds"),
                BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT));
    }

    private static DeferredItem<ItemNameBlockItem> seeds(String herb, DeferredBlock<HerbCropBlock> crop) {
        return ITEMS.registerItem(herb + "_seeds", properties -> new ItemNameBlockItem(crop.get(), properties));
    }

    private static DeferredItem<Item> mixture(String name) {
        return ITEMS.registerSimpleItem(name, new Item.Properties().stacksTo(16));
    }

    private static FoodProperties.Builder drinkFood() {
        return new FoodProperties.Builder().nutrition(0).saturationModifier(0)
                .alwaysEdible().usingConvertsTo(Items.BOWL);
    }

    private static Filterable<Component> page(String key) {
        return Filterable.passThrough(Component.translatable(key));
    }
}
