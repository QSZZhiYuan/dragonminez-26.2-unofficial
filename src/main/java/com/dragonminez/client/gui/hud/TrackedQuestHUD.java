package com.dragonminez.client.gui.hud;

import com.dragonminez.Reference;
import com.dragonminez.client.util.LocalizationUtil;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.QuestTextFormatter;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.gui.GuiLayer;

import java.util.ArrayList;
import java.util.List;

public class TrackedQuestHUD {

	private static final int PANEL_WIDTH = 180;
	private static final int MAX_TEXT_WIDTH = PANEL_WIDTH - 16;

    public static NotificationHUD.Content notification() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        var data = StatsProvider.get(StatsCapability.INSTANCE, mc.player).orElse(null);
        if (data == null) return null;
        PlayerQuestData pqd = data.getPlayerQuestData();
        String id = pqd.getTrackedQuestId();
        if (id == null || id.isBlank() || !pqd.isQuestAccepted(id) || pqd.isQuestCompleted(id)) return null;
        Quest quest = QuestRegistry.getClientQuest(id);
        if (quest == null) return null;
        var description = Component.empty();
        boolean any = false;
        for (int i = 0; i < quest.getObjectives().size(); i++) {
            QuestObjective objective = quest.getObjectives().get(i);
            int progress = pqd.getObjectiveProgress(id, i), required = quest.getObjectiveRequired(pqd, id, i);
            if (progress >= required) continue;
            if (any) description.append("\n");
            description.append("- ").append(QuestTextFormatter.describeObjective(objective))
                    .append(" (" + progress + "/" + required + ")");
            any = true;
            if (!quest.isParallelObjectives()) break;
        }
        if (!any) description.append(Component.translatable("gui.dragonminez.quests.status.complete"));
        return NotificationHUD.Content.of(toComponent(quest.getTitle()),
                Component.translatable("gui.dragonminez.story.hud.tracked"), description, null);
    }

	private static List<FormattedCharSequence> buildObjectiveLines(Font font, PlayerQuestData pqd, String questId, Quest quest) {
		List<FormattedCharSequence> lines = new ArrayList<>();
		List<QuestObjective> objectives = quest.getObjectives();
		if (objectives.isEmpty()) {
			lines.add(Component.translatable("gui.dragonminez.story.hud.no_objectives").getVisualOrderText());
			return lines;
		}

		for (int i = 0; i < objectives.size(); i++) {
			QuestObjective objective = objectives.get(i);
			int progress = pqd.getObjectiveProgress(questId, i);
			int required = quest.getObjectiveRequired(pqd, questId, i);
			if (!quest.isParallelObjectives()) {
				if (progress >= required) continue;
				lines.addAll(splitObjectiveLine(font, objective, progress, required));
				return lines;
			}

			if (progress < required) {
				lines.addAll(splitObjectiveLine(font, objective, progress, required));
			}
		}

		if (lines.isEmpty()) {
			lines.add(Component.translatable("gui.dragonminez.quests.status.complete").getVisualOrderText());
		}

		return lines;
	}

	private static List<FormattedCharSequence> splitObjectiveLine(Font font, QuestObjective objective, int progress, int required) {
		Component text = Component.literal("- ")
				.append(QuestTextFormatter.describeObjective(objective))
				.append(Component.literal(" (" + progress + "/" + required + ")"));
		return font.split(text, MAX_TEXT_WIDTH);
	}

	private static Component toComponent(String raw) {
		return LocalizationUtil.localizedOrReadable(raw);
	}
}

