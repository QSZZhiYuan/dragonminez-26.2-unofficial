package com.dragonminez.common.init.entities.goals;

import com.dragonminez.common.init.entities.namek.NamekWarriorEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class VillageAlertSystem {

    // Entity IDs are assigned after construction and can change during loading.
    private static final Set<NamekWarriorEntity> warriors = Collections.newSetFromMap(new IdentityHashMap<>());

    public static void registerWarrior(NamekWarriorEntity warrior) {
        if (!warrior.level().isClientSide()) warriors.add(warrior);
    }

    public static void unregisterWarrior(NamekWarriorEntity warrior) {
        warriors.remove(warrior);
    }


    public static void alertAll(Player player) {
        for (NamekWarriorEntity warrior : warriors) {
            if (warrior.isAlive() && warrior.level() == player.level()) warrior.setTarget(player);
        }
    }

}

