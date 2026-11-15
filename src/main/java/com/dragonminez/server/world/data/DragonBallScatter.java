package com.dragonminez.server.world.data;

import net.minecraft.core.BlockPos;

import java.util.Random;
import java.util.Set;

/** Assigns distinct surface columns even when a pack configures a very small spawn range. */
public final class DragonBallScatter {
    private DragonBallScatter() {}

    public static BlockPos reserve(Random random, BlockPos spawn, int range, Set<BlockPos> occupied) {
        int radius = Math.clamp(range, 1, 29_000_000);
        for (int attempt = 0; attempt < 64; attempt++) {
            BlockPos candidate = new BlockPos(spawn.getX() + random.nextInt(-radius, radius + 1),
                    0, spawn.getZ() + random.nextInt(-radius, radius + 1));
            if (occupied.add(candidate)) return candidate;
        }
        // Exhausted tiny ranges expand instead of stacking balls on top of each other.
        for (int r = 0; ; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != r) continue;
                    BlockPos candidate = new BlockPos(spawn.getX() + x, 0, spawn.getZ() + z);
                    if (occupied.add(candidate)) return candidate;
                }
            }
        }
    }
}
