package com.dragonminez.common.stats;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.character.Stats;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatsLimitsTest {
    private static final String[] STAT_NAMES = {"STR", "SKP", "RES", "VIT", "PWR", "ENE"};

    @AfterEach
    void clearConfig() {
        ConfigManager.clearServerSync();
    }

    @Test
    void existingTenThousandConfigAllowsEveryStatToKeepGrowing() {
        ConfigManager.applySpecificSyncedConfig("general-server",
                """
                {"gameplay": {"maxValue": 10000, "maxLevelValueInsteadOfStats": false}}
                """);
        var data = new StatsData(null);
        for (String name : STAT_NAMES) {
            data.getStats().addStat(name, 10000);
            assertEquals(100, data.getMaxAllowedIncreaseForStat(name, 100), name);
            data.getStats().addStat(name, 100);
            assertEquals(10100, data.getCurrentStatValue(name), name);
        }
        assertFalse(data.hasAttributeLimit());
        assertEquals(25000, data.clampStatToConfiguredMax(25000));
        assertTrue(data.getLevel() > 10000, "Level must continue alongside the six stats");
        var restored = new Stats();
        restored.load(data.getStats().save());
        assertEquals(60600, restored.getTotalStats());
    }

    @Test
    void serversCanExplicitlyEnableTheirConfiguredCap() {
        for (boolean maxByLevel : new boolean[]{false, true}) {
            ConfigManager.applySpecificSyncedConfig("general-server",
                    "{\"gameplay\": {\"maxValue\": 10000, \"attributeLimitEnabled\": true, \"maxLevelValueInsteadOfStats\": " + maxByLevel + "}}");
            var data = new StatsData(null);
            data.getStats().setStrength(20000);
            assertTrue(data.hasAttributeLimit());
            assertEquals(10000, data.getStats().getStrength());
            assertEquals(0, data.getMaxAllowedIncreaseForStat("STR", 1));
            assertEquals(10000, data.clampStatToConfiguredMax(25000));
        }
    }

    @Test
    void totalsBeyondIntegerRangeDoNotLockTheOtherStats() {
        ConfigManager.applySpecificSyncedConfig("general-server", "{}");
        var data = new StatsData(null);
        for (String name : STAT_NAMES) data.getStats().addStat(name, 500_000_000);
        assertEquals(3_000_000_000L, data.getStats().getTotalStatsLong());
        for (String name : STAT_NAMES) assertEquals(100, data.getMaxAllowedIncreaseForStat(name, 100));
        assertTrue(data.getLevel() > 10000);
        data.getStats().setStrength(Integer.MAX_VALUE - 5);
        assertEquals(5, data.getMaxAllowedIncreaseForStat("STR", 100));
        data.getStats().addStrength(100);
        assertEquals(Integer.MAX_VALUE, data.getStats().getStrength());
        assertEquals(0, data.getMaxAllowedIncreaseForStat("STR", 1));
        assertEquals(100, data.getMaxAllowedIncreaseForStat("ENE", 100));
    }

    @Test
    void largeBatchCostsStayPositiveAndCannotBuyUnaffordableStats() {
        ConfigManager.applySpecificSyncedConfig("general-server", "{}");
        var data = new StatsData(null);
        for (String name : STAT_NAMES) data.getStats().addStat(name, 500_000_000);
        long batchCost = data.calculateStatCost(100);
        assertTrue(batchCost > Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, data.calculateRecursiveCost(100, 10000));
        int onePointCost = data.getSingleStatCost(data.getStats().getTotalStatsLong());
        assertEquals(1, data.calculateStatIncrease(100, onePointCost * 1.5f, 10000));
        assertEquals(0, data.calculateStatIncrease(100, onePointCost * 0.5f, 10000));
    }

    @Test
    void disablingCapDoesNotChangeExistingTrainingCostCurve() {
        ConfigManager.applySpecificSyncedConfig("general-server",
                "{\"gameplay\": {\"attributeLimitEnabled\": true}}");
        var data = new StatsData(null);
        data.getStats().setStrength(5000);
        int singleCost = data.getSingleStatCost(5000);
        long batchCost = data.calculateStatCost(100);
        double gainMultiplier = data.getProgressionTpGainMultiplier();
        ConfigManager.applySpecificSyncedConfig("general-server", "{}");
        assertEquals(singleCost, data.getSingleStatCost(5000));
        assertEquals(batchCost, data.calculateStatCost(100));
        assertEquals(gainMultiplier, data.getProgressionTpGainMultiplier());
    }
}
