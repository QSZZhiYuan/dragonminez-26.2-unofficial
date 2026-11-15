package com.dragonminez.client.gui.character.minigames;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.UnblurredScreen;
import com.dragonminez.client.gui.character.MinigamesScreen;
import com.dragonminez.client.util.TextUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.TrainingConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.C2S.TrainingRewardC2S;
import com.dragonminez.common.network.C2S.TrainingAnimationC2S;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;
import java.util.List;

public abstract class BaseMinigameScreen extends UnblurredScreen {
	protected static final Identifier DMZ_FONT = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "smooth");
	private static final Identifier MENU_NPC_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menunpc.png");
	private static final int PANEL_TEX_W = 346;
	private static final int PANEL_TEX_H = 94;


	protected enum State {
		READY, PLAYING, FINISHED;
	}

	protected final String minigameId;
	private final String howToKey;
	protected int levelsCleared = 0;
	protected State stage = State.READY;
	protected UltimateChallenge challenge = null;
	private int finalRewardDisplay = 0;
	private boolean trainingAnimActive = false;

	protected BaseMinigameScreen(String minigameId, String titleKey) {
		super(Component.translatable(titleKey).withStyle(Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(DMZ_FONT))));
		this.minigameId = minigameId;
		this.howToKey = "gui.dragonminez.minigame." + minigameId + ".howto";
	}

	protected boolean isPlaying() {
		return stage == State.PLAYING;
	}

	protected int level() {
		return levelsCleared + 1;
	}

	protected void levelCleared() {
		levelsCleared++;
		playUi(SoundEvents.PLAYER_LEVELUP, 1.2F, 0.5F);
		if (challenge != null && levelsCleared >= challenge.targetLevel()) {
			this.stage = State.FINISHED;
			challenge.onStageComplete();
			return;
		}
		onLevelCleared();
	}

	protected void endGame() {
		if (stage != State.PLAYING) return;
		this.stage = State.FINISHED;
		if (challenge != null) {
			stopTrainingAnimation();
			challenge.onFail();
			return;
		}
		if (levelsCleared > 0) {
			NetworkHandler.sendToServer(new TrainingRewardC2S(minigameId, levelsCleared));
		}
		this.finalRewardDisplay = computeRewardDisplay();
		stopTrainingAnimation();
	}

	private int computeRewardDisplay() {
		if (levelsCleared <= 0) return 0;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return 0;
		TrainingConfig cfg = ConfigManager.getTrainingConfig();
		TrainingConfig.MinigameSettings settings = cfg.getSettings(minigameId);
		return StatsProvider.get(StatsCapability.INSTANCE, mc.player).map(d -> {
			int tpc = d.getSingleStatCost(d.getStats().getTotalStats());
			float total = cfg.computeTpsPerLevel(tpc, settings) * levelsCleared;
			float limit = settings.getTpsLimitPerGame();
			if (limit > 0 && total > limit) total = limit;
			return (int) total;
		}).orElse(0);
	}

	private void quitToHub() {
		if (challenge != null) {
			Minecraft.getInstance().gui.setScreen(null);
			return;
		}
		Minecraft.getInstance().gui.setScreen(new MinigamesScreen());
	}

	protected void startGame() {
		this.stage = State.PLAYING;
		startTrainingAnimation();
		onStart();
	}

	private void startTrainingAnimation() {
		if (trainingAnimActive) return;
		trainingAnimActive = true;
		NetworkHandler.sendToServer(new TrainingAnimationC2S(true));
	}

	private void stopTrainingAnimation() {
		if (!trainingAnimActive) return;
		trainingAnimActive = false;
		NetworkHandler.sendToServer(new TrainingAnimationC2S(false));
	}

	@Override
	public void removed() {
		stopTrainingAnimation();
		super.removed();
	}

	protected void playUi(SoundEvent sound, float pitch, float volume) {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
	}

	protected void playHit(boolean perfect) {
		playUi(SoundEvents.EXPERIENCE_ORB_PICKUP, perfect ? 1.5F : 1.0F, 0.3F);
	}

	protected void playMiss() {
		playUi(SoundEvents.NOTE_BLOCK_BASS.value(), 0.5F, 0.4F);
	}

	@Override
	public void tick() {
		super.tick();
		if (stage == State.PLAYING) tickGame();
	}


	// 26.2: vanilla already calls extractBackground once per frame from the final
	// extractRenderStateWithTooltipAndSubtitles; calling it again here caused
	//   IllegalStateException: Can only blur once per frame
	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {

		renderGame(graphics);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr(howToKey), this.width / 2, this.height - 16, -5197648);
		drawBigTitle(graphics);
		if (stage == State.PLAYING) drawRunningHud(graphics);
		if (stage == State.READY) renderReadyOverlay(graphics);
		 else if (stage == State.FINISHED) renderFinishedOverlay(graphics);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	private void drawBigTitle(GuiGraphicsExtractor graphics) {
		MutableComponent title = this.getTitle().copy().withStyle(net.minecraft.ChatFormatting.BOLD);
		int cx = this.width / 2;
		graphics.pose().pushMatrix();
		graphics.pose().translate(cx, 14);
		graphics.pose().scale(3.0F, 3.0F);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, title, 0, 0, -10496);
		graphics.pose().popMatrix();
	}

	private void drawRunningHud(GuiGraphicsExtractor graphics) {
		if (finalRewardDisplay == 0 && levelsCleared == 0) return;
		int tpsNow = computeRewardDisplay();
		if (tpsNow <= 0) return;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.tps_so_far", tpsNow), this.width / 2, 48, -11141291);
	}

	private void drawNpcPanel(GuiGraphicsExtractor graphics, int cx, int cy, int contentW, int contentH) {
		int padding = 16;
		int panelW = contentW + padding * 2;
		int panelH = contentH + padding * 2;
		float scaleX = (float) panelW / PANEL_TEX_W;
		float scaleY = (float) panelH / PANEL_TEX_H;
		int drawX = cx - panelW / 2;
		int drawY = cy - panelH / 2;
		graphics.pose().pushMatrix();
		graphics.pose().translate(drawX, drawY);
		graphics.pose().scale(scaleX, scaleY);
		graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, MENU_NPC_TEXTURE, 0, 0, 0, 0, PANEL_TEX_W, PANEL_TEX_H, 512, 512);
		graphics.pose().popMatrix();
	}

	private void renderReadyOverlay(GuiGraphicsExtractor graphics) {
		graphics.fill(0, 0, this.width, this.height, -1157627904);
		int cx = this.width / 2;
		int cy = this.height / 2;
		int wrapWidth = 260;
		List<FormattedCharSequence> lines = this.font.split(tr(howToKey), wrapWidth);
		int textHeight = lines.size() * 10;
		drawNpcPanel(graphics, cx, cy, 280, 20 + textHeight);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.start"), cx, cy - (textHeight / 2) - 5, -1);
		int y = cy - (textHeight / 2) + 10;
		for (FormattedCharSequence line : lines) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, line, cx, y, -5197648);
			y += 10;
		}
	}

	private void renderFinishedOverlay(GuiGraphicsExtractor graphics) {
		graphics.fill(0, 0, this.width, this.height, -1157627904);
		int cx = this.width / 2;
		int cy = this.height / 2;
		drawNpcPanel(graphics, cx, cy, 280, 90);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.finished"), cx, cy - 38, -10496);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.rounds_won", levelsCleared), cx, cy - 20, -1);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.tps_won", finalRewardDisplay), cx, cy - 4, -11141291);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.good_luck"), cx, cy + 14, -5197648);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.continue"), cx, cy + 30, -8947849);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		switch (stage) {
			case READY -> {
				if (event.key() == GLFW.GLFW_KEY_ESCAPE) quitToHub();
				 else startGame();
				return true;
			}
			case FINISHED -> {
				quitToHub();
				return true;
			}
			case PLAYING -> {
				if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
					endGame();
					return true;
				}
				if (onKey(event.key())) return true;
			}
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
		switch (stage) {
			case READY -> {
				startGame();
				return true;
			}
			case FINISHED -> {
				quitToHub();
				return true;
			}
			case PLAYING -> {
				if (onMouseClick(mouseX, mouseY, button)) return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	protected MutableComponent tr(String key, Object... args) {
		return Component.translatable(key, args).withStyle(Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(DMZ_FONT)));
	}

	protected abstract void tickGame();

	protected abstract void renderGame(GuiGraphicsExtractor graphics);

	protected boolean onKey(int keyCode) {
		return false;
	}

	protected boolean onMouseClick(double mouseX, double mouseY, int button) {
		return false;
	}

	protected void onStart() {
	}

	protected void onLevelCleared() {
	}

	@java.lang.SuppressWarnings("all")
	public void setChallenge(final UltimateChallenge challenge) {
		this.challenge = challenge;
	}
}

