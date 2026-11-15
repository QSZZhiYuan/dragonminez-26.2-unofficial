package com.dragonminez.client.render.effects;

import com.dragonminez.client.gui.render.DmzGuiTexturedQuad;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.AuraMeshFactory;
import com.dragonminez.client.render.util.AuraPose;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.compat.SableCompat;
import com.dragonminez.common.util.TransformationsHelper;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import com.geckolib.cache.model.BakedGeoModel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuraRenderer {
	private static final float HALF_SQRT_3 = (float) (Math.sqrt(3.0D) / 2.0D);
	private static final float FADE_SPEED = 0.005f;
	private static final float PULSE_SPEED = 0.01f;

	private static final Map<Integer, Long> FUSION_START_TIME = new ConcurrentHashMap<>();
	private static final Map<Integer, Boolean> WAS_FUSED_CACHE = new ConcurrentHashMap<>();
	private static final Map<Integer, Float> COLOR_PROGRESS_MAP = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> COLOR_TICK_MAP = new ConcurrentHashMap<>();
	private static final Map<Integer, Float> PULSE_PROGRESS = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> PULSE_LAST_RENDER_TIME = new ConcurrentHashMap<>();
	private static final Map<Integer, CachedAuraData> AURA_CACHE = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> LAST_RENDER_TIME = new ConcurrentHashMap<>();
	private static java.util.List<com.dragonminez.client.render.util.KiMeshFactory.Vertex> cachedLightningMesh;

	public static RenderType auraType(Identifier texture) {
		return IrisCompat.isShaderPackInUse() ? ModRenderTypes.getCustomAuraCompat(texture) : ModRenderTypes.getCustomAura(texture);
	}

	private static RenderType auraType(Identifier texture, boolean localThirdPerson) {
		return localThirdPerson ? ModRenderTypes.getCustomAuraCompat(texture) : auraType(texture);
	}

	public static RenderType lightningType(Identifier texture) {
		return IrisCompat.isShaderPackInUse() ? ModRenderTypes.getCustomLightningCompat(texture) : ModRenderTypes.getCustomLightning(texture);
	}

	/**
	 * 26.2：混合/深度/剔除状态由 RenderPipeline 承担（见 ModRenderTypes.getCustomAura），
	 * 且 RenderPass 在 submitCustomGeometry 回调里拿不到，因此手动 setup/clear 不再有意义。
	 * 保留方法签名是为了让 DBSagasRenderer 等调用点不用跟着改。
	 */
	public static void customSetup(RenderType type, Identifier texture, Object shader) {
		// no-op：状态在 pipeline 上
	}

	public static void customClear(RenderType type) {
		// no-op：状态在 pipeline 上
	}

	/**
	 * Aura 的最终绘制点。
	 *
	 * <h2>26.2 迁移</h2>
	 * 1.21 这里做三件事：{@code VertexBuffer#bind()}、手写 GL 开关跳过深度/模板测试、
	 * {@code mesh.drawWithShader(pose, proj, shader)}。三者现在都不成立：
	 * {@code VertexBuffer} 与 {@code ShaderInstance} 已删除，{@code RenderPass} 在
	 * {@code submitCustomGeometry} 回调里也拿不到，所以没法在绘制时开关 GL 状态。
	 * 混合/深度/剔除状态改为由 RenderPipeline 承担（{@code ModRenderTypes.getCustomAura} 已带上）；
	 * 着色参数（颜色/alpha/滚动速度）改为压进顶点色。
	 *
	 * @param mesh      预烘焙的公告板/地面四边形顶点数据
	 * @param tint      逐顶点乘算的 ARGB
	 * @param renderType 已按 Iris 状态选好的 aura 渲染类型
	 */
	public static void applyAndDraw(java.util.List<com.dragonminez.client.render.util.KiMeshFactory.Vertex> mesh,
									 PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType,
									 int tint) {
		collector.submitCustomGeometry(poseStack, renderType,
				(pose, vc) -> com.dragonminez.client.render.util.KiMeshFactory.emit(mesh, pose, vc, tint));
	}

	/** Converts an AuraMeshFactory quad ({x,y,z,u,v}) into replayable vertices. */
	public static java.util.List<com.dragonminez.client.render.util.KiMeshFactory.Vertex> toQuadVertices(float[][] quad, int argb) {
		java.util.List<com.dragonminez.client.render.util.KiMeshFactory.Vertex> out = new java.util.ArrayList<>(quad.length);
		for (float[] v : quad) {
			out.add(new com.dragonminez.client.render.util.KiMeshFactory.Vertex(v[0], v[1], v[2], v[3], v[4], 0.0f, 0.0f, 1.0f, argb));
		}
		return out;
	}

	public static class AuraLayer {
		public String type;
		public int layerId;
		public float[] color;
		public float alpha;

		public AuraLayer(String type, int layerId, float[] color) {
			this(type, layerId, color, 1.0f);
		}

		public AuraLayer(String type, int layerId, float[] color, float alpha) {
			this.type = type;
			this.layerId = layerId;
			this.color = color;
			this.alpha = alpha;
		}
	}

	private static class CachedAuraData {
		float auraScaleX, auraScaleY, auraScaleZ;
		float bodyScaleX, bodyScaleY, bodyScaleZ;
		float modelScaleX, modelScaleY, modelScaleZ;
		float alphaProgress;
		BakedGeoModel playerModel;
		AuraPose bodyPose = AuraPose.upright();
		List<AuraLayer> lastLayers;
	}

	/** Animated preview overlay using the same four-frame intensity-mask shader as world auras. */
	public static void renderGuiAura(Player player, net.minecraft.client.gui.GuiGraphicsExtractor graphics,
			int centerX, int centerY, int scale, float partialTick) {
		StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (stats == null) return;

		List<AuraLayer> activeLayers = getAuraLayers(player, stats, partialTick);
		if (activeLayers.isEmpty()) return;

		float[] modelScale = getModelScale(stats);
		float[] auraScale = getAuraScale(stats, modelScale);

		Minecraft mc = Minecraft.getInstance();
		ScreenRectangle scissor = graphics.peekScissorStack();

		for (AuraLayer layer : activeLayers) {
			String typeStr = layer.type != null && !layer.type.isEmpty() ? layer.type.toLowerCase() : "kakarot";
			Identifier tex = Identifier.fromNamespaceAndPath(Reference.MOD_ID,
					"textures/entity/races/aura/" + typeStr + "_aura.png");

			net.minecraft.client.renderer.texture.AbstractTexture texObj =
					mc.getTextureManager().getTexture(tex);
			net.minecraft.client.gui.render.TextureSetup textureSetup =
					net.minecraft.client.gui.render.TextureSetup.singleTexture(
							texObj.getTextureView(), texObj.getSampler());

			float halfW = auraScale[0] * scale * 2.0f * (1.0f + layer.layerId * 0.15f);
			float halfH = auraScale[1] * scale * 2.0f * (1.0f + layer.layerId * 0.15f);
			float auraY = centerY - (halfH - scale * 0.45f);
			float alpha = Math.max(0.0f, Math.min(1.0f, layer.alpha));
			if (alpha <= 0.01f) continue;

			int tint = net.minecraft.util.ARGB.colorFromFloat(alpha,
					layer.color[0], layer.color[1], layer.color[2]);

			graphics.submitGuiElementRenderState(
					new DmzGuiTexturedQuad(
							ModRenderTypes.GUI_AURA, textureSetup, new org.joml.Matrix3x2f(graphics.pose()),
							centerX - halfW, auraY - halfH,
							centerX + halfW, auraY + halfH,
							0.0f, 0.0f, 1.0f, 1.0f,
							tint, scissor));
		}
	}

	public static void processFusionFlashes(Minecraft mc, long gameTime, float partialTick, PoseStack poseStack, SubmitNodeCollector buffers) {
		for (Player player : mc.level.players()) {
			int playerId = player.getId();
			var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);

			if (stats != null) {
				boolean isFused = stats.getStatus().isFused();
				boolean wasFused = WAS_FUSED_CACHE.getOrDefault(playerId, false);

				if (isFused && !wasFused) FUSION_START_TIME.put(playerId, gameTime);
				WAS_FUSED_CACHE.put(playerId, isFused);

				if (FUSION_START_TIME.containsKey(playerId)) {
					long timeSinceStart = gameTime - FUSION_START_TIME.get(playerId);
					if (timeSinceStart < 60) {
						List<AuraLayer> layers = getAuraLayers(player, stats, partialTick);
						if (!layers.isEmpty()) {
							float[] color = layers.get(layers.size() - 1).color;
							int r = (int) (color[0] * 255);
							int g = (int) (color[1] * 255);
							int b = (int) (color[2] * 255);
							renderFusionFlash(player, timeSinceStart + partialTick, poseStack, buffers, r, g, b);
						}
					} else if (timeSinceStart > 80) {
						FUSION_START_TIME.remove(playerId);
					}
				}
		}

		}
	}
	public static void processThirdPersonAuras(Minecraft mc, PoseStack poseStack, SubmitNodeCollector collector, Set<Integer> currentFramePlayers, boolean isFirstPerson, boolean isCameraColliding) {
		var auras = PlayerEffectQueue.getAndClearAuras();
		for (var entry : auras) {
			Player player = entry.player();
			boolean isLocalPlayer = player == mc.player;

			if (!isFirstPerson || isCameraColliding || !isLocalPlayer) {
				currentFramePlayers.add(player.getId());
				renderShaderAura(entry, poseStack, mc, collector);
			}
		}
	}

	public static void processFirstPersonAuras(Minecraft mc, PoseStack poseStack, SubmitNodeCollector collector, float partialTick, Set<Integer> currentFramePlayers, boolean isFirstPerson, net.minecraft.client.renderer.state.level.CameraRenderState camera) {
		var firstPersonAuras = PlayerEffectQueue.getAndClearFirstPersonAuras();
		for (var entry : firstPersonAuras) {
			Player player = entry.player();
			if (!currentFramePlayers.contains(player.getId())) {
				currentFramePlayers.add(player.getId());
				renderShaderFirstPersonAura(player, entry.partialTick(), poseStack, mc, collector);
			}
		}

		Player localPlayer = mc.player;
		if (isFirstPerson && localPlayer != null && !currentFramePlayers.contains(localPlayer.getId())) {
			var stats = StatsProvider.get(StatsCapability.INSTANCE, localPlayer).orElse(null);
			if (stats != null) {
				boolean isAuraActive = stats.getStatus().isAuraActive() || stats.getStatus().isPermanentAura()
						|| KiChargeEffects.hasChargeVisual(localPlayer, partialTick);
				var character = stats.getCharacter();
				boolean hasLightning = false;

				if (character.hasActiveStackForm() && character.getActiveStackFormData() != null) {
					hasLightning = character.getActiveStackFormData().getHasLightnings();
				} else if (character.hasActiveForm() && character.getActiveFormData() != null) {
					hasLightning = character.getActiveFormData().getHasLightnings();
				}
				hasLightning |= AuraFxState.hasLightning(stats) || KiChargeEffects.burst(localPlayer, partialTick) > 0.0f;

				if (isAuraActive || hasLightning) {
					currentFramePlayers.add(localPlayer.getId());

					if (isAuraActive && !AuraFxState.isBurstAuraSuppressed(localPlayer, stats, partialTick)) {
						renderShaderFirstPersonAura(localPlayer, partialTick, poseStack, mc, collector);
					}

					if (hasLightning) {
						renderSparksImpl(localPlayer, poseStack, collector, partialTick, true, AuraPose.upright(), camera);
					}
				}
			}
		}
	}

	public static void processGhostAuras(Minecraft mc, PoseStack poseStack, SubmitNodeCollector collector, float partialTick, Set<Integer> currentFramePlayers) {
		Iterator<Map.Entry<Integer, CachedAuraData>> it = AURA_CACHE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, CachedAuraData> entry = it.next();
			int playerId = entry.getKey();
			CachedAuraData data = entry.getValue();

			if (!currentFramePlayers.contains(playerId)) {
				// entity ids get reused, so a cached player id can now point at any entity (e.g. a Bat) -> guard the cast
				if (!(mc.level.getEntity(playerId) instanceof Player player) || !player.isAlive()) {
					it.remove();
					continue;
				}

				var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
				boolean isAuraActive = stats != null && (stats.getStatus().isAuraActive() || stats.getStatus().isPermanentAura());

				if (isAuraActive) continue;

				boolean stillVisible = renderShaderGhostAura(player, data, poseStack, mc, partialTick, collector);
				if (!stillVisible) {
					it.remove();
				}
			}
		}
	}

	/** Full-charge flash uses the same deferred geometry path as ki explosions, including Iris. */
	public static void processChargeBursts(Minecraft mc, PoseStack poseStack, SubmitNodeCollector collector, float partialTick) {
		Vec3 camera = mc.gameRenderer.mainCamera().position();
		for (Player player : mc.level.players()) {
			var charge = KiChargeEffects.state(player);
			if (charge == null || charge.burst(partialTick) <= 0.0f) continue;
			var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
			if (stats == null) continue;
			var layers = getAuraLayers(player, stats, partialTick);
			if (layers.isEmpty()) continue;
			float[] color = layers.getLast().color;
			float progress = charge.burstProgress(partialTick);
			float fade = charge.burst(partialTick);
			float radius = (0.6f + 2.8f * (float) Math.sqrt(progress)) * getAuraScale(stats, getModelScale(stats))[0];
			boolean firstPerson = player == mc.player && mc.options.getCameraType().isFirstPerson();
			int tint = net.minecraft.util.ARGB.colorFromFloat(fade * fade * (firstPerson ? 0.12f : 0.4f), color[0], color[1], color[2]);
			int border = net.minecraft.util.ARGB.colorFromFloat(1, color[0] * 0.65f, color[1] * 0.65f, color[2] * 0.65f);
			Vec3 position = new Vec3(Mth.lerp(partialTick, player.xo, player.getX()),
					Mth.lerp(partialTick, player.yo, player.getY()) + player.getBbHeight() * 0.5,
					Mth.lerp(partialTick, player.zo, player.getZ()));
			position = SableCompat.projectToWorld(player.level(), position);
			poseStack.pushPose();
			poseStack.translate(position.x - camera.x, position.y - camera.y, position.z - camera.z);
			poseStack.scale(radius, radius, radius);
			collector.submitCustomGeometry(poseStack, ModRenderTypes.kiWave(), (pose, vertices) ->
					com.dragonminez.client.render.util.KiMeshFactory.emitKiWave(
							com.dragonminez.client.render.util.KiMeshFactory.sphereMesh(), pose, vertices, tint, border, 0xFFFFFFFF));
			poseStack.popPose();
		}
	}

	public static void processSparks(PoseStack poseStack, SubmitNodeCollector collector, boolean isFirstPerson, net.minecraft.client.renderer.state.level.CameraRenderState camera) {
		var sparks = PlayerEffectQueue.getAndClearSparks();
		if (sparks != null && !sparks.isEmpty()) {
			for (var entry : sparks) {
				if (entry != null) {
					boolean isFirstLocal = isFirstPerson && entry.player() == Minecraft.getInstance().player;
						renderSparksImpl(entry.player(), poseStack, collector, entry.partialTick(), isFirstLocal, entry.bodyPose(), camera);
				}
			}
		}
	}

	public static void cleanCaches(Set<Integer> currentFramePlayers) {
		RenderTarget mainTarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();

		LAST_RENDER_TIME.keySet().removeIf(id -> !currentFramePlayers.contains(id) && !AURA_CACHE.containsKey(id));
		COLOR_PROGRESS_MAP.keySet().removeIf(id -> !currentFramePlayers.contains(id) && !AURA_CACHE.containsKey(id));
		COLOR_TICK_MAP.keySet().removeIf(id -> !currentFramePlayers.contains(id) && !AURA_CACHE.containsKey(id));
		PULSE_LAST_RENDER_TIME.keySet().removeIf(id -> !currentFramePlayers.contains(id) && !AURA_CACHE.containsKey(id));
		PULSE_PROGRESS.keySet().removeIf(id -> !currentFramePlayers.contains(id) && !AURA_CACHE.containsKey(id));
	}

	private static float[] getModelScale(StatsData stats) {
		Float[] resolved = stats.getCharacter().getResolvedModelScaling();
		return new float[]{resolved[0], resolved[1], resolved[2]};
	}

	private static float[] getBodyScale(StatsData stats) {
		float[] modelScale = getModelScale(stats);
		float sX = modelScale[0], sY = modelScale[1], sZ = modelScale[2];
		var character = stats.getCharacter();

		String currentForm = character.getActiveForm() != null ? character.getActiveForm().toLowerCase() : "";
		if (currentForm.contains("ozaru")) {
			sX = Math.max(0.1f, sX - 2.8f);
			sY = Math.max(0.1f, sY - 2.8f);
			sZ = Math.max(0.1f, sZ - 2.8f);
		}

		return new float[]{sX, sY, sZ};
	}

	private static float[] getAuraScale(StatsData stats, float[] modelScale) {
		float baseScale = 1.05f;
		var character = stats.getCharacter();
		String currentForm = character.getActiveForm() != null ? character.getActiveForm().toLowerCase() : "";

		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null) baseScale += 0.1f;
		if (character.hasActiveForm() && character.getActiveFormData() != null) baseScale += 0.1f;
		if (currentForm.contains("oozaru")) baseScale = 1.2f;
		if (currentForm.contains("supersaiyan2") || currentForm.contains("supersaiyan3") || currentForm.contains("ultra") || currentForm.contains("superperfect")) {
			baseScale += 0.2f;
		}

		baseScale *= (float) AuraFxState.auraScaleMultiplier(stats);
		return new float[]{baseScale * modelScale[0], baseScale * modelScale[1], baseScale * modelScale[2]};
	}

	private static List<AuraLayer> getAuraLayers(Player player, StatsData stats, float partialTick) {
		var character = stats.getCharacter();
		int entityId = player.getId();

		FormConfig.FormData nextForm = null;
		boolean chargingNormal = false;
		boolean chargingStack = false;

		if (stats.getStatus().isActionCharging()) {
			if (stats.getStatus().getSelectedAction() == ActionMode.STACK) {
				nextForm = TransformationsHelper.getNextAvailableStackForm(stats);
				chargingStack = nextForm != null;
			} else if (stats.getStatus().getSelectedAction() == ActionMode.FORM) {
				if (!character.hasActiveStackForm()) {
					nextForm = TransformationsHelper.getNextAvailableForm(stats);
					chargingNormal = nextForm != null;
				}
			}
		}

		float chargeProgress = 0.0f;
		if (chargingNormal || chargingStack) {
			int mastery;
			if (chargingStack) {
				String mGroup = character.hasActiveStackForm() ? character.getActiveStackFormGroup() : character.getSelectedStackFormGroup();
				mastery = (int) character.getStackFormMasteries().getMastery(mGroup, nextForm.getName());
			} else {
				String mGroup = character.hasActiveForm() ? character.getActiveFormGroup() : character.getSelectedFormGroup();
				mastery = (int) character.getFormMasteries().getMastery(mGroup, nextForm.getName());
			}
			float ratePerTick = (5 + Math.min(20, (int)(mastery * 0.2))) / 2000.0f;

			float lastProgress = COLOR_PROGRESS_MAP.getOrDefault(entityId, 0.0f);
			long lastTick = COLOR_TICK_MAP.getOrDefault(entityId, 0L);
			long currentTick = player.tickCount;

			if (currentTick != lastTick) {
				long ticksElapsed = lastTick == 0L ? 1L : Math.max(1L, currentTick - lastTick);
				lastProgress = Math.min(1.0f, lastProgress + ratePerTick * ticksElapsed);
				COLOR_TICK_MAP.put(entityId, currentTick);
				COLOR_PROGRESS_MAP.put(entityId, lastProgress);
			}
			// Sub-tick smoothing so the colour ramps cleanly between ticks.
			chargeProgress = Math.max(0.0f, Math.min(1.0f, lastProgress + ratePerTick * partialTick));
		} else COLOR_PROGRESS_MAP.put(entityId, 0.0f);

		Map<Integer, AuraLayer> layerMap = new HashMap<>();

		String normalHex = character.getAuraColor();
		float[] normalColor = character.getRgbAuraColor();
		String normalType = ConfigManager.getRaceCharacter(character.getRace()) != null ?
				ConfigManager.getRaceCharacter(character.getRace()).getAuraType() : "kakarot";
		int normalLayerId = 0;

		if (character.hasActiveForm() && character.getActiveFormData() != null) {
			var fd = character.getActiveFormData();
			if (fd.getAuraColor() != null && !fd.getAuraColor().isEmpty()) {
				normalHex = fd.getAuraColor();
				normalColor = fd.getRgbAuraColor();
			}
			if (fd.getAuraType() != null && !fd.getAuraType().isEmpty()) normalType = fd.getAuraType();
			normalLayerId = fd.getAuraLayer() != null ? fd.getAuraLayer() : 0;
		}

		AuraLayer chargeLayer = null;
		if (chargingNormal && nextForm != null) {
			String targetHex = nextForm.getAuraColor() != null && !nextForm.getAuraColor().isEmpty() ? nextForm.getAuraColor() : normalHex;
			int targetLayer = nextForm.getAuraLayer() != null ? nextForm.getAuraLayer() : normalLayerId;
			if (targetLayer == normalLayerId) {
				normalColor = interpolateColor(normalHex, targetHex, chargeProgress);
			} else {
				String targetType = nextForm.getAuraType() != null && !nextForm.getAuraType().isEmpty() ? nextForm.getAuraType() : normalType;
				chargeLayer = new AuraLayer(targetType, targetLayer, ColorUtils.hexToRgb(targetHex), chargeProgress);
			}
		}

		putLayer(layerMap, normalLayerId, new AuraLayer(normalType, normalLayerId, normalColor));
		if (chargeLayer != null) putLayer(layerMap, chargeLayer.layerId, chargeLayer);

		if (character.hasActiveForm() && character.getActiveFormData() != null && character.getActiveFormData().hasExtraAura()) {
			var fd = character.getActiveFormData();
			putShifting(layerMap, fd.getExtraAuraLayer(), new AuraLayer(fd.getExtraAuraType(), fd.getExtraAuraLayer(), fd.getRgbExtraAuraColor()));
		}

		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null) {
			var fd = character.getActiveStackFormData();
			String stackHex = fd.getAuraColor() != null && !fd.getAuraColor().isEmpty() ? fd.getAuraColor() : "#FFFFFF";
			String stackType = fd.getAuraType() != null && !fd.getAuraType().isEmpty() ? fd.getAuraType() : "kakarot";
			int stackLayerId = fd.getAuraLayer() != null ? fd.getAuraLayer() : 1;

			float[] stackColor = (fd.getAuraColor() != null && !fd.getAuraColor().isEmpty()) ? fd.getRgbAuraColor() : ColorUtils.hexToRgb(stackHex);
			if (chargingStack && nextForm != null) {
				String targetHex = nextForm.getAuraColor() != null && !nextForm.getAuraColor().isEmpty() ? nextForm.getAuraColor() : stackHex;
				stackColor = interpolateColor(stackHex, targetHex, chargeProgress);
			}

			putLayer(layerMap, stackLayerId, new AuraLayer(stackType, stackLayerId, stackColor));

			if (fd.hasExtraAura()) {
				putShifting(layerMap, fd.getExtraAuraLayer(), new AuraLayer(fd.getExtraAuraType(), fd.getExtraAuraLayer(), fd.getRgbExtraAuraColor()));
			}

		} else if (chargingStack && nextForm != null) {
			String targetHex = nextForm.getAuraColor() != null && !nextForm.getAuraColor().isEmpty() ? nextForm.getAuraColor() : "#FFFFFF";
			String stackType = nextForm.getAuraType() != null && !nextForm.getAuraType().isEmpty() ? nextForm.getAuraType() : "kakarot";
			int stackLayerId = nextForm.getAuraLayer() != null ? nextForm.getAuraLayer() : 1;

			if (stackLayerId == normalLayerId) {
				AuraLayer base = layerMap.get(normalLayerId);
				if (base != null) base.color = interpolateColor(normalHex, targetHex, chargeProgress);
			} else {
				putLayer(layerMap, stackLayerId, new AuraLayer(stackType, stackLayerId, ColorUtils.hexToRgb(targetHex), chargeProgress));
			}
		}

		List<AuraLayer> activeLayers = new ArrayList<>(layerMap.values());
		activeLayers.sort(Comparator.comparingInt(l -> l.layerId));
		return activeLayers;
	}

	private static void putLayer(Map<Integer, AuraLayer> layerMap, int layerId, AuraLayer layer) {
		if (layerId < 0) return;
		layerId = Mth.clamp(layerId, 0, 6);
		layer.layerId = layerId;
		layerMap.put(layerId, layer);
	}

	private static void putShifting(Map<Integer, AuraLayer> layerMap, int layerId, AuraLayer layer) {
		if (layerId < 0) return;
		layerId = Mth.clamp(layerId, 0, 6);
		while (layerMap.containsKey(layerId) && layerId < 6) layerId++;
		layer.layerId = layerId;
		layerMap.put(layerId, layer);
	}

	private static float[] interpolateColor(String hexFrom, String hexTo, float factor) {
		float[] rgbFrom = ColorUtils.hexToRgb(hexFrom);
		float[] rgbTo = ColorUtils.hexToRgb(hexTo);

		float r = Mth.lerp(factor, rgbFrom[0], rgbTo[0]);
		float g = Mth.lerp(factor, rgbFrom[1], rgbTo[1]);
		float b = Mth.lerp(factor, rgbFrom[2], rgbTo[2]);

		return new float[]{r, g, b};
	}

	public static void renderShaderFirstPersonAura(Player player, float partialTick, PoseStack poseStack, Minecraft mc, SubmitNodeCollector collector) {
		var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (stats == null) return;
		if (AuraFxState.isBurstAuraSuppressed(player, stats, partialTick)) return;

		int playerId = player.getId();
		CachedAuraData data = AURA_CACHE.computeIfAbsent(playerId, k -> new CachedAuraData());
		long gameTime = player.level().getGameTime();

		if (gameTime - LAST_RENDER_TIME.getOrDefault(playerId, 0L) > 2) data.alphaProgress = 0.0f;
		LAST_RENDER_TIME.put(playerId, gameTime);

		if (data.alphaProgress < 1.0f) {
			data.alphaProgress += FADE_SPEED;
			if (data.alphaProgress > 1.0f) data.alphaProgress = 1.0f;
		}

		float[] modelScale = getModelScale(stats);
		float[] body = getBodyScale(stats);
		float[] auraScale = getAuraScale(stats, modelScale);

		data.modelScaleX = modelScale[0]; data.modelScaleY = modelScale[1]; data.modelScaleZ = modelScale[2];
		data.bodyScaleX = body[0]; data.bodyScaleY = body[1]; data.bodyScaleZ = body[2];
		data.auraScaleX = auraScale[0]; data.auraScaleY = auraScale[1]; data.auraScaleZ = auraScale[2];

		List<AuraLayer> activeLayers = getAuraLayers(player, stats, partialTick);
		if (activeLayers.isEmpty()) return;
		data.lastLayers = activeLayers;

		Vec3 cameraPos = mc.gameRenderer.mainCamera().position();
		double lerpX = Mth.lerp(partialTick, player.xo, player.getX());
		double lerpY = Mth.lerp(partialTick, player.yo, player.getY());
		double lerpZ = Mth.lerp(partialTick, player.zo, player.getZ());

		if (player.onGround() && KiChargeEffects.burst(player, partialTick) <= 0.001f) {
			AuraLayer topLayer = activeLayers.get(activeLayers.size() - 1);
			poseStack.pushPose();
			poseStack.translate(lerpX - cameraPos.x, lerpY - cameraPos.y + 0.05, lerpZ - cameraPos.z);
			renderShaderPulseAura(player, data, topLayer, poseStack, mc, collector, partialTick, data.alphaProgress);
			poseStack.popPose();
		}

		for (AuraLayer layer : activeLayers) {
			poseStack.pushPose();
			poseStack.translate(lerpX - cameraPos.x, lerpY - cameraPos.y, lerpZ - cameraPos.z);
			executeAuraShaderDraw(player, data, layer, poseStack, mc, collector, partialTick, data.alphaProgress, true);
			poseStack.popPose();
		}
	}

	private static void renderShaderAura(PlayerEffectQueue.AuraRenderEntry entry, PoseStack poseStack, Minecraft mc, SubmitNodeCollector collector) {
		var player = entry.player();
		var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (stats == null) return;

		int playerId = player.getId();
		CachedAuraData data = AURA_CACHE.computeIfAbsent(playerId, k -> new CachedAuraData());
		long gameTime = player.level().getGameTime();

		if (gameTime - LAST_RENDER_TIME.getOrDefault(playerId, 0L) > 2) {
			data.alphaProgress = 0.0f;
		}
		LAST_RENDER_TIME.put(playerId, gameTime);

		if (data.alphaProgress < 1.0f) {
			data.alphaProgress += FADE_SPEED;
			if (data.alphaProgress > 1.0f) data.alphaProgress = 1.0f;
		}

		float[] modelScale = getModelScale(stats);
		float[] body = getBodyScale(stats);
		float[] auraScale = getAuraScale(stats, modelScale);

		data.modelScaleX = modelScale[0]; data.modelScaleY = modelScale[1]; data.modelScaleZ = modelScale[2];
		data.bodyScaleX = body[0]; data.bodyScaleY = body[1]; data.bodyScaleZ = body[2];
		data.auraScaleX = auraScale[0]; data.auraScaleY = auraScale[1]; data.auraScaleZ = auraScale[2];
		data.playerModel = entry.playerModel();
		data.bodyPose = entry.bodyPose();

		List<AuraLayer> activeLayers = getAuraLayers(player, stats, entry.partialTick());
		if (activeLayers.isEmpty()) return;
		data.lastLayers = activeLayers;

		// Match DMZ 1.20.1: the deferred stage starts from the world-render base pose,
		// so position the aura with the interpolated camera-relative entity position.
		Vec3 cameraPos = mc.gameRenderer.mainCamera().position();
		double lerpX = Mth.lerp(entry.partialTick(), player.xo, player.getX());
		double lerpY = Mth.lerp(entry.partialTick(), player.yo, player.getY());
		double lerpZ = Mth.lerp(entry.partialTick(), player.zo, player.getZ());

		if (player.onGround() && KiChargeEffects.burst(player, entry.partialTick()) <= 0.001f) {
			AuraLayer topLayer = activeLayers.get(activeLayers.size() - 1);
			poseStack.pushPose();
			poseStack.translate(lerpX - cameraPos.x, lerpY - cameraPos.y + 0.05, lerpZ - cameraPos.z);
			renderShaderPulseAura(player, data, topLayer, poseStack, mc, collector, entry.partialTick(), data.alphaProgress);
			poseStack.popPose();
		}

		for (AuraLayer layer : activeLayers) {
			poseStack.pushPose();
			poseStack.translate(lerpX - cameraPos.x, lerpY - cameraPos.y, lerpZ - cameraPos.z);
			executeAuraShaderDraw(player, data, layer, poseStack, mc, collector, entry.partialTick(), data.alphaProgress, false);
			poseStack.popPose();
		}
	}

	private static boolean renderShaderGhostAura(Player player, CachedAuraData data, PoseStack poseStack, Minecraft mc, float partialTick, SubmitNodeCollector collector) {
		if (data.alphaProgress > 0.0f) {
			data.alphaProgress -= FADE_SPEED;
			if (data.alphaProgress < 0.0f) data.alphaProgress = 0.0f;
		}
		if (data.alphaProgress <= 0.001f) return false;

		var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (stats == null) return false;

		List<AuraLayer> activeLayers = data.lastLayers;
		if (activeLayers == null || activeLayers.isEmpty()) return false;

		Vec3 cameraPos = mc.gameRenderer.mainCamera().position();
		double lerpX = Mth.lerp(partialTick, player.xo, player.getX());
		double lerpY = Mth.lerp(partialTick, player.yo, player.getY());
		double lerpZ = Mth.lerp(partialTick, player.zo, player.getZ());

		if (player.onGround() && KiChargeEffects.burst(player, partialTick) <= 0.001f) {
			AuraLayer topLayer = activeLayers.get(activeLayers.size() - 1);
			poseStack.pushPose();
			poseStack.translate(lerpX - cameraPos.x, lerpY - cameraPos.y + 0.05, lerpZ - cameraPos.z);
			renderShaderPulseAura(player, data, topLayer, poseStack, mc, collector, partialTick, data.alphaProgress);
			poseStack.popPose();
		}

		boolean isLocalPlayer = player == mc.player;
		boolean isFirstPerson = isLocalPlayer && mc.options.getCameraType().isFirstPerson();

		for (AuraLayer layer : activeLayers) {
			poseStack.pushPose();
			poseStack.translate(lerpX - cameraPos.x, lerpY - cameraPos.y, lerpZ - cameraPos.z);
			executeAuraShaderDraw(player, data, layer, poseStack, mc, collector, partialTick, data.alphaProgress, isFirstPerson);
			poseStack.popPose();
		}
		return true;
	}


    private static boolean enqueueAura3D(Player player, CachedAuraData data, AuraLayer layer, PoseStack stack,
                                         float partialTick, float alpha, boolean firstPerson) {
        var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        if (stats == null) return false;
        var character = stats.getCharacter();
        FormConfig.FormData form = character.getActiveFormData();
        var stacked = character.getActiveStackFormData();
        if (stacked != null && stacked.getAuraLayer() != null && stacked.getAuraLayer() == layer.layerId) form = stacked;
        boolean extra = form != null && form.hasExtraAura() && form.getExtraAuraLayer() == layer.layerId;
        var config = form == null ? FormConfig.Aura3DStyle.DEFAULT : extra ? form.getExtraAura3DStyle() : form.getAura3DStyle();
        String type = FormConfig.resolveAura3DType(form == null ? null : extra ? form.getExtraAuraType3D() : form.getAuraType3D(), AuraModeState.style(player));
		float chargeEnvelope = chargeVisualEnvelope(player, stats, partialTick);
		float growth = Math.clamp(alpha * chargeEnvelope, 0, 1);
        if (growth <= 0.001f) return true;
        stack.pushPose();
        try {
            stack.translate(data.bodyPose.offset().x(), data.bodyPose.offset().y(), data.bodyPose.offset().z());
            stack.mulPose(data.bodyPose.rotation());
            float layerScale = (1 + layer.layerId * 0.15f) / 1.05f;
            float x = data.auraScaleX * layerScale, y = data.auraScaleY * layerScale, z = data.auraScaleZ * layerScale;
            if ("sparking".equals(type)) { stack.scale(x * 0.936f, y * 1.325f, z * 0.936f); stack.translate(0, 0.889f, 0); }
            else if ("god".equals(type)) stack.scale(x * 0.90f, y * 1.80f, z * 0.90f);
            else { stack.translate(0, 1.15f * y, 0); stack.scale(0.80f * x, 1.35f * y, 0.80f * z); }
			return Aura3DRenderer.enqueue(type, stack, AuraStyle.resolve(config, layer.color),
					alpha * layer.alpha * chargeEnvelope * (firstPerson ? 0.2f : 1), growth,
                    (float) ((player.tickCount + partialTick) / 20.0 * AuraFxState.auraSpeedMultiplier(player)), firstPerson ? 1 : "god".equals(type) ? 0.45f : 0.01f);
        } finally { stack.popPose(); }
    }

	private static float chargeVisualEnvelope(Player player, StatsData stats, float partialTick) {
		boolean surgeBurstAura = AuraFxState.isSurging(stats) && KiChargeEffects.hasTriggeredBurst(player);
		boolean chargeOnlyAura = !stats.getStatus().isAuraActive() && !stats.getStatus().isPermanentAura();
		return surgeBurstAura || chargeOnlyAura
				? KiChargeEffects.chargeEnvelope(player, partialTick) : 1.0f;
	}

	private static void executeAuraShaderDraw(Player player, CachedAuraData data, AuraLayer layer, PoseStack poseStack, Minecraft mc, SubmitNodeCollector collector, float partialTick, float alphaMultiplier, boolean isFirstPerson) {
		if (AuraModeState.isAura3D(player) && enqueueAura3D(player, data, layer, poseStack, partialTick, alphaMultiplier, isFirstPerson)) return;
		// 26.2: the old VertexBuffer/ShaderInstance pair is gone. Aura geometry is now replayed from
		// AuraMeshFactory quads at submit time, so these are vertex lists rather than GPU buffers.
		java.util.List<com.dragonminez.client.render.util.KiMeshFactory.Vertex> mesh =
				toQuadVertices(AuraMeshFactory.billboardQuad(), 0xFFFFFFFF);
		java.util.List<com.dragonminez.client.render.util.KiMeshFactory.Vertex> groundMesh =
				toQuadVertices(AuraMeshFactory.groundQuad(), 0xFFFFFFFF);

		boolean isLocalPlayer = player == mc.player;
		boolean localThirdPerson = isLocalPlayer && !isFirstPerson;
		var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		float chargeEnvelope = stats == null ? 1.0f : chargeVisualEnvelope(player, stats, partialTick);
		float maxAlpha = (isLocalPlayer && isFirstPerson) ? 0.5f : 1.0f;
		float finalAlpha = maxAlpha * alphaMultiplier * chargeEnvelope * layer.alpha;

		String typeStr = layer.type != null && !layer.type.isEmpty() ? layer.type.toLowerCase() : "kakarot";
		Identifier mainTex = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/aura/" + typeStr + "_aura.png");
		Identifier crossTex = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/aura/" + typeStr + "_cross.png");
		Identifier sparkingTex = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/aura/sparking_effects.png");

		float animSpeed = (player.tickCount + partialTick) * 0.5f;


		float baseMultiplier = 2.2f * (0.55f + chargeEnvelope * 0.45f);
		float finalScaleX = data.auraScaleX * baseMultiplier * (1.0f + layer.layerId * 0.15f);
		float finalScaleY = data.auraScaleY * baseMultiplier * (1.0f + layer.layerId * 0.15f);
		float finalScaleZ = data.auraScaleZ * baseMultiplier * (1.0f + layer.layerId * 0.15f);


		if (isLocalPlayer && isFirstPerson) {
			poseStack.pushPose();
			poseStack.last().pose().identity();
			poseStack.last().normal().identity();
			poseStack.mulPose(mc.gameRenderer.mainCamera().rotation());
			poseStack.translate(0.0, -0.6, -0.7);

			float normalizedScaleX = finalScaleX / (data.modelScaleX > 0 ? data.modelScaleX : 1.0f);
			float normalizedScaleY = finalScaleY / (data.modelScaleY > 0 ? data.modelScaleY : 1.0f);

			poseStack.scale(normalizedScaleX * 3.0f, normalizedScaleY * 3.0f, 1.0f);

			RenderType mainRender = auraType(mainTex, localThirdPerson);
			customSetup(mainRender, mainTex, null);
			applyAndDraw(mesh, poseStack, collector, auraType(mainTex), net.minecraft.util.ARGB.colorFromFloat(finalAlpha * 0.45f, layer.color[0], layer.color[1], layer.color[2]));
			customClear(mainRender);

			RenderType sparkingRender = auraType(sparkingTex, localThirdPerson);
			customSetup(sparkingRender, sparkingTex, null);
			poseStack.scale(0.6f, 0.45f, 0.6f);
			applyAndDraw(mesh, poseStack, collector, auraType(sparkingTex), net.minecraft.util.ARGB.colorFromFloat(finalAlpha * 0.45f, layer.color[0], layer.color[1], layer.color[2]));
			customClear(sparkingRender);

			poseStack.popPose();
			return;
		}

		AuraPose bodyPose = data.bodyPose;
		var cameraRotation = mc.gameRenderer.mainCamera().rotation();
		float crossFactor = bodyPose.endOnAmount(cameraRotation);
		float pitchSquash = 1.0f - crossFactor * 0.5f;
		var billboard = bodyPose.billboard(cameraRotation);
		poseStack.pushPose();
		poseStack.translate(bodyPose.offset().x(), bodyPose.offset().y(), bodyPose.offset().z());

		if (crossFactor < 1.0f) {
			poseStack.pushPose();

			poseStack.translate(0.0, 0.05, 0.0);
			poseStack.mulPose(billboard);
			poseStack.scale(finalScaleX, finalScaleY * pitchSquash, finalScaleZ);

			poseStack.translate(0.0, 0.7, 0.0);

			RenderType mainRender = auraType(mainTex, localThirdPerson);
			customSetup(mainRender, mainTex, null);
			applyAndDraw(mesh, poseStack, collector, auraType(mainTex, localThirdPerson), net.minecraft.util.ARGB.colorFromFloat((1.0f - crossFactor) * finalAlpha, layer.color[0], layer.color[1], layer.color[2]));
			customClear(mainRender);

			poseStack.pushPose();
			float sparkingPulse = 1.0f + (float) Math.sin((player.tickCount + partialTick) * 0.2f) * 0.05f;
			poseStack.scale(0.8f * sparkingPulse, 0.65f * sparkingPulse, 0.8f * sparkingPulse);

			poseStack.translate(0.0, -0.25, 0.0);

			RenderType sparkingRender = auraType(sparkingTex, localThirdPerson);
			customSetup(sparkingRender, sparkingTex, null);
			applyAndDraw(mesh, poseStack, collector, auraType(sparkingTex, localThirdPerson), net.minecraft.util.ARGB.colorFromFloat((1.0f - crossFactor) * finalAlpha * 0.8f, layer.color[0], layer.color[1], layer.color[2]));
			customClear(sparkingRender);
			poseStack.popPose();

			poseStack.popPose();
		}

		if (crossFactor > 0.0f) {
			poseStack.pushPose();
			poseStack.translate(0.0, 0.05, 0.0);
			poseStack.mulPose(billboard);
			if (cameraRotation.transform(new org.joml.Vector3f(0, 0, 1)).dot(bodyPose.up()) < 0) {
				poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
			}
			poseStack.scale(finalScaleX, 1.0f, finalScaleZ);

			RenderType crossRender = auraType(crossTex, localThirdPerson);
			customSetup(crossRender, crossTex, null);
			applyAndDraw(groundMesh, poseStack, collector, auraType(crossTex, localThirdPerson), net.minecraft.util.ARGB.colorFromFloat(crossFactor * finalAlpha, layer.color[0], layer.color[1], layer.color[2]));
			customClear(crossRender);

			poseStack.popPose();
		}
		poseStack.popPose();

	}

	private static void renderShaderPulseAura(Player player, CachedAuraData data, AuraLayer topLayer, PoseStack poseStack, Minecraft mc, SubmitNodeCollector collector, float partialTick, float alphaMultiplier) {
		int playerId = player.getId();
		long gameTime = player.level().getGameTime();

		if (gameTime - PULSE_LAST_RENDER_TIME.getOrDefault(playerId, 0L) > 2) {
			PULSE_PROGRESS.put(playerId, 0.0f);
		}
		PULSE_LAST_RENDER_TIME.put(playerId, gameTime);

		float currentProgress = PULSE_PROGRESS.getOrDefault(playerId, 0.0f);

		if (!mc.isPaused()) {
			currentProgress += PULSE_SPEED;
			if (currentProgress >= 1.0f) currentProgress -= 1.0f;
			PULSE_PROGRESS.put(playerId, currentProgress);
		}

		drawSinglePulseInstance(player, data, topLayer, poseStack, mc, collector, partialTick, alphaMultiplier, currentProgress);

		float progressPhase2 = (currentProgress + 0.5f) % 1.0f;
		drawSinglePulseInstance(player, data, topLayer, poseStack, mc, collector, partialTick, alphaMultiplier, progressPhase2);
	}

	private static void drawSinglePulseInstance(Player player, CachedAuraData data, AuraLayer topLayer, PoseStack poseStack, Minecraft mc, SubmitNodeCollector collector, float partialTick, float alphaMultiplier, float progress) {
		java.util.List<com.dragonminez.client.render.util.KiMeshFactory.Vertex> mesh =
				toQuadVertices(AuraMeshFactory.groundQuad(), 0xFFFFFFFF);
		float expansion = 1.0f + (6.0f * progress);
		float alphaCurve = (float) Math.sin(progress * Math.PI);

		String typeStr = topLayer.type != null && !topLayer.type.isEmpty() ? topLayer.type.toLowerCase() : "kakarot";
		Identifier crossTex = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/aura/" + typeStr + "_cross.png");


		poseStack.pushPose();

		float cameraPitch = mc.gameRenderer.mainCamera().xRot();
		if (cameraPitch < 0.0f) {
			poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
		}

		float layerScaleBoost = 1.0f + (topLayer.layerId * 0.15f);
		float scaleMultiplier = 0.5f;
		float sX = data.auraScaleX * expansion * scaleMultiplier * layerScaleBoost;
		float sZ = data.auraScaleZ * expansion * scaleMultiplier * layerScaleBoost;

		poseStack.scale(sX, 1.0f, sZ);

		float animSpeed = (player.tickCount + partialTick) * 0.5f;




		boolean localThirdPerson = player == mc.player && !mc.options.getCameraType().isFirstPerson();
		RenderType pulseRender = auraType(crossTex, localThirdPerson);
		customSetup(pulseRender, crossTex, null);

		applyAndDraw(mesh, poseStack, collector, auraType(crossTex, localThirdPerson), net.minecraft.util.ARGB.colorFromFloat(alphaCurve * 0.6f * alphaMultiplier, topLayer.color[0], topLayer.color[1], topLayer.color[2]));

		customClear(pulseRender);

		poseStack.popPose();
	}

	/**
	 * Unit lightning bolt mesh (a 4-plane, 3-block-tall cross), reused by the lightning renderers.
	 *
	 * <h2>26.2 migration</h2>
	 * This used to return a {@code VertexBuffer} uploaded once and re-bound per draw. Both
	 * {@code VertexBuffer} and the {@code Tesselator} singleton are gone, so the mesh is now plain
	 * vertex data replayed into the frame's {@code SubmitNodeCollector}. Callers use
	 * {@link com.dragonminez.client.render.util.KiMeshFactory#emit(java.util.List, com.mojang.blaze3d.vertex.PoseStack.Pose, com.mojang.blaze3d.vertex.VertexConsumer, int)}.
	 */
	public static java.util.List<com.dragonminez.client.render.util.KiMeshFactory.Vertex> getLightningMesh() {
		if (cachedLightningMesh == null) {
			java.util.List<com.dragonminez.client.render.util.KiMeshFactory.Vertex> out = new java.util.ArrayList<>();

			float w = 0.15f;
			float h = 3.0f;
			int segments = 20;
			float segHeight = h / segments;
			int white = 0xFFFFFFFF;

			for (int i = 0; i < segments; i++) {
				float y1 = i * segHeight;
				float y2 = (i + 1) * segHeight;

				out.add(new com.dragonminez.client.render.util.KiMeshFactory.Vertex(-w, y1, 0, 0f, 0f, 0, 0, 1, white));
				out.add(new com.dragonminez.client.render.util.KiMeshFactory.Vertex(w, y1, 0, 0f, 0f, 0, 0, 1, white));
				out.add(new com.dragonminez.client.render.util.KiMeshFactory.Vertex(w, y2, 0, 0f, 0f, 0, 0, 1, white));
				out.add(new com.dragonminez.client.render.util.KiMeshFactory.Vertex(-w, y2, 0, 0f, 0f, 0, 0, 1, white));

				out.add(new com.dragonminez.client.render.util.KiMeshFactory.Vertex(0, y1, -w, 0f, 0f, 1, 0, 0, white));
				out.add(new com.dragonminez.client.render.util.KiMeshFactory.Vertex(0, y1, w, 0f, 0f, 1, 0, 0, white));
				out.add(new com.dragonminez.client.render.util.KiMeshFactory.Vertex(0, y2, w, 0f, 0f, 1, 0, 0, white));
				out.add(new com.dragonminez.client.render.util.KiMeshFactory.Vertex(0, y2, -w, 0f, 0f, 1, 0, 0, white));
			}

			cachedLightningMesh = java.util.List.copyOf(out);
		}
		return cachedLightningMesh;
	}


	private static void renderSparksImpl(Player player, PoseStack poseStack, SubmitNodeCollector collector, float partialTick,
            boolean firstPerson, AuraPose bodyPose, net.minecraft.client.renderer.state.level.CameraRenderState camera) {
        var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        if (stats == null || !AuraFxState.hasLightning(stats)) return;
        var status = stats.getStatus();
        boolean active = status.isAuraActive() || status.isPermanentAura() || status.isForcedAura();
        float[] color = ColorUtils.hexToRgb(AuraFxState.lightningColor(stats));
        float age = player.tickCount + partialTick;
        float speed = AuraFxState.lightningSpeedMultiplier(stats);
        if (firstPerson) {
            LightningBoltRenderer.submitFirstPerson(collector, camera, player.getId(), age, color, active, speed, 0.275f);
            return;
        }

        // Rebuild camera-relative coordinates for this frame; retain Sable and the animated body pose.
        Vec3 position = new Vec3(Mth.lerp(partialTick, player.xo, player.getX()),
                Mth.lerp(partialTick, player.yo, player.getY()), Mth.lerp(partialTick, player.zo, player.getZ()));
        position = SableCompat.projectToWorld(player.level(), position);
        poseStack.pushPose();
        try {
            poseStack.translate(position.x - camera.pos.x, position.y - camera.pos.y, position.z - camera.pos.z);
            poseStack.translate(bodyPose.offset().x(), bodyPose.offset().y(), bodyPose.offset().z());
            poseStack.mulPose(bodyPose.rotation());
            LightningBoltRenderer.submitPlayer(poseStack, collector, camera, player.getId(), age, color, active, speed, 1.0f);
        } finally {
            poseStack.popPose();
        }
    }

	private static void renderFusionFlash(Player player, float time, PoseStack poseStack, SubmitNodeCollector buffer, int r, int g, int b) {
		float rotationTime = time * 0.01F;
		float rawSin = Mth.sin(time * 0.1F);
		float normalizedFade = (rawSin + 1.0F) / 2.0F;
		float fade = 0.4F + (normalizedFade * 0.6F);
		float intensity = 0.6F;

		RandomSource randomsource = RandomSource.create(432L);
		// 26.2: SubmitNodeCollector has no getBuffer; vertices go through submitCustomGeometry instead.
        final RenderType fusionLightning = RenderTypes.lightning();

		poseStack.pushPose();

		Vec3 cameraPos = Minecraft.getInstance().gameRenderer.mainCamera().position();
		poseStack.translate(player.getX() - cameraPos.x, (player.getY() + 1.0) - cameraPos.y, player.getZ() - cameraPos.z);
		poseStack.scale(1.0F, 1.0F, 1.0F);

		for (int i = 0; (float) i < (intensity + intensity * intensity) / 2.0F * 60.0F; ++i) {
			poseStack.mulPose(Axis.XP.rotationDegrees(randomsource.nextFloat() * 360.0F));
			poseStack.mulPose(Axis.YP.rotationDegrees(randomsource.nextFloat() * 360.0F));
			poseStack.mulPose(Axis.ZP.rotationDegrees(randomsource.nextFloat() * 360.0F));
			poseStack.mulPose(Axis.XP.rotationDegrees(randomsource.nextFloat() * 360.0F));
			poseStack.mulPose(Axis.YP.rotationDegrees(randomsource.nextFloat() * 360.0F));
			poseStack.mulPose(Axis.ZP.rotationDegrees(randomsource.nextFloat() * 360.0F + rotationTime * 90.0F));

			float width = randomsource.nextFloat() * 5.0F + 4.0F;
			float length = randomsource.nextFloat() + 0.5F;
			int alpha = (int) (255.0F * fade);
			final int a = alpha;
			final float w = width;
			final float len = length;
			final int fr = r, fg = g, fb = b;

			// 26.2: the vertex stream comes from the submit callback, not from a buffer lookup.
			buffer.submitCustomGeometry(poseStack, fusionLightning, (pose, vc) -> {
				org.joml.Matrix4f matrix4f = pose.pose();
				vertex01(vc, matrix4f, a, fr, fg, fb);
				vertex2(vc, matrix4f, w, len, fr, fg, fb, a);
				vertex3(vc, matrix4f, w, len, fr, fg, fb, a);
				vertex01(vc, matrix4f, a, fr, fg, fb);
				vertex3(vc, matrix4f, w, len, fr, fg, fb, a);
				vertex4(vc, matrix4f, w, len, fr, fg, fb, a);
				vertex01(vc, matrix4f, a, fr, fg, fb);
				vertex4(vc, matrix4f, w, len, fr, fg, fb, a);
				vertex2(vc, matrix4f, w, len, fr, fg, fb, a);
			});
		}

		poseStack.popPose();
	}

	private static void vertex01(VertexConsumer pConsumer, Matrix4f pMatrix, int pAlpha, int r, int g, int b) {
		pConsumer.addVertex(pMatrix, 0.0F, 0.0F, 0.0F).setColor(r, g, b, pAlpha);
	}

	private static void vertex2(VertexConsumer pConsumer, Matrix4f pMatrix, float pWidth, float pLength, int r, int g, int b, int alpha) {
		pConsumer.addVertex(pMatrix, -HALF_SQRT_3 * pLength, pWidth, -0.5F * pLength).setColor(r, g, b, alpha);
	}

	private static void vertex3(VertexConsumer pConsumer, Matrix4f pMatrix, float pWidth, float pLength, int r, int g, int b, int alpha) {
		pConsumer.addVertex(pMatrix, HALF_SQRT_3 * pLength, pWidth, -0.5F * pLength).setColor(r, g, b, alpha);
	}

	private static void vertex4(VertexConsumer pConsumer, Matrix4f pMatrix, float pWidth, float pLength, int r, int g, int b, int alpha) {
		pConsumer.addVertex(pMatrix, 0.0F, pWidth, pLength).setColor(r, g, b, alpha);
	}
}


