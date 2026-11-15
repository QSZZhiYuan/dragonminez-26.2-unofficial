package com.dragonminez.server.world.dimension;

import com.dragonminez.Reference;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/** Protects King Kai's bundled island, which has no generated StructureStart. */
@EventBusSubscriber(modid = Reference.MOD_ID)
public final class KaioPlanetProtection {
    // Bundled r.0.2/r.-1.2: X -11..129, Z 1027..1167, plus a five-block margin.
    // Protect the entire column so building above/below cannot undermine the island.
    private static final int MIN_X = -16;
    private static final int MAX_X = 134;
    private static final int MIN_Z = 1022;
    private static final int MAX_Z = 1172;

    private KaioPlanetProtection() {}

    public static boolean isProtected(LevelAccessor level, BlockPos pos) {
        return level instanceof Level world && isProtected(world.dimension(), pos);
    }

    static boolean isProtected(ResourceKey<Level> dimension, BlockPos pos) {
        return OtherworldDimension.OTHERWORLD_KEY.equals(dimension)
                && pos.getX() >= MIN_X && pos.getX() <= MAX_X
                && pos.getZ() >= MIN_Z && pos.getZ() <= MAX_Z;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BreakBlockEvent event) {
        if (isProtected(event.getLevel(), event.getPos())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (isProtected(event.getLevel(), event.getPos())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMultiPlace(BlockEvent.EntityMultiPlaceEvent event) {
        for (var snapshot : event.getReplacedBlockSnapshots()) {
            if (isProtected(event.getLevel(), snapshot.getPos())) {
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos -> isProtected(event.getLevel(), pos));
    }

    @SubscribeEvent
    public static void onPistonMove(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof Level level)
                || !OtherworldDimension.OTHERWORLD_KEY.equals(level.dimension())) return;
        if (isProtected(level, event.getPos()) || isProtected(level, event.getFaceOffsetPos())) {
            event.setCanceled(true);
            return;
        }

        var resolver = event.getStructureHelper();
        if (resolver == null || !resolver.resolve()) return;
        var direction = event.getPistonMoveType().isExtend
                ? event.getDirection() : event.getDirection().getOpposite();
        for (BlockPos pos : resolver.getToPush()) {
            if (isProtected(level, pos) || isProtected(level, pos.relative(direction))) {
                event.setCanceled(true);
                return;
            }
        }
        for (BlockPos pos : resolver.getToDestroy()) {
            if (isProtected(level, pos)) {
                event.setCanceled(true);
                return;
            }
        }
    }
}
