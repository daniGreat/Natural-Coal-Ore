package com.greatdani.coaloreplugin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CoalOreConfig {

    public static final BuilderCodec<CoalOreConfig> CODEC =
            BuilderCodec.builder(CoalOreConfig.class, CoalOreConfig::new)
                    // Y-level range
                    .append(new KeyedCodec<Integer>("MinY", Codec.INTEGER),
                            (cfg, val, info) -> cfg.minY = val,
                            (cfg, info) -> cfg.minY)
                    .add()
                    .append(new KeyedCodec<Integer>("MaxY", Codec.INTEGER),
                            (cfg, val, info) -> cfg.maxY = val,
                            (cfg, info) -> cfg.maxY)
                    .add()

                    // Vein settings
                    .append(new KeyedCodec<Integer>("VeinsPerChunk", Codec.INTEGER),
                            (cfg, val, info) -> cfg.veinsPerChunk = val,
                            (cfg, info) -> cfg.veinsPerChunk)
                    .add()
                    .append(new KeyedCodec<Integer>("MinVeinSize", Codec.INTEGER),
                            (cfg, val, info) -> cfg.minVeinSize = val,
                            (cfg, info) -> cfg.minVeinSize)
                    .add()
                    .append(new KeyedCodec<Integer>("MaxVeinSize", Codec.INTEGER),
                            (cfg, val, info) -> cfg.maxVeinSize = val,
                            (cfg, info) -> cfg.maxVeinSize)
                    .add()

                    // Spawn chance (0.0 to 1.0)
                    .append(new KeyedCodec<Double>("SpawnChance", Codec.DOUBLE),
                            (cfg, val, info) -> cfg.spawnChance = val,
                            (cfg, info) -> cfg.spawnChance)
                    .add()

                    // Enable/disable natural generation
                    .append(new KeyedCodec<Boolean>("EnableNaturalGeneration", Codec.BOOLEAN),
                            (cfg, val, info) -> cfg.enableNaturalGeneration = val,
                            (cfg, info) -> cfg.enableNaturalGeneration)
                    .add()
                    .append(new KeyedCodec<CustomOre[]>("CustomOres", new ArrayCodec<>(CustomOre.CODEC, CustomOre[]::new)),
                            (cfg, val, info) -> cfg.customOres = val,
                            (cfg, info) -> cfg.customOres)
                    .add()
                    .build();

    // Y-level range for ore spawning
    private int minY = 10;
    private int maxY = 120;

    // Vein configuration
    private int veinsPerChunk = 20;
    private int minVeinSize = 6;
    private int maxVeinSize = 17;

    // Chance for a chunk to contain coal (0.0 - 1.0)
    private double spawnChance = 0.85;

    private List<String> coalOreBlocks = List.of(
            "Ore_Coal_Stone",
            "Ore_Coal_Volcanic",
            "Ore_Coal_Slate",
            "Ore_Coal_Shale",
            "Ore_Coal_Sandstone",
            "Ore_Coal_Quartzite",
            "Ore_Coal_Marble",
            "Ore_Coal_Basalt",
            "Ore_Coal_Aqua"
    );

    // Add a method to get the matching stone types for each ore
    public final Map<String, List<String>> oreToStoneMap = Map.ofEntries(
            Map.entry("Ore_Coal_Stone", List.of("Rock_Stone", "Rock_Stone_Cobble", "Rock_Stone_Mossy", "Soil_Mud_Dry")),
            Map.entry("Ore_Coal_Volcanic", List.of("Rock_Volcanic_Cracked_Lava", "Rock_Volcanic")),
            Map.entry("Ore_Coal_Basalt", List.of("Rock_Basalt", "Rock_Basalt_Cobble")),
            Map.entry("Ore_Coal_Sandstone", List.of("Rock_Sandstone", "Rock_Sandstone_Cobble")),
            Map.entry("Ore_Coal_Marble", List.of("Rock_Marble", "Rock_Marble_Cobble")),
            Map.entry("Ore_Coal_Slate", List.of("Rock_Slate", "Rock_Slate_Cobble")),
            Map.entry("Ore_Coal_Shale", List.of("Rock_Shale", "Rock_Shale_Cobble")),
            Map.entry("Ore_Coal_Quartzite", List.of("Rock_Quartzite", "Rock_Quartzite_Cobble")),
            Map.entry("Ore_Coal_Aqua", List.of("Rock_Aqua", "Rock_Aqua_Cobble"))  // Adjust stone names as needed
    );

    private CustomOre[] customOres = new CustomOre[0];

    // Toggle natural generation on/off
    private boolean enableNaturalGeneration = true;

    // Constants (not configurable)
    public static final int WORLD_MIN_Y = 1;
    public static final int WORLD_MAX_Y = 310;
    public static final int CHUNK_SIZE = 32;
    public static final int CHUNK_SHIFT = 5;

    public CoalOreConfig() {
    }

    // Getters with validation
    public int getMinY() {
        return Math.max(WORLD_MIN_Y, Math.min(maxY - 1, minY));
    }

    public int getMaxY() {
        return Math.max(minY + 1, Math.min(WORLD_MAX_Y, maxY));
    }

    public int getVeinsPerChunk() {
        return Math.max(1, Math.min(80, veinsPerChunk));
    }

    public int getMinVeinSize() {
        return Math.max(1, Math.min(maxVeinSize, minVeinSize));
    }

    public int getMaxVeinSize() {
        return Math.max(minVeinSize, Math.min(100, maxVeinSize));
    }

    public double getSpawnChance() {
        return Math.max(0.0, Math.min(1.0, spawnChance));
    }

    public List<String> getCoalOreBlocks() {
        return coalOreBlocks;
    }

    public List<CustomOre> getCustomOres() {
        if (customOres == null || customOres.length == 0) {
            return List.of();
        }
        return Arrays.asList(customOres);
    }

    public boolean isNaturalGenerationEnabled() {
        return enableNaturalGeneration;
    }
}