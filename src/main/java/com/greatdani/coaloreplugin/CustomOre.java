package com.greatdani.coaloreplugin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.util.Arrays;
import java.util.List;

public class CustomOre {
    public static final BuilderCodec<CustomOre> CODEC =
            BuilderCodec.builder(CustomOre.class, CustomOre::new)
                    .append(new KeyedCodec<String>("OreName", Codec.STRING),
                            (ore, val, info) -> ore.oreName = val,
                            (ore, info) -> ore.oreName)
                    .add()
                    .append(new KeyedCodec<String[]>("ReplacesBlocks", Codec.STRING_ARRAY),
                            (ore, val, info) -> ore.replacesBlocks = val,
                            (ore, info) -> ore.replacesBlocks)
                    .add()
                    .append(new KeyedCodec<Integer>("MinY", Codec.INTEGER),
                            (ore, val, info) -> ore.minY = val,
                            (ore, info) -> ore.minY)
                    .add()
                    .append(new KeyedCodec<Integer>("MaxY", Codec.INTEGER),
                            (ore, val, info) -> ore.maxY = val,
                            (ore, info) -> ore.maxY)
                    .add()
                    .append(new KeyedCodec<Integer>("VeinsPerChunk", Codec.INTEGER),
                            (ore, val, info) -> ore.veinsPerChunk = val,
                            (ore, info) -> ore.veinsPerChunk)
                    .add()
                    .append(new KeyedCodec<Double>("SpawnChance", Codec.DOUBLE),
                            (ore, val, info) -> ore.spawnChance = val,
                            (ore, info) -> ore.spawnChance)
                    .add()
                    .append(new KeyedCodec<Integer>("MinVeinSize", Codec.INTEGER),
                            (ore, val, info) -> ore.minVeinSize = val,
                            (ore, info) -> ore.minVeinSize)
                    .add()
                    .append(new KeyedCodec<Integer>("MaxVeinSize", Codec.INTEGER),
                            (ore, val, info) -> ore.maxVeinSize = val,
                            (ore, info) -> ore.maxVeinSize)
                    .add()
                    .build();

    private String oreName = "";
    private String[] replacesBlocks = new String[0];  // Array instead of List
    private int minY = 10;
    private int maxY = 120;

    private int veinsPerChunk = 5;
    private double spawnChance = 0.5;
    private int minVeinSize = 3;
    private int maxVeinSize = 8;

    public CustomOre() {
    }

    public String getOreName() {
        return oreName;
    }

    public List<String> getReplacesBlocks() {
        if (replacesBlocks == null || replacesBlocks.length == 0) {
            return List.of();
        }
        return Arrays.asList(replacesBlocks);
    }


    public int getMinY() {
        return Math.max(1, minY);
    }

    public int getMaxY() {
        return Math.min(310, maxY);
    }

    public boolean isValidAtY(int y) {
        return y >= getMinY() && y <= getMaxY();
    }

    public int getVeinsPerChunk() {
        return Math.max(1, veinsPerChunk);
    }

    public double getSpawnChance() {
        return Math.max(0.0, Math.min(1.0, spawnChance));
    }

    public int getMinVeinSize() {
        return Math.max(1, minVeinSize);
    }

    public int getMaxVeinSize() {
        return Math.max(minVeinSize, maxVeinSize);
    }

}
