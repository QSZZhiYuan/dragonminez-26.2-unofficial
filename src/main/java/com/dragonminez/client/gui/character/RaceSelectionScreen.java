package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.events.ForgeClientEvents;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.render.shader.UtilityMenuBlur;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.hair.HairManager;
import com.dragonminez.common.network.C2S.StatsSyncC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import com.dragonminez.client.render.EntityPreviewRenderContext;
import net.minecraft.client.renderer.CubeMap;
import com.dragonminez.client.render.util.DMZPanorama;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class RaceSelectionScreen extends ScaledScreen {

	private static final Identifier BUTTONS_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/buttons/characterbuttons.png");
	private static final Identifier MENU_BIG = Identifier.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/menu/menubig.png");

	private final Map<String, DMZPanorama> panoramaCache = new HashMap<>();
	private DMZPanorama currentPanorama;
	private DMZPanorama previousPanorama;
	private float panoramaFade = 1.0f;
	private float carouselAnim = 0.0f;

	protected static boolean GLOBAL_SWITCHING = false;

	private final Character character;
	private int selectedRaceIndex = 0;
	private boolean isSwitchingMenu = false;

	private float playerRotation = 180.0f;
	private boolean isDraggingModel = false;
	private double lastMouseX = 0;

	private CustomTextureButton leftButton;
	private CustomTextureButton rightButton;
	private TexturedTextButton selectButton;

	private enum TransitionState { NONE, OPENING, CLOSING }
	private static final long OPEN_ANIMATION_DURATION = 200;
	private static final long CLOSE_ANIMATION_DURATION = 120;
	private long animationStartTime;
	private TransitionState transitionState = TransitionState.NONE;
	private Screen pendingScreen;
	private boolean closeCommitted;

	public RaceSelectionScreen(Character character) {
		super(Component.translatable("gui.dragonminez.character_creation.title"));
		this.character = character;
		List<String> races = getAvailableRaces();
		for (int i = 0; i < races.size(); i++) {
			if (races.get(i).equals(character.getRace())) {
				selectedRaceIndex = i;
				break;
			}
		}
	}

	private List<String> getAvailableRaces() {
		return ConfigManager.getLoadedRaces();
	}

	private DMZPanorama getPanorama(String raceName) {
		return panoramaCache.computeIfAbsent(raceName, k -> {
			Identifier testLoc = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/background/" + k + "_panorama_0.png");
			boolean exists = false;
			if (Minecraft.getInstance().getResourceManager() != null) exists = Minecraft.getInstance().getResourceManager().getResource(testLoc).isPresent();

			Identifier baseLoc = exists
					? Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/background/" + k + "_panorama")
					: Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/background/human_panorama");

			return new DMZPanorama(new CubeMap(baseLoc), baseLoc);
		});
	}

	@Override
	protected void init() {
		UtilityMenuBlur.stop();
		super.init();
		startOpenTransition();

		List<String> races = getAvailableRaces();
		if (!races.isEmpty()) {
			selectedRaceIndex = Math.min(selectedRaceIndex, races.size() - 1);
			currentPanorama = getPanorama(races.get(selectedRaceIndex));
			previousPanorama = currentPanorama;
		}

		int centerX = getUiWidth() / 2;
		int centerY = getUiHeight() / 2;

		leftButton = new CustomTextureButton.Builder()
				.position(centerX - 60 - 25, centerY + 88)
				.size(20, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(32, 0, 32, 14)
				.textureSize(8, 14)
				.onPress(btn -> previousRace())
				.build();

		rightButton = new CustomTextureButton.Builder()
				.position(centerX - 60 + 138, centerY + 88)
				.size(20, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(20, 0, 20, 14)
				.textureSize(8, 14)
				.onPress(btn -> nextRace())
				.build();

		selectButton = new TexturedTextButton.Builder()
				.position(getUiWidth() - 85, getUiHeight() - 25)
				.size(74, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(tr("gui.dragonminez.customization.select"))
				.onPress(btn -> selectRace())
				.build();

		addRenderableWidget(leftButton);
		addRenderableWidget(rightButton);
		addRenderableWidget(selectButton);
	}

	@Override
	public void tick() {
		super.tick();
		List<String> races = getAvailableRaces();
		boolean ready = !races.isEmpty();
		leftButton.active = ready;
		rightButton.active = ready;
		selectButton.active = ready;
		if (ready && currentPanorama == null) {
			selectedRaceIndex = Math.min(selectedRaceIndex, races.size() - 1);
			String race = races.get(selectedRaceIndex);
			applyRaceDefaults(race);
			currentPanorama = getPanorama(race);
		}

		if (transitionState == TransitionState.OPENING && getTransitionProgress() >= 1.0f) transitionState = TransitionState.NONE;
		if (transitionState != TransitionState.CLOSING || closeCommitted) return;
		if (getTransitionProgress() >= 1.0f) {
			closeCommitted = true;
			if (this.minecraft != null) this.minecraft.gui.setScreen(pendingScreen);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		float tickDelta = Minecraft.getInstance().getDeltaTracker().getRealtimeDeltaTicks();

		if (panoramaFade < 1.0f) panoramaFade = Math.min(1.0f, panoramaFade + (tickDelta * 0.05f));

		if (Math.abs(carouselAnim) > 0.001f) carouselAnim = Mth.lerp(tickDelta * 0.35f, carouselAnim, 0.0f);
		else carouselAnim = 0.0f;

		// 26.2: Screen#extractRenderStateWithTooltipAndSubtitles (final) already routes
		// through extractBackground -> extractPanorama once per frame. Calling the
		// panorama here as well drew it twice and, when blur was enabled, tripped
		//   IllegalStateException: Can only blur once per frame
		this.renderCinematicBars(graphics);

		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));

		beginUiScale(graphics);

		renderCarouselElements(graphics, uiMouseX, uiMouseY);

		super.extractRenderState(graphics, uiMouseX, uiMouseY, partialTick);

		renderRaceInfo(graphics);
		renderRacialInfo(graphics);

		endUiScale(graphics);
	}

	private void renderCarouselElements(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		List<String> races = getAvailableRaces();
		if (races.isEmpty()) return;

		int centerX = getUiWidth() / 2;
		int centerY = getUiHeight() / 2 + 92;
		int modelBaseY = getUiHeight() / 2 + 70;

		String originalRace = races.get(selectedRaceIndex);

		List<Integer> carouselIndices = new ArrayList<>(List.of(-2, -1, 0, 1, 2));
		carouselIndices.sort((a, b) -> {
			float absA = Math.abs(a + carouselAnim);
			float absB = Math.abs(b + carouselAnim);
			return Float.compare(absB, absA);
		});

		for (int i : carouselIndices) {
			float visualPos = i + carouselAnim;
			float absPos = Math.abs(visualPos);

			if (absPos > 1.5f) continue;

			float scale = Math.max(0.5f, 1.0f - (absPos * 0.35f));
			float alpha = Math.max(0.0f, 1.0f - (absPos * 0.6f));

			if (alpha <= 0.05f) continue;

			int raceIndex = (selectedRaceIndex + i) % races.size();
			if (raceIndex < 0) raceIndex += races.size();
			String raceName = races.get(raceIndex);

			float xOffset = visualPos * 130f;
			applyRaceDefaults(raceName);
			int modelX = (int) (centerX + 5 + xOffset);

			renderPlayerModel(graphics, modelX, modelBaseY, (int)(75 * scale), mouseX, mouseY);

			graphics.pose().pushMatrix();
			graphics.pose().translate(centerX + xOffset, centerY);
			graphics.pose().scale(scale, scale);

			graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, MENU_BIG, -74, -7, 0f, 215f, 149, 21, 256, 256, ((int)(alpha * 255) << 24) | 0xFFFFFF);

			int alphaHex = (int)(alpha * 255) << 24;
			int color = (0x00FFFFFF & 0xFF7CFDD6) | alphaHex;
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("race." + Reference.MOD_ID + "." + raceName), 0, 0, color);

			graphics.pose().popMatrix();
		}

		applyRaceDefaults(originalRace);
	}

	private void renderCinematicBars(GuiGraphicsExtractor guiGraphics) {
		int totalBarHeight = (int) (this.height * 0.12);
		int fadeSize = 60;
		if (totalBarHeight <= fadeSize) totalBarHeight = fadeSize + 1;

		int solidHeight = totalBarHeight - fadeSize;
		int colorSolid = 0xFF000000;
		int colorTransparent = 0x00000000;

		guiGraphics.fill(0, 0, this.width, solidHeight, colorSolid);
		guiGraphics.fillGradient(0, solidHeight, this.width, solidHeight + fadeSize, colorSolid, colorTransparent);
		int bottomBarStartY = this.height - totalBarHeight;
		guiGraphics.fillGradient(0, bottomBarStartY, this.width, bottomBarStartY + fadeSize, colorTransparent, colorSolid);
		guiGraphics.fill(0, bottomBarStartY + fadeSize, this.width, this.height, colorSolid);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		extractPanorama(graphics, partialTick);
	}

	@Override
	protected void extractPanorama(GuiGraphicsExtractor graphics, float partialTick) {
		if (currentPanorama != null) currentPanorama.render(graphics, this.width, this.height, 1.0f, partialTick);
	}

	private void renderRaceInfo(GuiGraphicsExtractor graphics) {
		List<String> races = getAvailableRaces();
		if (races.isEmpty()) return;
		if (selectedRaceIndex >= races.size()) selectedRaceIndex = 0;
		String currentRace = races.get(selectedRaceIndex);

		int uiHeight = getUiHeight();

		int panelWidth = 130;
		int marginFromEdge = 10;
		int boxStartX = marginFromEdge;
		int centerX = boxStartX + (panelWidth / 2);
		int startY = (uiHeight / 2) - 50;

		String rawName = Component.translatable("race." + Reference.MOD_ID + "." + currentRace).getString();
		String boldAndColoredName = "\u00A7l" + rawName.replaceAll("(?i)(\u00A7[0-9a-fr])", "$1\u00A7l");
		Component raceName = txt(boldAndColoredName);

		graphics.pose().pushMatrix();

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, raceName, centerX, startY - 12, 0xFF7CFDD6);

		Component description = tr("race." + Reference.MOD_ID + "." + currentRace + ".desc");
		List<String> wrappedDesc = wrapText(description.getString(), panelWidth);

		int textY = startY;
		for (String line : wrappedDesc) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(line), centerX, textY, 0xFFCCCCCC);
			textY += 12;
		}

		graphics.pose().popMatrix();
	}

	private void renderRacialInfo(GuiGraphicsExtractor graphics) {
		List<String> races = getAvailableRaces();
		if (races.isEmpty()) return;
		if (selectedRaceIndex >= races.size()) selectedRaceIndex = 0;
		String currentRace = races.get(selectedRaceIndex);

		if (ConfigManager.getRaceCharacter(currentRace) == null) return;
		GeneralServerConfig.RacialSkillsConfig config = ConfigManager.getServerConfig().getRacialSkills();
		String racialSkill = ConfigManager.getRaceCharacter(currentRace).getRacialSkill();
		if (racialSkill == null || racialSkill.isEmpty()) return;

		String titleKey = "skill.dragonminez.racial_" + racialSkill;
		String descKey = "skill.dragonminez.racial_" + racialSkill + ".desc";

		Component titleComp = tr(titleKey);
		String description = "";

		switch (racialSkill) {
			case "human" -> {
				int regen = (int) Math.round((config.getHumanKiRegenBoost() - 1.0) * 100);
				description = tr(descKey, regen).getString();
			}
			case "saiyan" -> {
				int zenkaiHealth = (int) Math.round(config.getSaiyanZenkaiHealthRegen() * 100);
				int zenkaiStat = (int) Math.round(config.getSaiyanZenkaiStatBoost() * 100);
				int cooldown = config.getSaiyanZenkaiCooldownSeconds();
				int maxUses = config.getSaiyanZenkaiAmount();
				description = tr(descKey, zenkaiHealth, zenkaiStat, cooldown, maxUses).getString();
			}
			case "namekian" -> {
				int assimHealth = (int) Math.round(config.getNamekianAssimilationHealthRegen() * 100);
				int assimStat = (int) Math.round(config.getNamekianAssimilationStatBoost() * 100);
				int maxUses = config.getNamekianAssimilationAmount();
				description = tr(descKey, assimHealth, assimStat, maxUses).getString();
			}
			case "frostdemon" -> {
				int tpBoost = (int) Math.round((config.getFrostDemonTPBoost() - 1.0) * 100);
				description = tr(descKey, tpBoost).getString();
			}
			case "bioandroid" -> {
				int drainRatio = (int) Math.round(config.getBioAndroidDrainRatio() * 100);
				int cooldown = config.getBioAndroidCooldownSeconds();
				description = tr(descKey, drainRatio, cooldown).getString();
			}
			case "majin" -> {
				int absHealth = (int) Math.round(config.getMajinAbsorptionHealthRegen() * 100);
				int absStat = (int) Math.round(config.getMajinAbsorptionStatCopy() * 100);
				int maxUses = config.getMajinAbsorptionAmount();
				description = tr(descKey, absHealth, absStat, maxUses).getString();
			}
			default -> description = tr(descKey).getString();
		}

		int uiWidth = getUiWidth();
		int uiHeight = getUiHeight();

		int panelWidth = 130;
		int marginFromEdge = 68;
		int boxStartX = uiWidth - marginFromEdge - panelWidth;
		int centerX = boxStartX + (panelWidth / 2);
		int startY = (uiHeight / 2) - 50;

		graphics.pose().pushMatrix();

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, titleComp.copy().withStyle(ChatFormatting.BOLD), centerX + 60, startY - 12, 0xFF55FF55);
		List<String> wrappedDesc = wrapText(description, panelWidth);
		int textY = startY;

		for (String line : wrappedDesc) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(line), centerX + 60, textY, 0xFFCCCCCC);
			textY += 12;
		}

		graphics.pose().popMatrix();
	}

	private void renderPlayerModel(GuiGraphicsExtractor graphics, int x, int y, int scale, float mouseX, float mouseY) {
		LivingEntity player = Minecraft.getInstance().player;
		if (player == null) return;
		int adjustedScale = getAdjustedModelScale(scale);
		Quaternionf pose = (new Quaternionf()).rotateZ((float)Math.PI);
		Quaternionf cameraOrientation = (new Quaternionf()).rotateX(0);
		pose.mul(cameraOrientation);

		float yBodyRotO = player.yBodyRot;
		float yRotO = player.getYRot();
		float xRotO = player.getXRot();
		float yHeadRotO = player.yHeadRotO;
		float yHeadRot = player.yHeadRot;

		player.yBodyRot = playerRotation;
		player.setYRot(playerRotation);
		player.setXRot(0);
		player.yHeadRot = playerRotation;
		player.yHeadRotO = playerRotation;

		graphics.pose().pushMatrix();
		EntityPreviewRenderContext.renderEntityInInventory(graphics, x, y, adjustedScale, new org.joml.Vector3f(0.0F, 0.0F, 0.0F), pose, cameraOrientation, player);
		graphics.pose().popMatrix();

		player.yBodyRot = yBodyRotO;
		player.setYRot(yRotO);
		player.setXRot(xRotO);
		player.yHeadRotO = yHeadRotO;
		player.yHeadRot = yHeadRot;
	}

	protected int getAdjustedModelScale(int baseScale) {
		var player = Minecraft.getInstance().player;
		if (player == null) return baseScale;

		final float[] inverseScale = {1.0f};
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			var character = stats.getCharacter();

			Float[] resolved = character.getResolvedModelScaling();
			float currentScale = (resolved[0] + resolved[1]) / 2.0f;

			if (currentScale > 1.0f) inverseScale[0] = 0.9375f / currentScale;
		});

		return (int)(baseScale * inverseScale[0]);
	}

	private List<String> wrapText(String text, int maxWidth) {
		return TextUtil.wrap(this.font, text, maxWidth, DMZ_FONT);
	}

	private void applyRaceDefaults(String race) {
		character.setRace(race);
		RaceCharacterConfig config = ConfigManager.getRaceCharacter(race);
		if (config != null) {
			character.setBodyColor(config.getDefaultBodyColor());
			character.setBodyColor2(config.getDefaultBodyColor2());
			character.setBodyColor3(config.getDefaultBodyColor3());
			character.setHairColor(config.getDefaultHairColor());
			character.setEye1Color(config.getDefaultEye1Color());
			character.setEye2Color(config.getDefaultEye2Color());
			character.setAuraColor(config.getDefaultAuraColor());
			character.setBodyType(config.getDefaultBodyType());
			character.setHairId(config.getDefaultHairType());

			if (HairManager.canUseHair(character)) {
				character.setActiveHeadBone("hair");
				character.setRenderHairBase(true);
			} else if (config.getHeadBones() != null && config.getHeadBones().length > 0) {
				String firstExtraBone = "";
				if (character.areExtraHeadBonesEnabled()) {
					for (String bone : config.getHeadBones()) {
						if (bone != null && !bone.isEmpty() && !bone.equals("hair")) {
							firstExtraBone = bone;
							break;
						}
					}
				}
				character.setActiveHeadBone(firstExtraBone);
			} else {
				character.setActiveHeadBone("");
			}

			character.setEyesType(config.getDefaultEyesType());
			character.setNoseType(config.getDefaultNoseType());
			character.setMouthType(config.getDefaultMouthType());
			character.setTattooType(config.getDefaultTattooType());
		}
	}

	private void updateCharacterRace() {
		List<String> races = getAvailableRaces();
		if (races.isEmpty()) return;
		applyRaceDefaults(races.get(selectedRaceIndex));
		NetworkHandler.sendToServer(new StatsSyncC2S(character));
	}

	private void previousRace() {
		List<String> races = getAvailableRaces();
		if (races.isEmpty()) return;

		previousPanorama = currentPanorama;
		panoramaFade = 0.0f;
		carouselAnim = -1.0f;

		selectedRaceIndex = (selectedRaceIndex - 1 + races.size()) % races.size();
		updateCharacterRace();

		currentPanorama = getPanorama(races.get(selectedRaceIndex));
	}

	private void nextRace() {
		List<String> races = getAvailableRaces();
		if (races.isEmpty()) return;

		previousPanorama = currentPanorama;
		panoramaFade = 0.0f;
		carouselAnim = 1.0f;

		selectedRaceIndex = (selectedRaceIndex + 1) % races.size();
		updateCharacterRace();

		currentPanorama = getPanorama(races.get(selectedRaceIndex));
	}

	private void selectRace() {
		List<String> races = getAvailableRaces();
		if (races.isEmpty()) return;
		String selectedRace = races.get(selectedRaceIndex);
		character.setRace(selectedRace);

		if (this.minecraft != null) {
			isSwitchingMenu = true;
			GLOBAL_SWITCHING = true;
			startCloseTransition(new CharacterCustomizationScreen(this, character));
		}

		NetworkHandler.sendToServer(new StatsSyncC2S(character));
	}

	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
		if (transitionState == TransitionState.CLOSING) return true;
		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);
		int centerX = getUiWidth() / 2 + 5;
		int centerY = getUiHeight() / 2 + 70;
		int modelRadius = 60;

		if (uiMouseX >= centerX - modelRadius && uiMouseX <= centerX + modelRadius &&
				uiMouseY >= centerY - 100 && uiMouseY <= centerY + 20) {
			isDraggingModel = true;
			lastMouseX = uiMouseX;
			return true;
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
		if (transitionState == TransitionState.CLOSING) return true;
		isDraggingModel = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
		if (transitionState == TransitionState.CLOSING) return true;
		if (isDraggingModel) {
			double uiMouseX = toUiX(mouseX);
			double deltaX = uiMouseX - lastMouseX;
			playerRotation -= (float)(deltaX * 0.8);
			lastMouseX = uiMouseX;
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		if (transitionState == TransitionState.CLOSING) return true;
		if (event.key() == 256 && this.minecraft != null) {
			if (ConfigManager.getServerConfig().getGameplay().getForceCharacterCreation()) {
				ForgeClientEvents.requestCharacterCreationReopen();
				this.minecraft.gui.setScreen(new PauseScreen(true));
			} else {
				startCloseTransition(null);
			}
			return true;
		}

		if (event.key() == 263) {
			previousRace();
			return true;
		}
		if (event.key() == 262) {
			nextRace();
			return true;
		}

		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/**
	 * Releases every cached panorama.
	 *
	 * <p>Each {@link DMZPanorama} owns a {@code CubeMap}, and a {@code CubeMap} owns a {@code GpuBuffer}
	 * plus a {@code ProjectionMatrixBuffer}. The cache holds one entry per race, so without this the
	 * GPU resources of every race except the active one were never freed — switching races repeatedly
	 * leaked steadily.
	 */
	@Override
	public void removed() {
		super.removed();
		for (DMZPanorama panorama : this.panoramaCache.values()) {
			panorama.close();
		}
		this.panoramaCache.clear();
		this.currentPanorama = null;
		this.previousPanorama = null;
	}

	@Override
	public void onClose() {
		startCloseTransition(null);
	}

	private void startOpenTransition() {
		transitionState = TransitionState.OPENING;
		animationStartTime = System.currentTimeMillis();
		pendingScreen = null;
		closeCommitted = false;
	}

	private void startCloseTransition(Screen nextScreen) {
		if (transitionState == TransitionState.CLOSING) return;
		pendingScreen = nextScreen;
		transitionState = TransitionState.CLOSING;
		animationStartTime = System.currentTimeMillis();
		closeCommitted = false;
	}

	private float getTransitionProgress() {
		long duration = transitionState == TransitionState.CLOSING ? CLOSE_ANIMATION_DURATION : OPEN_ANIMATION_DURATION;
		if (duration <= 0L) return 1.0f;
		long elapsed = System.currentTimeMillis() - animationStartTime;
		return net.minecraft.util.Mth.clamp(elapsed / (float) duration, 0.0f, 1.0f);
	}
}

