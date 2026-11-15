package com.dragonminez.common.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RaceConfigSyncTest {
    @AfterEach
    void clearSync() {
        ConfigManager.clearServerSync();
    }

    @Test
    void nestedServerCharacterPathsPopulateOnlyTheRacesSentByTheServer() {
        ConfigManager.beginServerSyncBatch();
        assertTrue(ConfigManager.getLoadedRaces().isEmpty());
        ConfigManager.applySpecificSyncedConfig("races/saiyan/character", "{}");
        ConfigManager.applySpecificSyncedConfig("races/namekian/character", "{}");
        assertEquals(java.util.List.of("saiyan", "namekian"), ConfigManager.getLoadedRaces());
        assertTrue(ConfigManager.isRaceLoaded("saiyan"));
        assertFalse(ConfigManager.isRaceLoaded("human"));
    }

    @Test
    void nextServerSyncBatchClearsThePreviousServersRaces() {
        ConfigManager.beginServerSyncBatch();
        ConfigManager.applySpecificSyncedConfig("races/saiyan/character", "{}");
        ConfigManager.beginServerSyncBatch();
        ConfigManager.applySpecificSyncedConfig("races/majin/character", "{}");
        assertEquals(java.util.List.of("majin"), ConfigManager.getLoadedRaces());
    }
}
