package com.dragonminez.common.util;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.Saga;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class TransformationRequirementsTest {
    @AfterEach
    void clearSyncedData() {
        QuestRegistry.applySyncedSagas(Map.of());
        ConfigManager.clearServerSync();
    }

    @Test
    void zeroMasteryWithIncompleteStoryReportsTheStoryRequirement() {
        StatsData data = character("\"sagaRequisite\":\"android_saga:4\",\"unlockOnMastery\":0");
        syncAndroidSaga();

        assertFalse(TransformationsHelper.isNextFormMasteryBlocked(data));
        var message = translation(TransformationsHelper.getNextFormRequirementMessage(data, true));
        assertEquals("message.dragonminez.form.requisites", message.getKey());
        var description = translation((Component) message.getArgs()[1]);
        assertEquals("gui.dragonminez.skills.locked_until", description.getKey());
        var reference = translation((Component) ((Component) description.getArgs()[0]).getSiblings().getFirst());
        assertEquals("gui.dragonminez.skills.quest_ref", reference.getKey());
        assertEquals(4, reference.getArgs()[1]);

        data.getPlayerQuestData().completeQuest(PlayerQuestData.sagaQuestKey("android_saga", 4));
        assertNull(TransformationsHelper.getNextFormRequirementMessage(data, true));
    }

    @Test
    void levelLocksRemainEnforcedAndAreNotReportedAsZeroMastery() {
        StatsData data = character("\"levelRequisite\":100,\"unlockOnMastery\":0");
        assertNull(TransformationsHelper.getNextAvailableForm(data));
        assertFalse(TransformationsHelper.isNextFormMasteryBlocked(data));
        assertEquals("message.dragonminez.form.requisites",
                translation(TransformationsHelper.getNextFormRequirementMessage(data, true)).getKey());

        ConfigManager.applySpecificSyncedConfig("general-server", "{\"gameplay\":{\"levelFormRequisiteEnabled\":false}}");
        assertNotNull(TransformationsHelper.getNextAvailableForm(data));
        assertNull(TransformationsHelper.getNextFormRequirementMessage(data, true));
    }

    @Test
    void positiveFractionalMasteryIsDisplayedWithoutTruncatingToZero() {
        StatsData data = character("\"formRequisite\":\"ssgrades.supersaiyan\",\"unlockOnMastery\":0.25");
        assertTrue(TransformationsHelper.isNextFormMasteryBlocked(data));
        assertNull(TransformationsHelper.getNextAvailableForm(data));
        var message = translation(TransformationsHelper.getNextFormRequirementMessage(data, true));
        assertEquals("message.dragonminez.form.no_mastery", message.getKey());
        assertEquals("0.25", message.getArgs()[0]);

        data.getCharacter().getFormMasteries().setMastery("ssgrades", "supersaiyan", 0.25, 100);
        assertFalse(TransformationsHelper.isNextFormMasteryBlocked(data));
        assertNotNull(TransformationsHelper.getNextAvailableForm(data));
        assertNull(TransformationsHelper.getNextFormRequirementMessage(data, true));
    }

    @Test
    void zeroMasteryWithoutOtherLocksAllowsTheTransformation() {
        StatsData data = character("\"formRequisite\":\"ssgrades.supersaiyan\",\"unlockOnMastery\":0");
        assertFalse(TransformationsHelper.isNextFormMasteryBlocked(data));
        assertNotNull(TransformationsHelper.getNextAvailableForm(data));
        assertNull(TransformationsHelper.getNextFormRequirementMessage(data, true));
    }

    @Test
    void disabledStoryRequirementsAllowOldCharactersToTransform() {
        StatsData data = character("\"sagaRequisite\":\"android_saga:4\"");
        syncAndroidSaga();
        for (String option : List.of("sagaFormRequisiteEnabled", "storyModeEnabled")) {
            ConfigManager.applySpecificSyncedConfig("general-server", "{\"gameplay\":{\"" + option + "\":false}}");
            assertNull(TransformationsHelper.getNextFormRequirementMessage(data, true), option);
        }
    }

    @Test
    void crossGroupErrorNamesTheTargetFormAndPreservesItsMasteryRequirement() {
        StatsData data = character("\"formRequisite\":\"ssgrades.supersaiyan\",\"unlockOnMastery\":25");
        data.getCharacter().setActiveForm("ssgrades", "supersaiyan");
        var message = translation(TransformationsHelper.getNextFormRequirementMessage(data, true));
        assertEquals("25", message.getArgs()[0]);
        assertEquals("race.dragonminez.saiyan.form.supersaiyan.supersaiyanmastered",
                translation((Component) message.getArgs()[1]).getKey());
    }

    private static StatsData character(String requirements) {
        ConfigManager.beginServerSyncBatch();
        ConfigManager.applySpecificSyncedConfig("general-server", "{}");
        ConfigManager.applySpecificSyncedConfig("races/saiyan/character", "{}");
        ConfigManager.applySpecificSyncedConfig("races/saiyan/forms/supersaiyan", """
                {"groupName":"supersaiyan","formType":"superforms","forms":{
                  "supersaiyanmastered":{"name":"supersaiyanmastered","unlockOnSkillLevel":4,
                    "allowFreeTransformOnMastery":0,
                """ + requirements + "}}}");
        var data = new StatsData(null);
        data.getCharacter().setRace("saiyan");
        data.getCharacter().setSelectedFormGroup("supersaiyan");
        data.getCharacter().setSelectedForm("supersaiyanmastered");
        data.getSkills().registerDefaultSkill("superforms", 10);
        data.getSkills().setSkillLevel("superforms", 4);
        return data;
    }

    private static void syncAndroidSaga() {
        var quests = IntStream.rangeClosed(1, 4).mapToObj(id -> new Quest(id, null, Quest.QuestType.SAGA,
                "dmz.quest.android" + id + ".name", "", "general", false, false,
                List.of(), List.of(), null, null, null, null)).toList();
        QuestRegistry.applySyncedSagas(Map.of("android_saga", new Saga("android_saga", "dmz.saga.android", quests, null)));
    }

    private static TranslatableContents translation(Component message) {
        assertNotNull(message);
        return assertInstanceOf(TranslatableContents.class, message.getContents());
    }
}
