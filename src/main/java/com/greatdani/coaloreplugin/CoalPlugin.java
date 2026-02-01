package com.greatdani.coaloreplugin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class CoalPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private volatile BlockCache.BlockCaches blockCache;
    private final AtomicBoolean initializationAttempted = new AtomicBoolean(false);

    private Config<CoalOreConfig> config = null;
    private boolean hasFilePathLocated = false;

    public CoalPlugin(@Nonnull JavaPluginInit init) {
        super(init);

        String userHome = System.getProperty("user.home");
        Path userModsPath = Path.of(userHome, "AppData", "Roaming", "Hytale", "UserData", "Mods", "NaturalCoalOre", "Config");

        // Use the server's plugin data directory instead of user AppData
        Path serverModsPath = getDataDirectory(); // This gives you: mods/NaturalCoalOre/ (or similar)
        Path configPath  = null;

        if (Files.exists(userModsPath) || Files.exists(userModsPath.getParent())) {
            try {
                if (!Files.exists(userModsPath)) {
                    Files.createDirectories(userModsPath);
                }
                configPath = userModsPath;
                hasFilePathLocated = true;
                LOGGER.atInfo().log("Using user mods folder: %s", configPath);
            } catch (Exception e) {
                LOGGER.atWarning().log("Failed to create user config directory: %s", e.getMessage());
            }
        }

        // Fall back to server mods folder
        if (configPath == null) {
            try {
                if (!Files.exists(serverModsPath)) {
                    Files.createDirectories(serverModsPath);
                }
                configPath = serverModsPath;
                hasFilePathLocated = true;
                LOGGER.atInfo().log("Using server mods folder: %s", configPath);
            } catch (Exception e) {
                LOGGER.atWarning().log("Failed to create server config directory: %s", e.getMessage());
                hasFilePathLocated = false;
            }
        }

        if(hasFilePathLocated) {
            this.config = new Config<>(configPath, "CoalOre", CoalOreConfig.CODEC);
        }
        else {
            this.config = withConfig("CoalOre", CoalOreConfig.CODEC);
        }



        System.out.println("Directory: " + getDataDirectory());
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up Coal Ore plugin...");

        CoalOreConfig cfg = null;
        if(hasFilePathLocated) {
            this.config.load().join();
            this.config.save();
            cfg = config.get();
            LOGGER.atInfo().log("Loaded the Config");
        }
        else {
            cfg = config.get();
            LOGGER.atInfo().log("Loaded the Default Config");
        }

        if(cfg != null) {
            if(!cfg.isNaturalGenerationEnabled() && cfg.getCustomOres().isEmpty()) {
                LOGGER.atInfo().log("No ore generation enabled");
                // Don't register any event
            }else if (cfg.isNaturalGenerationEnabled()) {
                // Register onChunkCoalGenerated and onChunkCustomOreGenerated.
                LOGGER.atInfo().log("Coal + Custom ore generation enabled");
                getEventRegistry().registerGlobal(
                        EventPriority.LATE,
                        ChunkPreLoadProcessEvent.class,
                        this::onChunkCoalGenerated
                );
            } else {
                // Register Only Custom Ore No Coal Ore Generation.
                LOGGER.atInfo().log("Custom ore generation only");
                getEventRegistry().registerGlobal(
                        EventPriority.LATE,
                        ChunkPreLoadProcessEvent.class,
                        this::onChunkCustomOreGenerated
                );
            }
        }

        getCommandRegistry().registerCommand(new CoalOreCommand());

        LOGGER.atInfo().log("Coal Ore plugin setup complete!");
        LOGGER.atInfo().log("  Natural generation: Y=%d-%d, ~%d veins/chunk, %.0f%% chunk chance",
                cfg.getMinY(), cfg.getMaxY(), cfg.getVeinsPerChunk(), cfg.getSpawnChance());
    }

    private void onChunkCoalGenerated(@Nonnull ChunkPreLoadProcessEvent event) {

        if (!event.isNewlyGenerated()) {
            return;
        }

        WorldChunk chunk = event.getChunk();
        if (chunk == null) {
            LOGGER.atWarning().log("Chunk was null!");
            return;
        }

        BlockCache.BlockCaches cache = getBlockCache();
        if (cache == null) {
            LOGGER.atWarning().log("BlockCache is null - initialization failed");
            return;
        }

        long chunkSeed = computeChunkSeed(chunk.getX(), chunk.getZ());
        Random chunkRandom = new Random(chunkSeed);
        CoalOreConfig cfg = this.config.get();

        if (chunkRandom.nextDouble() > cfg.getSpawnChance()) {
            return;
        }

        int chunkBlockX = chunk.getX() << CoalOreConfig.CHUNK_SHIFT;
        int chunkBlockZ = chunk.getZ() << CoalOreConfig.CHUNK_SHIFT;

        int numVeins = cfg.getVeinsPerChunk() + chunkRandom.nextInt(2);
        int totalPlaced = 0;
        int veinsCreated = 0;

        for (int i = 0; i < numVeins; i++) {
            int x = chunkBlockX + chunkRandom.nextInt(CoalOreConfig.CHUNK_SIZE);
            int z = chunkBlockZ + chunkRandom.nextInt(CoalOreConfig.CHUNK_SIZE);
            int y = computeOreY(chunkRandom);
            int size = cfg.getMinVeinSize() + chunkRandom.nextInt(cfg.getMaxVeinSize() - cfg.getMinVeinSize() + 1);

            int placed = generateVein(chunk, cache, x, y, z, size, chunkRandom, null);
            if (placed > 0) {
                totalPlaced += placed;
                veinsCreated++;
            }
        }

        onChunkCustomOreGenerated(event);
    }

    private void onChunkCustomOreGenerated(@Nonnull ChunkPreLoadProcessEvent event) {

        if (!event.isNewlyGenerated()) {
            return;
        }

        WorldChunk chunk = event.getChunk();
        if (chunk == null) {
            LOGGER.atWarning().log("Chunk was null!");
            return;
        }

        BlockCache.BlockCaches cache = getBlockCache();
        if (cache == null) {
            LOGGER.atWarning().log("BlockCache is null - initialization failed");
            return;
        }

        long chunkSeed = computeChunkSeed(chunk.getX(), chunk.getZ());
        Random chunkRandom = new Random(chunkSeed);
        CoalOreConfig cfg = this.config.get();



        int chunkBlockX = chunk.getX() << CoalOreConfig.CHUNK_SHIFT;
        int chunkBlockZ = chunk.getZ() << CoalOreConfig.CHUNK_SHIFT;

        List<CustomOre> customOreConfigs = cfg.getCustomOres();
        for (CustomOre customOre : customOreConfigs) {
            if (chunkRandom.nextDouble() > customOre.getSpawnChance()) {
                continue;
            }

            BlockCache.OreType oreType = cache.getOreByName(customOre.getOreName());
            if(oreType == null) continue;

            int customVeins = customOre.getVeinsPerChunk() + chunkRandom.nextInt(2);

            for (int i = 0; i < customVeins; i++) {
                int x = chunkBlockX + chunkRandom.nextInt(CoalOreConfig.CHUNK_SIZE);
                int z = chunkBlockZ + chunkRandom.nextInt(CoalOreConfig.CHUNK_SIZE);

                // Use THIS ore's Y range, not global
                int yRange = customOre.getMaxY() - customOre.getMinY();
                int y = customOre.getMinY() + chunkRandom.nextInt(yRange + 1);

                int size = customOre.getMinVeinSize() + chunkRandom.nextInt(customOre.getMaxVeinSize() - customOre.getMinVeinSize() + 1);

                generateVein(chunk, cache, x, y, z, size, chunkRandom, oreType);
            }
        }
    }

    private long computeChunkSeed(int chunkX, int chunkZ) {
        // Use large primes for better distribution
        return ((long) chunkX * 341873128712L) + ((long) chunkZ * 132897987541L);
    }

    private int computeOreY(Random random) {
        double factor = Math.pow(random.nextDouble(), 1.5);
        CoalOreConfig cfg = this.config.get();

        return cfg.getMinY() + (int) (factor * (cfg.getMaxY() - cfg.getMinY()));
    }

    private BlockCache.BlockCaches getBlockCache() {
        BlockCache.BlockCaches cache = blockCache;
        if (cache != null) {
            return cache;
        }

        if (!initializationAttempted.compareAndSet(false, true)) {
            return null;
        }

        return initializeBlockCache();
    }

    private synchronized BlockCache.BlockCaches initializeBlockCache() {
        if (blockCache != null) {
            return blockCache;
        }

        CoalOreConfig cfg = this.config.get();
        List<String> oreBlockNames = cfg.getCoalOreBlocks();
        List<CustomOre> customOres = cfg.getCustomOres();

        List<BlockCache.OreType> oreTypes = new ArrayList<>();

        for (String oreName : oreBlockNames) {
            BlockType oreType = BlockType.getAssetMap().getAsset(oreName);


            if (oreType == null) {
                LOGGER.atWarning().log("Ore block type '%s' not found, skipping.", oreName);
                continue;
            }

            int oreId = BlockType.getAssetMap().getIndex(oreName);
            // Get the stone types this ore can replace
            List<String> stoneNames = cfg.oreToStoneMap.getOrDefault(oreName, List.of());
            Set<Integer> replaceableIds = new HashSet<>();

            for (String stoneName : stoneNames) {
                int stoneId = BlockType.getAssetMap().getIndex(stoneName);
                if (stoneId != Integer.MIN_VALUE) {
                    replaceableIds.add(stoneId);
                }
            }

            if (replaceableIds.isEmpty()) {
                LOGGER.atWarning().log("No valid stone types found for '%s', skipping.", oreName);
                continue;
            }

            oreTypes.add(new BlockCache.OreType(oreId, oreType, oreName, replaceableIds, cfg.getMinY(), cfg.getMaxY()));
            LOGGER.atInfo().log("Registered ore: %s -> replaces %d stone types", oreName, replaceableIds.size());
        }

        for (CustomOre customOre : customOres) {
            String oreName = customOre.getOreName();
            if (oreName == null || oreName.isEmpty()) continue;

            BlockType oreType = BlockType.getAssetMap().getAsset(oreName);
            if (oreType == null) {
                LOGGER.atWarning().log("Custom ore '%s' not found, skipping.", oreName);
                continue;
            }

            int oreId = BlockType.getAssetMap().getIndex(oreName);
            Set<Integer> replaceableIds = new HashSet<>();

            for (String stoneName : customOre.getReplacesBlocks()) {
                int stoneId = BlockType.getAssetMap().getIndex(stoneName);
                if (stoneId != Integer.MIN_VALUE) {
                    replaceableIds.add(stoneId);
                }
            }

            if (replaceableIds.isEmpty()) {
                LOGGER.atWarning().log("No valid stone types for custom ore '%s', skipping.", oreName);
                continue;
            }

            oreTypes.add(new BlockCache.OreType(oreId, oreType, oreName, replaceableIds, customOre.getMinY(), customOre.getMaxY()));
            LOGGER.atInfo().log("Registered custom ore: %s -> replaces %d stone types", oreName, replaceableIds.size());
        }


        if (oreTypes.isEmpty()) {
            LOGGER.atWarning().log("No valid ore types found! Natural generation disabled.");
            return null;
        }

        blockCache = new BlockCache.BlockCaches(oreTypes);

        return blockCache;
    }


    private int generateVein(WorldChunk chunk, BlockCache.BlockCaches cache, int startX, int startY, int startZ,
                             int size, Random rand,BlockCache.OreType specificOre) {
        int placed = 0;
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        int x = startX;
        int y = startY;
        int z = startZ;

        int maxAttempts = size * 3;
        int targetBlocks = size;


        for (int attempt = 0; attempt < maxAttempts && placed < targetBlocks; attempt++) {
            // Try to place ore at current position
            if (y >= CoalOreConfig.WORLD_MIN_Y && y <= CoalOreConfig.WORLD_MAX_Y) {
                if ((x >> CoalOreConfig.CHUNK_SHIFT) == chunkX && (z >> CoalOreConfig.CHUNK_SHIFT) == chunkZ) {
                    if (tryPlaceOre(chunk, cache, x, y, z, specificOre)) {
                        placed++;
                    }
                }
            }

            int direction = rand.nextInt(10);
            switch (direction) {
                case 0, 1 -> x++;
                case 2, 3 -> x--;
                case 4 -> y++;
                case 5 -> y--;
                case 6, 7 -> z++;
                case 8, 9 -> z--;
            }
        }

        return placed;
    }

    private boolean tryPlaceOre(WorldChunk chunk, BlockCache.BlockCaches cache, int x, int y, int z, BlockCache.OreType specificOre) {
        try {
            int currentBlock = chunk.getBlock(x, y, z);

            if (specificOre != null) {
                if (specificOre.canReplace(currentBlock) && specificOre.isValidAtY(y)) {
                    chunk.setBlock(x, y, z, specificOre.id, specificOre.type, 0, 0, 4);
                    return true;
                }
            } else {
                BlockCache.OreType ore = cache.getOreForBlock(currentBlock, y);

                if (ore != null) {
                    chunk.setBlock(x, y, z, ore.id, ore.type, 0, 0, 4);
                    return true;
                }
            }
        } catch (Exception e) {
            // Silently fail
        }
        return false;
    }

    // ========== COMMANDS ==========

    private class CoalOreCommand extends AbstractCommandCollection {
        CoalOreCommand() {
            super("coalore", "Spawn coal ore veins in the world");
            addAliases("co");
            setPermissionGroup(GameMode.Creative);
            addSubCommand(new SpawnCommand());
            addSubCommand(new GenerateCommand());
            addSubCommand(new FillCommand());
            addSubCommand(new ReloadCommand());
        }
    }

    private class SpawnCommand extends AbstractPlayerCommand {
        @Nonnull
        private final DefaultArg<Integer> sizeArg = withDefaultArg(
                "size", "Size of the vein (1-20)", ArgTypes.INTEGER, 8, "Vein size"
        );

        SpawnCommand() {
            super("spawn", "Spawn a coal ore vein at your location");
            setPermissionGroup(GameMode.Creative);
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            Vector3d pos = getPlayerPosition(store, ref);
            if (pos == null) {
                context.sendMessage(Message.raw("Could not get player position!"));
                return;
            }

            int x = (int) pos.x;
            int y = (int) pos.y - 2;
            int z = (int) pos.z;
            int size = clamp(sizeArg.get(context), 1, 20);

            world.execute(() -> {
                int placed = spawnVeinInWorld(world, x, y, z, size);
                context.sendMessage(Message.raw(
                        String.format("Spawned coal ore vein with %d blocks at (%d, %d, %d)", placed, x, y, z)));
            });
        }
    }

    private class GenerateCommand extends AbstractPlayerCommand {
        @Nonnull
        private final DefaultArg<Integer> radiusArg = withDefaultArg(
                "radius", "Radius to generate in", ArgTypes.INTEGER, 32, "Generation radius"
        );
        @Nonnull
        private final DefaultArg<Integer> countArg = withDefaultArg(
                "count", "Number of veins to generate", ArgTypes.INTEGER, 10, "Vein count"
        );

        GenerateCommand() {
            super("generate", "Generate multiple coal ore veins in an area");
            setPermissionGroup(GameMode.Creative);
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            Vector3d pos = getPlayerPosition(store, ref);
            if (pos == null) {
                context.sendMessage(Message.raw("Could not get player position!"));
                return;
            }

            int centerX = (int) pos.x;
            int centerZ = (int) pos.z;
            int radius = clamp(radiusArg.get(context), 1, 128);
            int count = clamp(countArg.get(context), 1, 100);

            context.sendMessage(Message.raw(
                    String.format("Generating %d coal ore veins in radius %d...", count, radius)));

            world.execute(() -> {
                Random rand = ThreadLocalRandom.current();
                int totalPlaced = 0;
                int veinsCreated = 0;

                for (int i = 0; i < count; i++) {
                    int x = centerX + rand.nextInt(radius * 2) - radius;
                    int z = centerZ + rand.nextInt(radius * 2) - radius;
                    int y = 10 + rand.nextInt(50);
                    int size = 4 + rand.nextInt(9);

                    int placed = spawnVeinInWorld(world, x, y, z, size);
                    if (placed > 0) {
                        totalPlaced += placed;
                        veinsCreated++;
                    }
                }

                context.sendMessage(Message.raw(
                        String.format("Generated %d veins with %d total coal ore blocks!", veinsCreated, totalPlaced)));
            });
        }
    }

    private class FillCommand extends AbstractPlayerCommand {
        @Nonnull
        private final DefaultArg<Integer> radiusArg = withDefaultArg(
                "radius", "Radius to fill", ArgTypes.INTEGER, 16, "Fill radius"
        );

        FillCommand() {
            super("fill", "Fill underground area with coal ore veins");
            setPermissionGroup(GameMode.Creative);
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            Vector3d pos = getPlayerPosition(store, ref);
            if (pos == null) {
                context.sendMessage(Message.raw("Could not get player position!"));
                return;
            }

            int centerX = (int) pos.x;
            int centerZ = (int) pos.z;
            int radius = clamp(radiusArg.get(context), 1, 64);

            context.sendMessage(Message.raw(
                    String.format("Filling area with coal ore (radius %d)...", radius)));

            world.execute(() -> {
                Random rand = ThreadLocalRandom.current();
                int totalPlaced = 0;
                int veinsCreated = 0;
                int spacing = 8;

                for (int x = centerX - radius; x <= centerX + radius; x += spacing) {
                    for (int z = centerZ - radius; z <= centerZ + radius; z += spacing) {
                        for (int yBase = 15; yBase <= 55; yBase += 15) {
                            int vx = x + rand.nextInt(spacing) - spacing/2;
                            int vz = z + rand.nextInt(spacing) - spacing/2;
                            int vy = yBase + rand.nextInt(10) - 5;
                            int size = 5 + rand.nextInt(6);

                            int placed = spawnVeinInWorld(world, vx, vy, vz, size);
                            if (placed > 0) {
                                totalPlaced += placed;
                                veinsCreated++;
                            }
                        }
                    }
                }

                context.sendMessage(Message.raw(
                        String.format("Created %d veins with %d total coal ore blocks!", veinsCreated, totalPlaced)));
            });
        }
    }
    private class ReloadCommand extends AbstractPlayerCommand {
        ReloadCommand() {
            super("reload", "Reload the config file");
            setPermissionGroup(GameMode.Creative);
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {

            // Clear cached block data so it reinitializes with new config
            blockCache = null;
            initializationAttempted.set(false);

            config.load().join();

            CoalOreConfig cfg = config.get();

            context.sendMessage(Message.raw("Config reloaded!"));
            context.sendMessage(Message.raw(String.format("  Y range: %d to %d", cfg.getMinY(), cfg.getMaxY())));
            context.sendMessage(Message.raw(String.format("  Veins/chunk: %d (size %d-%d)",
                    cfg.getVeinsPerChunk(), cfg.getMinVeinSize(), cfg.getMaxVeinSize())));
            context.sendMessage(Message.raw(String.format("  Spawn chance: %.0f%%", cfg.getSpawnChance() * 100)));
            //context.sendMessage(Message.raw(String.format("  Ore block: %s", cfg.getCoalOreBlock())));
        }
    }

    // ========== HELPER METHODS ==========

    private Vector3d getPlayerPosition(Store<EntityStore> store, Ref<EntityStore> ref) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        return transform != null ? transform.getPosition() : null;
    }

    private int spawnVeinInWorld(World world, int centerX, int centerY, int centerZ, int size) {
        BlockCache.BlockCaches cache = getBlockCache();
        if (cache == null) {
            return 0;
        }

        Random rand = ThreadLocalRandom.current();
        int placed = 0;

        for (int i = 0; i < size; i++) {
            float progress = (float) i / size;
            float angle1 = rand.nextFloat() * (float) (Math.PI * 2);
            float angle2 = rand.nextFloat() * (float) (Math.PI * 2);

            int x = centerX + (int) (Math.cos(angle1) * progress * 2);
            int y = centerY + (int) (Math.sin(angle1) * Math.cos(angle2) * progress * 2);
            int z = centerZ + (int) (Math.sin(angle2) * progress * 2);

            int clusterRadius = 1 + rand.nextInt(2);

            for (int dx = -clusterRadius; dx <= clusterRadius; dx++) {
                for (int dy = -clusterRadius; dy <= clusterRadius; dy++) {
                    for (int dz = -clusterRadius; dz <= clusterRadius; dz++) {
                        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                        if (dist <= clusterRadius + rand.nextFloat() * 0.5) {
                            int bx = x + dx;
                            int by = y + dy;
                            int bz = z + dz;

                            if (by >= CoalOreConfig.WORLD_MIN_Y && by <= CoalOreConfig.WORLD_MAX_Y) {
                                if (placeOreInWorld(world, cache, bx, by, bz)) {
                                    placed++;
                                }
                            }
                        }
                    }
                }
            }
        }

        return placed;
    }

    private boolean placeOreInWorld(World world, BlockCache.BlockCaches cache, int x, int y, int z) {
        try {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
            WorldChunk chunk = (WorldChunk) world.getNonTickingChunk(chunkIndex);

            if (chunk != null) {
                int currentBlock = chunk.getBlock(x, y, z);

                // Find matching ore for this stone type
                BlockCache.OreType ore = cache.getOreForBlock(currentBlock, y);

                if (ore != null) {
                    chunk.setBlock(x, y, z, ore.id, ore.type, 0, 0, 4);
                    return true;
                }
            }
        } catch (Exception e) {
            // Silently fail
        }
        return false;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
