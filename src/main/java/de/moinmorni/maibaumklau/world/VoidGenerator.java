package de.moinmorni.maibaumklau.world;

import java.util.List;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

/** Leere Welt: nur Himmel. Wirtsgarten und Karten baut das Plugin selbst. */
public final class VoidGenerator extends ChunkGenerator {

    private static final BiomeProvider PLAINS = new BiomeProvider() {
        @Override
        public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
            return Biome.PLAINS;
        }

        @Override
        public List<Biome> getBiomes(WorldInfo worldInfo) {
            return List.of(Biome.PLAINS);
        }
    };

    @Override
    public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
        return PLAINS;
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, HallPlan.SPAWN_X, MapType.FLOOR_Y + HallPlan.SPAWN_Y, HallPlan.SPAWN_Z,
                HallPlan.SPAWN_YAW, 0f);
    }
}
