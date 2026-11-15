package com.dragonminez.common.init.item;

import net.minecraft.world.item.component.TooltipDisplay;
import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonRadarDefinition;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.quest.QuestUnlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;

@ParametersAreNonnullByDefault
public class DragonRadarItem extends Item {
	public static final String NBT_RANGE = "RadarRange";
	private static final int COOLDOWN_TICKS = 20 * 16;
	/**
	 * Bulma sidequest ids that gate radar upgrades.
	 */
	public static final String QUEST_RADAR_AMPLIFIER = "bulma_radar_amplifier";
	public static final String QUEST_PROXIMITY_HUD = "bulma_proximity_hud";
	/**
	 * Extra long-range tier unlocked by the radar amplifier sidequest.
	 */
	private static final int AMPLIFIED_RANGE = 600;

	/**
	 * Whether the given player has completed a quest --works on both sides (stats are synced).
	 */
	public static boolean hasCompletedQuest(Player player, String questId) {
		return QuestUnlocks.isCompleted(player, questId);
	}

	private final String radarDefinitionId;

	public DragonRadarItem(String radarDefinitionId) {
		this(radarDefinitionId, new net.minecraft.world.item.Item.Properties().stacksTo(1));
	}

	/** 26.2: the registry id must be on the properties before super() runs. */
	public DragonRadarItem(String radarDefinitionId, net.minecraft.world.item.Item.Properties properties) {
		super(properties);
		this.radarDefinitionId = radarDefinitionId;
	}

	public DragonRadarDefinition getDefinition() {
		return DragonBallDefinitions.getRadar(radarDefinitionId);
	}

	@Override
	public InteractionResult use(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (hand == InteractionHand.OFF_HAND && !player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
			return InteractionResult.FAIL;
		}
		if (player.getCooldowns().isOnCooldown(new ItemStack(this))) {
			return InteractionResult.FAIL;
		}
		player.playSound(MainSounds.DRAGONRADAR.get());
		if (!world.isClientSide()) {
			DragonRadarDefinition definition = getDefinition();
			int currentRange = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr(NBT_RANGE, 0);
			int[] ranges = definition == null ? new int[0] : effectiveRanges(definition, player);
			int newRange = ranges.length == 0 ? currentRange : getNextRange(ranges, currentRange);
			CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(NBT_RANGE, newRange));
			player.sendSystemMessage(Component.translatable("gui.dmzradar.range", newRange));
		}
		player.getCooldowns().addCooldown(new ItemStack(this), COOLDOWN_TICKS);
		return world.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}

	private int[] effectiveRanges(DragonRadarDefinition definition, Player player) {
		int[] base = definition.getRanges();
		if (!hasCompletedQuest(player, QUEST_RADAR_AMPLIFIER)) return base;
		for (int r : base) {
			if (r == AMPLIFIED_RANGE) return base; // already present (e.g. fused radar)
		}
		int[] extended = Arrays.copyOf(base, base.length + 1);
		extended[base.length] = AMPLIFIED_RANGE;
		return extended;
	}

	private int getNextRange(int[] ranges, int currentRange) {
		if (ranges.length == 0) return currentRange;
		for (int i = 0; i < ranges.length; i++) {
			if (ranges[i] == currentRange) {
				return ranges[(i + 1) % ranges.length];
			}
		}
		return ranges[0];
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, java.util.function.Consumer<Component> tooltip, TooltipFlag isAdvanced) {
		DragonRadarDefinition definition = getDefinition();
		if (definition != null) {
			tooltip.accept(Component.translatable(definition.getTooltipKey()).withStyle(ChatFormatting.GRAY));
		}
	}

	@java.lang.SuppressWarnings("all")
	public String getRadarDefinitionId() {
		return this.radarDefinitionId;
	}
}


