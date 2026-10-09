package com.bencao;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

public final class HerbCropBlock extends CropBlock {
    public static final MapCodec<HerbCropBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            propertiesCodec(), Codec.STRING.fieldOf("seed").forGetter(crop -> crop.seedName)
    ).apply(instance, HerbCropBlock::new));

    private final String seedName;

    public HerbCropBlock(Properties properties, String seedName) {
        super(properties);
        this.seedName = seedName;
    }

    @Override
    public MapCodec<HerbCropBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Bencao.MOD_ID, seedName));
    }
}
