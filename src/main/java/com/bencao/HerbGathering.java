package com.bencao;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

final class HerbGathering {
    private HerbGathering() {}

    static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player player) || player.isCreative()
                || event.getTool().canPerformAction(ItemAbilities.SHEARS_DIG)
                || !event.getLevel().getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            return;
        }
        // Single-block plants give exactly one roll; planted crops cannot reroll wild seeds.
        if (!event.getState().is(Blocks.SHORT_GRASS) && !event.getState().is(Blocks.FERN)) {
            return;
        }
        var random = event.getLevel().getRandom();
        if (random.nextInt(10) != 0) {
            return;
        }
        int roll = random.nextInt(10);
        Item seed = roll < 5 ? Bencao.MINT_SEEDS.get()
                : roll < 8 ? Bencao.MUGWORT_SEEDS.get() : Bencao.GINSENG_SEEDS.get();
        var pos = event.getPos();
        event.getDrops().add(new ItemEntity(event.getLevel(), pos.getX() + 0.5,
                pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(seed)));
    }
}
