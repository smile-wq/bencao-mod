"""Generate the prototype's JSON resources; requires only Python 3's standard library."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"
HERBS = {
    "mint": ("薄荷", "Mint", "carrots", "kelp", "lime_dye", "wheat_seeds"),
    "mugwort": ("艾草", "Mugwort", "potatoes", "wheat", "green_dye", "melon_seeds"),
    "ginseng": ("人参", "Ginseng", "beetroots", "beetroot", "brown_dye", "pumpkin_seeds"),
}
ZH = {
    "itemGroup.bencao": "本草",
    "item.bencao.mortar": "药杵与药臼",
    "item.bencao.herbal_manual": "本草手札",
    "tooltip.bencao.mortar": "与干药材合成药粉；可研磨 64 次",
    "tooltip.bencao.mint_drink": "游戏效果：速度 I（1:30）",
    "tooltip.bencao.mugwort_drink": "游戏效果：抗性提升 I（0:45）",
    "tooltip.bencao.ginseng_drink": "游戏效果：生命恢复 I（0:10）",
    "book.bencao.gather": "§2本草 · 入门§r\n\n打掉矮草或蕨，有10%概率找到药种。\n\n薄荷常见，艾草次之，人参较少。\n\n剪刀不掉落药种。\n\n书 + 小麦种子可合成本手札。",
    "book.bencao.grow": "§2种植药材§r\n\n在有光照、附近有水的耕地上种植药种。\n\n有8个生长阶段，可用骨粉催熟。\n\n成熟后打掉，获得药材和种子。未成熟时只返还种子。",
    "book.bencao.process": "§2烘干与研磨§r\n\n鲜药材放入熔炉10秒，或放在营火上30秒，可得到干药材。\n\n干药材 + 药杵与药臼，可制成对应药粉。\n\n下一页是药臼配方。",
    "book.bencao.mortar": "§2药杵与药臼§r\n\n在工作台按以下方式摆放：\n\n空 木棍 空\n圆石 碗 圆石\n空 圆石 空\n\n每次研磨消耗1点耐久，共可使用64次。",
    "book.bencao.brew": "§2配制药饮§r\n\n薄荷粉或艾草粉\n+ 水桶 + 碗\n→ 对应待煎药饮。\n\n两份人参粉\n+ 蜜脾 + 水桶 + 碗\n→ 待煎参蜜饮。\n\n配制后返还空桶。",
    "book.bencao.boil": "§2煎煮§r\n\n待煎药饮放入熔炉20秒，或营火30秒，即可饮用。\n\n长按使用键喝下，获得对应游戏增益。\n\n喝完返还空碗。\n满饥饿值也可饮用。",
    "book.bencao.effects": "§2药饮效果§r\n\n薄荷饮：\n速度 I，90秒。\n\n艾草饮：\n抗性提升 I，45秒。\n\n参蜜饮：\n生命恢复 I，10秒。\n\n以上为游戏效果设定。",
}
EN = {
    "itemGroup.bencao": "Bencao",
    "item.bencao.mortar": "Mortar and Pestle",
    "item.bencao.herbal_manual": "Herbal Field Guide",
    "tooltip.bencao.mortar": "Craft with dried herbs to grind; lasts 64 uses",
    "tooltip.bencao.mint_drink": "Game effect: Speed I (1:30)",
    "tooltip.bencao.mugwort_drink": "Game effect: Resistance I (0:45)",
    "tooltip.bencao.ginseng_drink": "Game effect: Regeneration I (0:10)",
    "book.bencao.gather": "Bencao: Gathering\n\nBreak short grass or ferns without shears: 10% chance of herb seeds.\n\nMint is common; ginseng is rare.\n\nBook + wheat seeds makes this guide.",
    "book.bencao.grow": "Growing Herbs\n\nPlant on lit farmland near water. Bone meal works.\n\nBreak mature crops for herbs and seeds.\n\nYoung crops only return a seed.",
    "book.bencao.process": "Dry and Grind\n\nDry herbs in a furnace (10s) or on a campfire (30s).\n\nDried herb + mortar makes powder.\n\nSee the next page for the mortar recipe.",
    "book.bencao.mortar": "Mortar and Pestle\n\nCrafting table:\n .  stick  .\nstone bowl stone\n .  stone  .\n\nUse cobblestone.\n\nLasts 64 uses.",
    "book.bencao.brew": "Mixing\n\nMint or mugwort powder + water bucket + bowl.\n\nGinseng needs two powders, a honeycomb, a water bucket and a bowl.\n\nThe bucket is returned.",
    "book.bencao.boil": "Brewing\n\nHeat an unbrewed drink in a furnace (20s) or on a campfire (30s).\n\nHold use to drink, even at full hunger.\n\nDrinking returns a bowl.",
    "book.bencao.effects": "Game Effects\n\nMint:\nSpeed I, 90s.\n\nMugwort:\nResistance I, 45s.\n\nHoney Ginseng:\nRegeneration I, 10s.",
}


def write(path, data):
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def item_model(name, texture):
    write(f"assets/bencao/models/item/{name}.json", {
        "parent": "minecraft:item/generated", "textures": {"layer0": f"minecraft:item/{texture}"}
    })


def recipe(name, data, trigger):
    write(f"data/bencao/recipe/{name}.json", data)
    write(f"data/bencao/advancement/recipes/{name}.json", {
        "criteria": {"has_ingredient": {
            "trigger": "minecraft:inventory_changed",
            "conditions": {"items": [{"items": [trigger]}]},
        }},
        "requirements": [["has_ingredient"]],
        "rewards": {"recipes": [f"bencao:{name}"]},
    })


def shapeless(name, ingredients, trigger):
    recipe(name, {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": item} for item in ingredients],
        "result": {"id": f"bencao:{name}", "count": 1},
    }, trigger)


def cook(output, ingredient, furnace_time):
    for method, ticks in (("smelting", furnace_time), ("campfire_cooking", 600)):
        recipe(f"{output}_from_{method}", {
            "type": f"minecraft:{method}", "category": "misc" if method == "smelting" else "food",
            "ingredient": {"item": f"bencao:{ingredient}"},
            "result": {"id": f"bencao:{output}"}, "experience": 0.1, "cookingtime": ticks,
        }, f"bencao:{ingredient}")


def loot_pool(item, count, conditions):
    functions = [{"function": "minecraft:set_count", "count": count}]
    return {
        "rolls": 1, "conditions": conditions + [{"condition": "minecraft:survives_explosion"}],
        "entries": [{"type": "minecraft:item", "name": f"bencao:{item}", "functions": functions}],
    }


for herb, (zh, en, crop_texture, fresh_texture, powder_texture, seed_texture) in HERBS.items():
    for suffix, zh_name, en_name in (
        (herb, zh, en), (f"{herb}_seeds", f"{zh}种子", f"{en} Seeds"),
        (f"dried_{herb}", f"干{zh}", f"Dried {en}"),
        (f"{herb}_powder", f"{zh}粉", f"{en} Powder"),
        (f"raw_{herb}_drink", f"待煎{'参蜜' if herb == 'ginseng' else zh}饮", f"Unbrewed {en} Drink"),
        (f"{herb}_drink", "参蜜饮" if herb == "ginseng" else f"{zh}饮",
         "Honey Ginseng Drink" if herb == "ginseng" else f"{en} Drink"),
    ):
        ZH[f"item.bencao.{suffix}"] = zh_name
        EN[f"item.bencao.{suffix}"] = en_name
    ZH[f"block.bencao.{herb}_crop"] = f"{zh}植株"
    EN[f"block.bencao.{herb}_crop"] = f"{en} Crop"
    item_model(herb, fresh_texture)
    item_model(f"{herb}_seeds", seed_texture)
    item_model(f"dried_{herb}", "dried_kelp")
    item_model(f"{herb}_powder", powder_texture)
    item_model(f"raw_{herb}_drink", "mushroom_stew")
    item_model(f"{herb}_drink", "rabbit_stew")
    write(f"assets/bencao/blockstates/{herb}_crop.json", {"variants": {
        f"age={age}": {"model": f"bencao:block/{herb}_stage{stage}"}
        for age, stage in enumerate([0, 0, 1, 1, 2, 2, 2, 3])
    }})
    for stage in range(4):
        write(f"assets/bencao/models/block/{herb}_stage{stage}.json", {
            "parent": "minecraft:block/crop", "render_type": "minecraft:cutout",
            "textures": {"crop": f"minecraft:block/{crop_texture}_stage{stage}"},
        })
    mature = [{"condition": "minecraft:block_state_property", "block": f"bencao:{herb}_crop",
               "properties": {"age": "7"}}]
    write(f"data/bencao/loot_table/blocks/{herb}_crop.json", {
        "type": "minecraft:block", "pools": [
            loot_pool(f"{herb}_seeds", 1, []),
            loot_pool(herb, {"type": "minecraft:uniform", "min": 1, "max": 2}, mature),
            loot_pool(f"{herb}_seeds", {"type": "minecraft:uniform", "min": 0, "max": 2}, mature),
        ],
    })
    cook(f"dried_{herb}", herb, 200)
    shapeless(f"{herb}_powder", [f"bencao:dried_{herb}", "bencao:mortar"], f"bencao:dried_{herb}")
    ingredients = [f"bencao:{herb}_powder", "minecraft:water_bucket", "minecraft:bowl"]
    if herb == "ginseng":
        ingredients += ["bencao:ginseng_powder", "minecraft:honeycomb"]
    shapeless(f"raw_{herb}_drink", ingredients, f"bencao:{herb}_powder")
    cook(f"{herb}_drink", f"raw_{herb}_drink", 400)

item_model("mortar", "bowl")
item_model("herbal_manual", "book")
recipe("mortar", {
    "type": "minecraft:crafting_shaped", "category": "equipment",
    "pattern": [" S ", "CBC", " C "],
    "key": {"S": {"item": "minecraft:stick"}, "C": {"item": "minecraft:cobblestone"},
            "B": {"item": "minecraft:bowl"}},
    "result": {"id": "bencao:mortar", "count": 1},
}, "minecraft:cobblestone")
shapeless("herbal_manual", ["minecraft:book", "minecraft:wheat_seeds"], "minecraft:wheat_seeds")
write("assets/bencao/lang/zh_cn.json", ZH)
write("assets/bencao/lang/en_us.json", EN)
write("data/minecraft/tags/block/crops.json", {
    "replace": False, "values": [f"bencao:{herb}_crop" for herb in HERBS]
})
write("data/c/tags/item/seeds.json", {
    "replace": False, "values": [f"bencao:{herb}_seeds" for herb in HERBS]
})
print("Generated resources for 3 crops, 20 items, and 20 recipes.")
