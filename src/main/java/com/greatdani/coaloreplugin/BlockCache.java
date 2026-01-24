package com.greatdani.coaloreplugin;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

import java.util.List;
import java.util.Random;
import java.util.Set;

public class BlockCache {

    public static class BlockCaches {
        final List<OreType> oreTypes;

        BlockCaches(List<OreType> oreTypes){
            this.oreTypes = oreTypes;
        }

        OreType getOreByName(String name) {
            for (OreType ore : oreTypes) {
                if (ore.name.equals(name)) {
                    return ore;
                }
            }
            return null;
        }

        OreType getOreForBlock ( int blockId, int y){
            for (OreType ore : oreTypes) {
                if (ore.canPlaceAt(blockId, y)) {
                    return ore;
                }
            }
            return null;
        }

        OreType getRandomOre (Random rand){
            return oreTypes.get(rand.nextInt(oreTypes.size()));
        }
    }

    public static class OreType {
        final int id;
        final BlockType type;
        final String name;
        final Set<Integer> replaceableBlockIds;
        final int minY;
        final int maxY;

        OreType(int id, BlockType type, String name, Set<Integer> replaceableBlockIds, int minY, int maxY) {
            this.id = id;
            this.type = type;
            this.name = name;
            this.replaceableBlockIds = replaceableBlockIds;
            this.minY = minY;
            this.maxY = maxY;
        }

        boolean canReplace(int blockId) {
            return replaceableBlockIds.contains(blockId);
        }

        // Add this method
        boolean isValidAtY(int y) {
            return y >= minY && y <= maxY;
        }

        // Combined check
        boolean canPlaceAt(int blockId, int y) {
            return isValidAtY(y) && canReplace(blockId);
        }
    }
}
