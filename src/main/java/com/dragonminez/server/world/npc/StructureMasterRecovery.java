package com.dragonminez.server.world.npc;

import com.dragonminez.Reference;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Reference.MOD_ID)
public final class StructureMasterRecovery {
    @SubscribeEvent
    public static void tick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % 100 == 0)
            NPCPlacementManager.recoverLookoutMasters(level);
    }
}
