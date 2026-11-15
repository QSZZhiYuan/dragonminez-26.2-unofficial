package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.hud.HudStatNumberAnimator;
import com.dragonminez.client.render.util.RenderBufferUtil;
import com.dragonminez.client.systems.kisense.CombatIndicators;
import com.dragonminez.client.systems.kisense.KiSenseScan;
import com.dragonminez.client.systems.kisense.KiSenseState;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.dragonminez.client.render.util.ModRenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import org.joml.Matrix4f;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class KiSenseEvent {
	private static final Identifier HUD_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/alternativehud.png");
	private static final Identifier DMZ_FONT = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "smooth");
	static NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

	private static final double LOD_DISTANCE = 24.0;
	/** How far above an entity's hitbox the combat overlay floats, in blocks. */
	private static final double OVERLAY_HEIGHT_ABOVE_ENTITY = 0.8;
	private static final float BAR_MAX_WIDTH = 76.0f;
	private static final float BAR_STACK = 10.0f;
	private static final int DAMAGE_COLOR = 0xFF5555;
	private static final int HEAL_COLOR = 0x55FF55;

	/** One entity's overlay, captured while that entity renders and drawn once the level is done. */
	private record PendingOverlay(LivingEntity entity, Matrix4f billboard, float partialTick) {}

	private static final List<PendingOverlay> pendingOverlays = new ArrayList<>();
	private static final Map<Integer, HudStatNumberAnimator> healthAnimators = new HashMap<>();
	private static final Map<Integer, Float> lerpedHealthWidths = new HashMap<>();
	/** Entity remembered between CanRender and DoRender (26.2's render state carries no entity id). */
	private static LivingEntity pendingEntity;

	static {
		numberFormat.setMaximumFractionDigits(1);
		numberFormat.setMinimumFractionDigits(0);
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null || mc.level == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) {
				KiSenseScan.clear();
				CombatIndicators.clear();
				return;
			}
			if (KiSenseState.isActive() && (!data.getSkills().isSkillActive("kisense") || data.getSkills().getSkillLevel("kisense") <= 0)) {
				KiSenseState.reset();
			}
			if (KiSenseState.isActive() && KiSenseScan.hasScouter(player)) {
				KiSenseState.set(KiSenseState.Mode.NONE);
				return;
			}
			KiSenseScan.tick(player, data, KiSenseState.getMode());
			if (KiSenseState.isCombat()) CombatIndicators.tick();
			else {
				CombatIndicators.clear();
			}
		});

		lerpedHealthWidths.keySet().retainAll(KiSenseScan.getCombatEntities());
		healthAnimators.keySet().retainAll(KiSenseScan.getCombatEntities());
	}

	/**
	 * Draws the combat overlay for one sensed entity.
	 *
	 * <h2>26.2 migration</h2>
	 * 1.21 had to <em>capture</em> the entity-local pose from {@code RenderNameTagEvent} and
	 * <em>replay</em> it from {@code RenderLevelStageEvent.AfterLevel}: that stage offered no
	 * collector, so the only way to draw after the opaque pass was to stash a matrix. The capture
	 * was keyed on the live entity, which 26.2's {@link EntityRenderState} no longer references.
	 *
	 * <p>26.2 makes the dance unnecessary. {@link RenderNameTagEvent.CanRender} still carries the
	 * live {@link Entity} and fires immediately before {@link RenderNameTagEvent.DoRender} for the
	 * same entity, so the entity is remembered there and consumed by the very next {@code DoRender}
	 * — which supplies the pose stack, a {@link SubmitNodeCollector} and the camera render state.
	 * Depth behaviour now belongs to the {@link RenderType}'s pipeline, not global render state.
	 */
	@SubscribeEvent
	public static void onRenderNameTagCanRender(RenderNameTagEvent.CanRender event) {
		pendingEntity = null;
		if (!KiSenseState.isCombat()) return;
		if (!(event.getEntity() instanceof LivingEntity living) || !living.isAlive()) return;
		if (!KiSenseScan.getCombatEntities().contains(living.getId())) return;
		pendingEntity = living;
	}

	@SubscribeEvent
	public static void onRenderNameTag(RenderNameTagEvent.DoRender event) {
		LivingEntity living = pendingEntity;
		pendingEntity = null;
		if (living == null) return;
		if (!KiSenseState.isCombat()) return;
		if (!living.isAlive()) return;

		PoseStack poseStack = event.getPoseStack();
		SubmitNodeCollector collector = event.getSubmitNodeCollector();
		RenderType renderType = ModRenderTypes.auraBillboard(HUD_TEXTURE);

		poseStack.pushPose();
		RenderBufferUtil.nameplateBillboard(poseStack, living, OVERLAY_HEIGHT_ABOVE_ENTITY);
		renderCombatOverlay(collector, poseStack, renderType, living, event.getPartialTick());
		poseStack.popPose();
	}

	private static void renderCombatOverlay(SubmitNodeCollector collector, PoseStack poseStack,
			RenderType renderType, LivingEntity entity, float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		boolean lod = mc.player != null && mc.player.distanceTo(entity) > LOD_DISTANCE;

		float topY;
		if (entity instanceof Player target) {
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, target).orElse(null);
			if (data != null) {
				drawStaminaBar(collector, poseStack, renderType, data, 0f);
				drawKiBar(collector, poseStack, renderType, data, -BAR_STACK);
			}
			renderHealthBar(collector, poseStack, renderType, entity, partialTick, lod, -2f * BAR_STACK);
			topY = -2f * BAR_STACK;
		} else {
			renderHealthBar(collector, poseStack, renderType, entity, partialTick, lod, 0f);
			topY = 0f;
		}

		renderBPLabel(collector, poseStack, entity, topY);
		renderIndicators(collector, poseStack, entity, partialTick, lod, topY);
	}

	private static void drawKiBar(SubmitNodeCollector collector, PoseStack poseStack, RenderType renderType, StatsData data, float baseY) {
		float maxKi = Math.max(1, data.getMaxEnergy());
		float pct = Mth.clamp(data.getResources().getCurrentEnergy() / maxKi, 0f, 1f);
		int fillW = 7 + (int) (BAR_MAX_WIDTH * pct);
		float x = -41.5f;

		poseStack.pushPose();
		poseStack.translate(0f, baseY, 0f);

		drawTexture(collector, poseStack, renderType, x, 0, 83, 9, 0, 44);

		float[] auraRgb = auraColorRgb(data);
		int tint = net.minecraft.util.ARGB.colorFromFloat(1.0F, auraRgb[0], auraRgb[1], auraRgb[2]);
		drawTexture(collector, poseStack, renderType, x + 3, 3, fillW, 4, 3, 61, tint);

		poseStack.popPose();
	}

	private static void drawStaminaBar(SubmitNodeCollector collector, PoseStack poseStack, RenderType renderType,
			StatsData data, float baseY) {
		float maxStm = Math.max(1, data.getMaxStamina());
		float pct = Mth.clamp(data.getResources().getCurrentStamina() / maxStm, 0f, 1f);
		int fillW = (int) (BAR_MAX_WIDTH * pct) - 5;
		float x = -41.5f;

		poseStack.pushPose();
		poseStack.translate(0f, baseY, 0f);

		drawTexture(collector, poseStack, renderType, x, 0, 83, 9, 0, 72);
		if (fillW > 0) drawTexture(collector, poseStack, renderType, x + 2, 3, fillW, 4, 2, 90);
		drawTexture(collector, poseStack, renderType, x + 77, 3, 4, 4, 77, 90);

		poseStack.popPose();
	}

	private static float[] auraColorRgb(StatsData data) {
		com.dragonminez.common.stats.character.Character character = data.getCharacter();
		String auraColor = character.getAuraColor();
		com.dragonminez.common.config.FormConfig.FormData formData =
				character.getActiveStackForm() != null && !character.getActiveStackForm().isEmpty() ? character.getActiveStackFormData() :
						character.getActiveForm() != null && !character.getActiveForm().isEmpty() ? character.getActiveFormData() : null;
		if (formData != null && formData.getAuraColor() != null && !formData.getAuraColor().isEmpty()) auraColor = formData.getAuraColor();
		return com.dragonminez.client.util.ColorUtils.hexToRgb(auraColor != null ? auraColor : "#FFFFFF");
	}

	private static void renderHealthBar(SubmitNodeCollector collector, PoseStack poseStack, RenderType renderType,
			LivingEntity entity, float partialTick, boolean lod, float baseY) {
		Minecraft mc = Minecraft.getInstance();

		float health = entity.getHealth();
		float maxHealth = entity.getMaxHealth();
		float healthPercent = Math.max(0, Math.min(1, health / maxHealth));

		poseStack.pushPose();
		poseStack.translate(0f, baseY, 0f);

		float width = 83;
		float x = -width / 2.0f;
		float y = 0;

		drawTexture(collector, poseStack, renderType, x, y, (int) width, 9, 0, 0);

		float targetBarWidth = 76 * healthPercent;
		int entityId = entity.getId();
		float currentBarWidth;
		if (lod) {
			currentBarWidth = targetBarWidth;
		} else {
			currentBarWidth = lerpedHealthWidths.getOrDefault(entityId, targetBarWidth);
			currentBarWidth += (targetBarWidth - currentBarWidth) * 0.25f * partialTick;
			if (Math.abs(currentBarWidth - targetBarWidth) <= 0.5f) currentBarWidth = targetBarWidth;
		}
		lerpedHealthWidths.put(entityId, currentBarWidth);

		int fillV;
		if (healthPercent < 0.33f) fillV = 33;
		else if (healthPercent < 0.66f) fillV = 22;
		else fillV = 11;

		if (currentBarWidth > 0) {
			drawTexture(collector, poseStack, renderType, x + 2, y + 3, 7 + (int) currentBarWidth, 5, 2, fillV);
		}

		boolean showPercent = ConfigManager.getUserConfig().getAdvancedDescriptionPercentage();
		String textStr = showPercent
				? String.format("%.0f", health / maxHealth * 100) + "%"
				: numberFormat.format(health) + " / " + numberFormat.format(maxHealth);
		MutableComponent text = txt(textStr);

		poseStack.pushPose();
		float textScale = 0.6f;
		poseStack.scale(textScale, textScale, textScale);
		float textY = 3.0f;

		float textWidth = mc.font.width(text);
		float textX = -textWidth / 2.0f;

		if (lod) {
			drawText(poseStack, text, textX, textY, HudStatNumberAnimator.WHITE_RGB, 1.0f, collector);
					} else {
			HudStatNumberAnimator animator = healthAnimators.computeIfAbsent(entityId, id -> new HudStatNumberAnimator(HudStatNumberAnimator.StatKind.KISENSE_HEALTH));
			float value = showPercent ? Math.round((health / maxHealth) * 100.0f) : Math.round(health);
			HudStatNumberAnimator.RenderState state = animator.update(textStr, value, mc.player.tickCount + partialTick);
			if (!state.isHidden()) {
				poseStack.translate(state.offsetX(), state.offsetY(), 0);
				drawText(poseStack, text, textX, textY, state.rgbColor(), state.alpha(), collector);
							}
		}
		poseStack.popPose();
		poseStack.popPose();
	}

	private static void renderIndicators(SubmitNodeCollector collector, PoseStack poseStack, LivingEntity entity, float partialTick, boolean lod, float topY) {
		Minecraft mc = Minecraft.getInstance();
		int id = entity.getId();
		float time = mc.level.getGameTime() + partialTick;
		float maxHealth = entity.getMaxHealth();


		poseStack.pushPose();
		poseStack.translate(0f, topY, 0f);
		float textScale = 1.05f;
		poseStack.scale(textScale, textScale, textScale);

		if (ConfigManager.getUserConfig().getShowAccumulativeDamage()) {
			float damage = CombatIndicators.getAccumDamage(id);
			float heal = CombatIndicators.getAccumHeal(id);
			if (damage > 0) {
				float alpha = lod ? 1.0f : accumAlpha(time - CombatIndicators.getDamageTick(id));
				drawIndicator(poseStack, formatDelta(damage, false, maxHealth), CombatIndicators.getDamageOffX(id), CombatIndicators.getDamageOffY(id), DAMAGE_COLOR, alpha, collector);
			}
			if (heal > 0) {
				float alpha = lod ? 1.0f : accumAlpha(time - CombatIndicators.getHealTick(id));
				drawIndicator(poseStack, formatDelta(heal, true, maxHealth), CombatIndicators.getHealOffX(id), CombatIndicators.getHealOffY(id), HEAL_COLOR, alpha, collector);
			}
		} else {
			for (CombatIndicators.DamagePopup popup : CombatIndicators.getPopups(id)) {
				float age = time - popup.bornTick();
				float life = Mth.clamp(age / CombatIndicators.POPUP_LIFETIME, 0f, 1f);
				float alpha = lod ? 1.0f : (1.0f - life);
				float rise = lod ? 0f : life * 10f;
				int color = popup.heal() ? HEAL_COLOR : DAMAGE_COLOR;
				drawIndicator(poseStack, formatDelta(popup.value(), popup.heal(), maxHealth), popup.offX(), popup.offY() - rise, color, alpha, collector);
			}
		}

				poseStack.popPose();
	}

	private static String formatDelta(float amount, boolean heal, float maxHealth) {
		String sign = heal ? "+" : "-";
		if (ConfigManager.getUserConfig().getAdvancedDescriptionPercentage() && maxHealth > 0) {
			return sign + String.format("%.0f", amount / maxHealth * 100f) + "%";
		}
		return sign + Math.round(amount);
	}

	private static void renderBPLabel(SubmitNodeCollector collector, PoseStack poseStack, LivingEntity entity, float topY) {
		Minecraft mc = Minecraft.getInstance();
		float bp = KiSenseScan.getCachedBP(entity.getId());

		boolean isPlayer = entity instanceof Player;
		boolean isMaxed = isPlayer ? bp >= Float.MAX_VALUE : bp >= Integer.MAX_VALUE;

		String bpStr = isMaxed ? "BP: ???" : "BP: " + String.format("%,.0f", bp).replace(",", ".");
		MutableComponent text = txt(bpStr);

		poseStack.pushPose();
		poseStack.translate(0f, topY, 0f);
		float textScale = 0.6f;
		poseStack.scale(textScale, textScale, textScale);
		float textWidth = mc.font.width(text);
		drawText(poseStack, text, -textWidth / 2.0f, -8.0f, HudStatNumberAnimator.WHITE_RGB, 1.0f, collector);
				poseStack.popPose();
	}

	private static float accumAlpha(float ticksSinceChange) {
		float fadeStart = 25f;
		float fadeEnd = 40f;
		if (ticksSinceChange <= fadeStart) return 1.0f;
		if (ticksSinceChange >= fadeEnd) return 0.0f;
		return 1.0f - (ticksSinceChange - fadeStart) / (fadeEnd - fadeStart);
	}

	private static void drawIndicator(PoseStack poseStack, String str, float centerX, float y, int rgb, float alpha, SubmitNodeCollector collector) {
		if (alpha <= 0.01f) return;
		Minecraft mc = Minecraft.getInstance();
		MutableComponent text = txt(str);
		float textWidth = mc.font.width(text);
		drawText(poseStack, text, centerX - textWidth / 2.0f, y, rgb, alpha, collector);
	}

	private static void drawText(PoseStack poseStack, MutableComponent text, float textX, float textY, int rgb, float alpha, SubmitNodeCollector collector) {
		Minecraft mc = Minecraft.getInstance();
		int color = withAlpha(rgb, alpha);
		int borderColor = withAlpha(0x000000, alpha);
		int light = 15728880;

		FormattedCharSequence seq = text.getVisualOrderText();
		collector.submitText(poseStack, textX + 1, textY, seq, false, Font.DisplayMode.SEE_THROUGH, light, borderColor, 0, 0);
		collector.submitText(poseStack, textX - 1, textY, seq, false, Font.DisplayMode.SEE_THROUGH, light, borderColor, 0, 0);
		collector.submitText(poseStack, textX, textY + 1, seq, false, Font.DisplayMode.SEE_THROUGH, light, borderColor, 0, 0);
		collector.submitText(poseStack, textX, textY - 1, seq, false, Font.DisplayMode.SEE_THROUGH, light, borderColor, 0, 0);
		
		collector.submitText(poseStack, textX, textY, seq, false, Font.DisplayMode.SEE_THROUGH, light, color, 0, 0);
			}

	private static int withAlpha(int rgb, float alpha) {
		int alphaChannel = Math.round(Mth.clamp(alpha, 0.0f, 1.0f) * 255.0f);
		return (alphaChannel << 24) | (rgb & 0xFFFFFF);
	}

	private static void drawTexture(SubmitNodeCollector collector, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.rendertype.RenderType renderType, float x, float y, int width, int height, int u, int v) {
        drawTexture(collector, poseStack, renderType, x, y, width, height, u, v, 0xFFFFFFFF);
    }

    /** Tinted variant: 26.2 has no RenderSystem.setShaderColor, so the tint travels as a vertex colour. */
    private static void drawTexture(SubmitNodeCollector collector, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.rendertype.RenderType renderType, float x, float y, int width, int height, int u, int v, int tint) {
		float textureSize = 128.0f;
		float minU = (float) u / textureSize;
		float maxU = (float) (u + width) / textureSize;
		float minV = (float) v / textureSize;
		float maxV = (float) (v + height) / textureSize;
		RenderBufferUtil.drawTexturedQuad(
				collector, poseStack, renderType, x, y, x + width, y + height, 0, minU, minV, maxU, maxV);
	}

	private static MutableComponent txt(String text) {
		return Component.literal(text).withStyle(Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(DMZ_FONT)));
	}
}

