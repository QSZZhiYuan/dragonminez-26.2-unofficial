package com.dragonminez.server.world.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KaioPlanetProtectionTest {
    @Test
    void protectsTheMasterTheFarSideAndTheUndersideOfTheBundledPlanet() {
        for (BlockPos pos : new BlockPos[]{new BlockPos(54, 190, 1082),
                new BlockPos(-11, 128, 1027), new BlockPos(129, 128, 1167),
                new BlockPos(59, 30, 1097), new BlockPos(59, 300, 1097)}) {
            assertTrue(KaioPlanetProtection.isProtected(OtherworldDimension.OTHERWORLD_KEY, pos), pos.toString());
        }
    }

    @Test
    void leavesTheRestOfOtherworldEditable() {
        for (BlockPos pos : new BlockPos[]{new BlockPos(0, 41, 10),
                new BlockPos(-17, 190, 1097), new BlockPos(135, 190, 1097),
                new BlockPos(59, 190, 1021), new BlockPos(59, 190, 1173)}) {
            assertFalse(KaioPlanetProtection.isProtected(OtherworldDimension.OTHERWORLD_KEY, pos), pos.toString());
        }
    }

    @Test
    void doesNotProtectMatchingCoordinatesInOtherDimensions() {
        BlockPos island = new BlockPos(54, 190, 1082);
        assertFalse(KaioPlanetProtection.isProtected(dimension("overworld"), island));
        assertFalse(KaioPlanetProtection.isProtected(dimension("the_nether"), island));
        assertFalse(KaioPlanetProtection.isProtected(dimension("the_end"), island));
    }

    private static ResourceKey<Level> dimension(String name) {
        return ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("minecraft", name));
    }
}
