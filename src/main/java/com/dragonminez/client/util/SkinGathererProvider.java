package com.dragonminez.client.util;

import com.dragonminez.Reference;
import com.dragonminez.client.render.layer.DMZSkinLayer;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.common.util.lists.FrostDemonForms;
import com.dragonminez.common.util.lists.MajinForms;
import com.dragonminez.common.util.lists.SaiyanForms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.server.packs.resources.Resource;
import java.util.Optional;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class SkinGathererProvider {

	public static SkinGathererProvider INSTANCE = new SkinGathererProvider();

	private static final Set<String> BUILTIN_RACES = Set.of(
			"human", "saiyan", "namekian", "majin", "frostdemon", "bioandroid", "glind"
	);

	public static boolean isBuiltInRace(String race) {
		return race != null && BUILTIN_RACES.contains(race.toLowerCase());
	}

	public static String modelFamily(String key) {
		if (key == null || key.isEmpty()) return "human";
		String k = key.toLowerCase();
		if (k.startsWith("oozaru")) return "oozaru";
		if (k.startsWith("namekian")) return "namekian";
		if (k.startsWith("frostdemon")) return "frostdemon";
		if (k.startsWith("bioandroid")) return "bioandroid";
		if (k.startsWith("majin") || k.startsWith("janemba")) return "majin";
		if (k.startsWith("human") || k.startsWith("saiyan") || k.contains("ssj4d") || k.contains("ssj4gt")
				|| k.startsWith("buffed") || k.equals("4arms") || k.equals("glinddark")) return "human";
		return "custom";
	}

	public static boolean isHumanoidKey(String logicKey) {
		return logicKey.equals("human") || logicKey.equals("saiyan") || logicKey.contains("ssj4d")
				|| logicKey.contains("ssj4gt") || logicKey.equals("buffed") || logicKey.equals("buffedg3") || logicKey.equals("4arms")
				|| logicKey.equals("glinddark");
	}

	public interface BodyLayerSink extends BiConsumer<Identifier, float[]> {
		@Override
		default void accept(Identifier texture, float[] color) {
			base(texture, color);
		}

		void base(Identifier texture, float[] color);

		default void overlay(Identifier texture, float[] color) { base(texture, color); }
		default void translucent(Identifier texture, float[] color) { base(texture, color); }
		default void overlayTranslucent(Identifier texture, float[] color) { translucent(texture, color); }

		void fading(String layerId, Identifier texture, float[] color, float targetAlpha);

		default void fading(String layerId, Identifier texture, float[] color) {
			fading(layerId, texture, color, 1.0f);
		}
	}

	private static final Map<String, Identifier> TEXTURE_CACHE = new ConcurrentHashMap<>();

	private static final Identifier[] UNSHADED = new Identifier[0];
	private static final Map<Identifier, Identifier[]> SHADED_LAYERS = new HashMap<>();
	private static final float[] WHITE_COLOR = {1.0f, 1.0f, 1.0f};
	private static final float WHITE_SHADOW = ColorUtils.skinShadowTone(WHITE_COLOR)[0];
	private static final float[] DEFAULT_TAIL_COLOR = ColorUtils.hexToRgb("#572117");
	private static final float[] DEFAULT_ORANGE_COLOR = ColorUtils.hexToRgb("#e67d40");
	private static final float[] DEFAULT_STINGER_COLOR = ColorUtils.hexToRgb("#D9B28D");

	public static Identifier getCachedTexture(String path) {
		return TEXTURE_CACHE.computeIfAbsent(path, p -> Identifier.fromNamespaceAndPath(Reference.MOD_ID, p));
	}

	private void emitFadingLayer(BiConsumer<Identifier, float[]> consumer, String layerId, Identifier texture, float[] color) {
		if (consumer instanceof BodyLayerSink sink) sink.fading(layerId, texture, color);
		else consumer.accept(texture, color);
	}

	private void emitOverlayLayer(BiConsumer<Identifier, float[]> consumer, Identifier texture, float[] color) {
		if (consumer instanceof BodyLayerSink sink) sink.overlay(texture, color);
		else consumer.accept(texture, color);
	}

	private void emitTranslucentLayer(BiConsumer<Identifier, float[]> consumer, Identifier texture, float[] color) {
		if (consumer instanceof BodyLayerSink sink) sink.translucent(texture, color);
		else consumer.accept(texture, color);
	}

	private void emitExtraFormLayer(BiConsumer<Identifier, float[]> consumer, String layerId, FormConfig.FormData form) {
		Identifier extraTex = resolveConfiguredTexture(form.getExtraFormLayer());
		if (extraTex == null) return;
		float[] extraColor = form.getRgbExtraFormColor() != null ? form.getRgbExtraFormColor() : WHITE_COLOR;
		emitFadingLayer(consumer, layerId, DMZSkinLayer.getSafeTexture(extraTex), extraColor);
	}

	private Identifier resolveConfiguredTexture(String path) {
		if (path.contains(":")) return Identifier.tryParse(path);
		return getCachedTexture(path);
	}

	public void gatherBodyLayers(AbstractClientPlayer player, StatsData stats, float partialTick, BiConsumer<Identifier, float[]> consumer) {
		var character = stats.getCharacter();
		String raceName = character.getRaceName().toLowerCase();
		int bodyType = character.getBodyType();
		String currentForm = character.getActiveForm();

		RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(raceName);
		if (raceConfig == null) return;

		String raceCustomModel = raceConfig.getCustomModel() != null ? raceConfig.getCustomModel().toLowerCase() : "";
		String formCustomModel = "";

		boolean hasStackForm = character.hasActiveStackForm() && character.getActiveStackFormData() != null;
		boolean hasForm = character.hasActiveForm() && character.getActiveFormData() != null;

		if (hasStackForm && character.getActiveStackFormData().hasCustomModel()) {
			formCustomModel = character.getActiveStackFormData().getCustomModel().toLowerCase();
		} else if (hasForm && character.getActiveFormData().hasCustomModel()) {
			formCustomModel = character.getActiveFormData().getCustomModel().toLowerCase();
		}

		String key = formCustomModel.isEmpty() ? raceCustomModel : formCustomModel;
		if (key.isEmpty()) key = isBuiltInRace(raceName) ? raceName : "human";

		String logicKey = key;
		if (key.equals("human_slim") || key.equals("majin_slim") || key.equals("base_slim")) {
			logicKey = raceName;
		}

		float[] b1 = character.getRgbBodyColor();
		float[] b2 = character.getRgbBodyColor2();
		float[] b3 = character.getRgbBodyColor3();
		float[] hair = character.getRgbHairColor();

		if (hasForm) {
			var f = character.getActiveFormData();
			if (f.getRgbBodyColor1() != null) b1 = f.getRgbBodyColor1();
			if (f.getRgbBodyColor2() != null) b2 = f.getRgbBodyColor2();
			if (f.getRgbBodyColor3() != null) b3 = f.getRgbBodyColor3();
			if (f.getRgbHairColor() != null) hair = f.getRgbHairColor();
		}

		if (hasStackForm) {
			var sf = character.getActiveStackFormData();
			if (sf.getRgbBodyColor1() != null) b1 = sf.getRgbBodyColor1();
			if (sf.getRgbBodyColor2() != null) b2 = sf.getRgbBodyColor2();
			if (sf.getRgbBodyColor3() != null) b3 = sf.getRgbBodyColor3();
			if (sf.getRgbHairColor() != null) hair = sf.getRgbHairColor();
		}

		if (stats.getStatus().isActionCharging()) {
			FormConfig.FormData nextForm = null;
			if (stats.getStatus().getSelectedAction() == ActionMode.FORM) nextForm = TransformationsHelper.getNextAvailableForm(stats);
			else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) nextForm = TransformationsHelper.getNextAvailableStackForm(stats);

			if (nextForm != null) {
				float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
				if (nextForm.getRgbBodyColor1() != null) b1 = DMZSkinLayer.lerpColor(factor, b1, nextForm.getRgbBodyColor1());
				if (nextForm.getRgbBodyColor2() != null) b2 = DMZSkinLayer.lerpColor(factor, b2, nextForm.getRgbBodyColor2());
				if (nextForm.getRgbBodyColor3() != null) b3 = DMZSkinLayer.lerpColor(factor, b3, nextForm.getRgbBodyColor3());
				if (nextForm.getRgbHairColor() != null) hair = DMZSkinLayer.lerpColor(factor, hair, nextForm.getRgbHairColor());
			}
		}

		if (hasForm && character.getActiveFormData().hasExtraFormLayer()) {
			emitExtraFormLayer(consumer, "extraform_form", character.getActiveFormData());
		}
		if (hasStackForm && character.getActiveStackFormData().hasExtraFormLayer()) {
			emitExtraFormLayer(consumer, "extraform_stack", character.getActiveStackFormData());
		}

		boolean isOozaruForm = raceName.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU));

		if (logicKey.equals("oozaru") || isOozaruForm) {
			resolveBodyOozaru(b1, b2, consumer);
			return;
		}

		boolean isSaiyanLogic = logicKey.equals("saiyan") || logicKey.contains("ssj4gt") || logicKey.contains("ssj4d") || raceName.equals("saiyan");
		boolean hasSaiyanTail = raceConfig.getHasSaiyanTail() != null && raceConfig.getHasSaiyanTail();
		boolean isSSJ4Active = currentForm != null && (currentForm.contains("supersaiyan4") || currentForm.contains("ssj4"));
		boolean renderSaiyanTail = (isSaiyanLogic || hasSaiyanTail) && (isSSJ4Active || (stats.getStatus().isTailVisible() && character.isHasSaiyanTail()));

		boolean isHumanoid = isHumanoidKey(logicKey);

		if (isHumanoid && bodyType == 0) {
			consumer.accept(player.getSkin().body().texturePath(), WHITE_COLOR);
		} else if (isHumanoid) {
			resolveBodyHumanSaiyan(character, logicKey, b1, b2, b3, consumer);
		} else {
            switch (logicKey) {
                case "namekian", "namekian_orange", "namekian_buffed" -> resolveBodyNamekian(character, b1, b2, b3, consumer);
                case "majin", "majin_super", "majin_ultra", "majin_evil", "majin_kid", "janemba_imperfect", "janemba_fat","janemba_super" -> resolveBodyMajin(character, logicKey, b1, b2, b3, consumer);
                case "frostdemon", "frostdemon_second", "frostdemon_final", "frostdemon_fifth", "frostdemon_third", "frostdemon_fp", "frostdemon_mecha", "frostdemon_metalcore" -> resolveBodyFrostDemon(character, logicKey, b1, b2, b3, hair, consumer);
                case "bioandroid", "bioandroid_semi", "bioandroid_perfect", "bioandroid_base", "bioandroid_ultra", "bioandroid_xeno", "bioandroid_xenofp" -> resolveBodyBioAndroid(character, logicKey, b1, b2, b3, hair, consumer);
				case "glindtrueform" -> resolveBodyGlindTrueForm(b1, b2, consumer);
				case "trascended" -> resolveBodyGlindTranscended(character, b1, b2, consumer);
				default -> {
					boolean hasGender = Boolean.TRUE.equals(raceConfig.getHasGender());
					String genSuffix = hasGender ? (character.getGender().equals(Character.GENDER_FEMALE) ? "_female" : "_male") : "";

					if (Boolean.TRUE.equals(raceConfig.getUseVanillaSkin()) && bodyType == 0) {
						consumer.accept(player.getSkin().body().texturePath(), WHITE_COLOR);
						break;
					}

					if (Boolean.TRUE.equals(raceConfig.getIsLayered())) {
						String prefix = "textures/entity/races/" + raceName + "/" + logicKey + genSuffix + "_" + bodyType + "_";
						String fallbackPrefix = "textures/entity/races/" + raceName + "/" + logicKey + genSuffix + "_0_";

						consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer1.png"), getCachedTexture(fallbackPrefix + "layer1.png")), b1);
						consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer2.png")), b2);
						consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer3.png")), b3);
					} else {
						Identifier customTex = getCachedTexture("textures/entity/races/" + raceName + "/" + logicKey + genSuffix + ".png");
						consumer.accept(DMZSkinLayer.getSafeTexture(customTex), b1);
					}
				}
            }
        }

		if (logicKey.equals("glinddark")) resolveBodyGlindDark(b2, b3, consumer);

		if (renderSaiyanTail && !formHidesTail(character)) {
			float[] tailColor = b2 != null ? b2 : DEFAULT_TAIL_COLOR;
			consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture("textures/entity/races/tail1.png")), tailColor);
		}
    }

	private static boolean formHidesTail(Character character) {
        var form = character.getActiveFormData();
        var stack = character.getActiveStackFormData();
        return (form != null && form.hidesTail()) || (stack != null && stack.hidesTail());
    }

	public void gatherAndroidLayers(AbstractClientPlayer player, StatsData stats, float partialTick, BiConsumer<Identifier, float[]> consumer) {
		var character = stats.getCharacter();
		String raceName = character.getRace().toLowerCase();
		boolean canBeUpgraded = ConfigManager.getRaceCharacter(raceName) != null && ConfigManager.getRaceCharacter(raceName).getFormSkillTpCosts("androidforms").length > 0;
		if (!canBeUpgraded || !stats.getStatus().isAndroidUpgraded()) return;

		String androidPath = character.getGender().equals(Character.GENDER_FEMALE) ? "textures/entity/races/female_android.png" : "textures/entity/races/male_android.png";
		emitFadingLayer(consumer, "android", DMZSkinLayer.getSafeTexture(getCachedTexture(androidPath)), WHITE_COLOR);
	}

	public void gatherTattooLayers(AbstractClientPlayer player, StatsData stats, float partialTick, BiConsumer<Identifier, float[]> consumer) {
		int tattooType = stats.getCharacter().getTattooType();
		if (tattooType == 0) return;

		consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture("textures/entity/races/tattoos/tattoo_" + tattooType + ".png")), WHITE_COLOR);
	}

	public void gatherEffectLayers(AbstractClientPlayer player, StatsData stats, float partialTick, BiConsumer<Identifier, float[]> consumer) {
		if (stats.getEffects() != null && stats.getEffects().hasEffect("majin")) {
			consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture("textures/entity/races/majinm.png")), WHITE_COLOR);
		}
	}

	protected void resolveBodyHumanSaiyan(Character character, String key, float[] bodyColor, float[] bodyColor2, float[] bodyColor3, BiConsumer<Identifier, float[]> consumer) {
		int bodyType = character.getBodyType();
        String gender = character.getGender().toLowerCase().trim();
        String genderPart = (gender.equals(Character.GENDER_FEMALE)) ? "_female" : "_male";
		String basePath = "textures/entity/races/humansaiyan/bodytype" + genderPart + "_" + bodyType + "_";
		String fallbackPath = "textures/entity/races/humansaiyan/bodytype" + genderPart + "_0_";
		RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(character.getRaceName());
		if (bodyType != 0 && raceConfig != null && raceConfig.isSlimBodyType(bodyType) && !key.equals("glinddark")) {
			Identifier slimLayer = getCachedTexture(basePath + "slim_layer1.png");
			if (DMZSkinLayer.getSafeTexture(slimLayer).equals(slimLayer)) basePath = basePath + "slim_";
		}

		Identifier shadowLayer = getCachedTexture(basePath + "layer1_shadow.png");
		boolean shaded = DMZSkinLayer.getSafeTexture(shadowLayer).equals(shadowLayer);

		consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer1.png"), getCachedTexture(fallbackPath + "layer1.png")), shaded ? ColorUtils.skinBaseTone(bodyColor) : bodyColor);
		if (shaded) emitTranslucentLayer(consumer, shadowLayer, ColorUtils.skinShadowTone(bodyColor));
	}

	protected void resolveBodyGlindTrueForm(float[] bodyColor, float[] bodyColor2, BiConsumer<Identifier, float[]> consumer) {
		String basePath = "textures/entity/races/glind/trueform_";
		Identifier layer1 = DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer1.png"));
		consumer.accept(layer1, ColorUtils.skinBaseTone(bodyColor));
		emitShadowLayer(consumer, layer1, bodyColor);
		acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer2.png")), bodyColor2);
	}

	protected void resolveBodyGlindDark(float[] bodyColor2, float[] bodyColor3, BiConsumer<Identifier, float[]> consumer) {
		String basePath = "textures/entity/races/glind/darkking_";
		emitOverlayLayer(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer2.png")), bodyColor2);
		emitOverlayLayer(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer3.png")), bodyColor3);
	}

	protected void resolveBodyGlindTranscended(Character character, float[] bodyColor, float[] bodyColor2, BiConsumer<Identifier, float[]> consumer) {
		boolean female = Character.GENDER_FEMALE.equalsIgnoreCase(character.getGender());
		String fallbackPath = female ? "textures/entity/races/humansaiyan/bodytype_female_1_" : "textures/entity/races/humansaiyan/bodytype_male_1_";
		String bodyPath = female ? "textures/entity/races/humansaiyan/bodytype_female_" + Math.max(1, character.getBodyType()) + "_" : fallbackPath;
		Identifier bodyLayer = getCachedTexture(bodyPath + "layer1.png");
		if (!DMZSkinLayer.getSafeTexture(bodyLayer).equals(bodyLayer)) bodyPath = fallbackPath;
		consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(bodyPath + "layer1.png")), ColorUtils.skinBaseTone(bodyColor));
		Identifier bodyShadow = getCachedTexture(bodyPath + "layer1_shadow.png");
		if (DMZSkinLayer.getSafeTexture(bodyShadow).equals(bodyShadow)) emitTranslucentLayer(consumer, bodyShadow, ColorUtils.skinShadowTone(bodyColor));
		Identifier overlay = DMZSkinLayer.getSafeTexture(getCachedTexture("textures/entity/races/glind/trascended_layer2.png"));
		emitOverlayLayer(consumer, overlay, ColorUtils.skinBaseTone(bodyColor2));
		Identifier overlayShadow = getCachedTexture("textures/entity/races/glind/trascended_layer2_shadow.png");
		if (!DMZSkinLayer.getSafeTexture(overlayShadow).equals(overlayShadow)) return;
		float[] shadowTone = ColorUtils.skinShadowTone(bodyColor2);
		if (consumer instanceof BodyLayerSink sink) sink.overlayTranslucent(overlayShadow, shadowTone);
		else consumer.accept(overlayShadow, shadowTone);
	}

	protected void resolveBodyOozaru(float[] bodyColor, float[] bodyColor2, BiConsumer<Identifier, float[]> consumer) {
		String basePath = "textures/entity/races/humansaiyan/oozaru_";
		consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer1.png"), getCachedTexture(basePath + "layer1.png")), bodyColor2);
		consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer2.png"), getCachedTexture(basePath + "layer2.png")), bodyColor);
		emitOverlayLayer(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer3.png"), getCachedTexture(basePath + "layer3.png")), WHITE_COLOR);
	}

    protected void resolveBodyNamekian(Character character, float[] c1, float[] c2, float[] c3, BiConsumer<Identifier, float[]> consumer) {
        int bodyType = character.getBodyType();
        var hairColor = character.getRgbHairColor();
        String basePath = "textures/entity/races/namekian/bodytype_" + bodyType + "_";
        String fallbackPath = "textures/entity/races/namekian/bodytype_0_";

        Identifier layer1 = DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer1.png"), getCachedTexture(fallbackPath + "layer1.png"));
        consumer.accept(layer1, c1);
        emitShadowLayer(consumer, layer1, c1);

        emitShadowLayer(consumer, tryLoadOptionalLayerWithFallback(basePath + "layer2.png", fallbackPath + "layer2.png", c2, consumer), c2);
        emitShadowLayer(consumer, tryLoadOptionalLayerWithFallback(basePath + "layer3.png", fallbackPath + "layer3.png", c3, consumer), c3);
        emitShadowLayer(consumer, tryLoadOptionalLayerWithFallback(basePath + "layer4.png", fallbackPath + "layer4.png", hairColor, consumer), hairColor);
    }

    private void acceptWithShadow(BiConsumer<Identifier, float[]> consumer, Identifier layer, float[] color) {
        if (shadowMask(layer) == null) {
            Identifier[] shaded = shadedLayer(layer);
            if (shaded != null) {
                consumer.accept(shaded[0], color);
                emitTranslucentLayer(consumer, shaded[1], ColorUtils.skinShadowTone(color));
                return;
            }
        }
        consumer.accept(layer, color);
        emitShadowLayer(consumer, layer, color);
    }

    private void acceptWithShadowMask(BiConsumer<Identifier, float[]> consumer, Identifier layer, float[] color) {
        consumer.accept(layer, color);
        emitShadowLayer(consumer, layer, color);
    }

    private void emitShadowLayer(BiConsumer<Identifier, float[]> consumer, Identifier layer, float[] color) {
        Identifier shadow = shadowMask(layer);
        if (shadow != null) emitTranslucentLayer(consumer, shadow, ColorUtils.skinShadowTone(color));
    }

    private static Identifier shadowMask(Identifier layer) {
        if (layer == null) return null;
        String path = layer.getPath();
        Identifier shadow = getCachedTexture(path.substring(0, path.length() - ".png".length()) + "_shadow.png");
        return DMZSkinLayer.getSafeTexture(shadow).equals(shadow) ? shadow : null;
    }

    public static void clearShadedLayers() {
        TextureManager manager = Minecraft.getInstance().getTextureManager();
        for (Identifier[] shaded : SHADED_LAYERS.values()) {
            for (Identifier location : shaded) manager.release(location);
        }
        SHADED_LAYERS.clear();
    }

    private static Identifier[] shadedLayer(Identifier layer) {
        if (layer == null || !RenderSystem.isOnRenderThread()) return null;
        Identifier[] shaded = SHADED_LAYERS.computeIfAbsent(layer, SkinGathererProvider::buildShadedLayer);
        return shaded == UNSHADED ? null : shaded;
    }

    private static Identifier[] buildShadedLayer(Identifier layer) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(layer);
        if (resource.isEmpty()) return UNSHADED;
        NativeImage base = null;
        NativeImage shadow = null;
        boolean shaded = false;
        try (InputStream in = resource.get().open(); NativeImage source = NativeImage.read(NativeImage.Format.RGBA, in)) {
            int width = source.getWidth();
            int height = source.getHeight();
            base = new NativeImage(NativeImage.Format.RGBA, width, height, false);
            shadow = new NativeImage(NativeImage.Format.RGBA, width, height, false);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int abgr = source.getPixel(x, y);
                    int a = abgr >>> 24;
                    int r = (abgr >> 16) & 0xFF;
                    int g = (abgr >> 8) & 0xFF;
                    int b = abgr & 0xFF;
                    int max = Math.max(r, Math.max(g, b));
                    if (a == 0 || max == 0 || max == 255) {
                        base.setPixel(x, y, abgr);
                        shadow.setPixel(x, y, 0);
                        continue;
                    }
                    float value = max / 255.0f;
                    float strength = Math.min(1.0f, (1.0f - value) / (1.0f - WHITE_SHADOW));
                    float lift = 255.0f / max;
                    float mask = 1.0f / Math.max(value, WHITE_SHADOW);
                    base.setPixel(x, y, packPixel(a, r * lift, g * lift, b * lift));
                    shadow.setPixel(x, y, packPixel(a * strength, r * mask, g * mask, b * mask));
                    shaded = true;
                }
            }
        } catch (Exception e) {
            shaded = false;
        }
        if (!shaded) {
            if (base != null) base.close();
            if (shadow != null) shadow.close();
            return UNSHADED;
        }
        return new Identifier[]{registerShaded(layer, ".png", base), registerShaded(layer, "_shadow.png", shadow)};
    }

    private static Identifier registerShaded(Identifier layer, String suffix, NativeImage image) {
        String path = layer.getPath();
        if (path.endsWith(".png")) path = path.substring(0, path.length() - ".png".length());
        Identifier location = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "shaded/" + layer.getNamespace() + "/" + path + suffix);
        Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(() -> "DMZ racial skin " + location, image));
        return location;
    }

    private static int packPixel(float a, float r, float g, float b) {
        return (pixelChannel(a) << 24) | (pixelChannel(r) << 16) | (pixelChannel(g) << 8) | pixelChannel(b);
    }

    private static int pixelChannel(float value) {
        return Math.max(0, Math.min(255, Math.round(value)));
    }

	protected void resolveBodyFrostDemon(Character character, String key, float[] b1, float[] b2, float[] b3, float[] hair, BiConsumer<Identifier, float[]> consumer) {
		String currentForm = character.getActiveForm();
		int bodyType = character.getBodyType();
		String folder = "textures/entity/races/frostdemon/";
		String prefix, fallbackPrefix;

		boolean isSecondForm = Objects.equals(currentForm, FrostDemonForms.SECOND_FORM);
		boolean isBase = currentForm == null || currentForm.isEmpty() || currentForm.equalsIgnoreCase("base");
		boolean isBulky = (key.equals("frostdemon") && (isBase || isSecondForm) || key.equals("frostdemon_second"))
				|| key.equals("frostdemon_third");

		if (isBulky) {
			prefix = key.equals("frostdemon_third") ? folder + "thirdform_bodytype_" + bodyType + "_" : folder + "bodytype_" + bodyType + "_";
			fallbackPrefix = key.equals("frostdemon_third") ? folder + "thirdform_bodytype_0_" : folder + "bodytype_0_";
			acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer1.png"), getCachedTexture(fallbackPrefix + "layer1.png")), b1);
			acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer2.png"), getCachedTexture(fallbackPrefix + "layer2.png")), b2);
			acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer3.png"), getCachedTexture(fallbackPrefix + "layer3.png")), b3);
			acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer4.png"), getCachedTexture(fallbackPrefix + "layer4.png")), hair);
			if (bodyType == 0)
				acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer5.png"), getCachedTexture(fallbackPrefix + "layer5.png")), DEFAULT_ORANGE_COLOR);
		} else {
			prefix = key.equals("frostdemon_fifth") ? folder + "fifth_bodytype_" + bodyType + "_" : folder + "finalform_bodytype_" + bodyType + "_";
			fallbackPrefix = key.equals("frostdemon_fifth") ? folder + "fifth_bodytype_0_" : folder + "finalform_bodytype_0_";

			acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer1.png"), getCachedTexture(fallbackPrefix + "layer1.png")), b1);
			acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer2.png"), getCachedTexture(fallbackPrefix + "layer2.png")), (bodyType == 0 || bodyType == 2) ? hair : b2);
			if (bodyType == 1) {
				acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer3.png"), getCachedTexture(fallbackPrefix + "layer3.png")), b3);
				acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer4.png"), getCachedTexture(fallbackPrefix + "layer4.png")), hair);
			} else if (bodyType == 2) {
				acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer3.png"), getCachedTexture(fallbackPrefix + "layer3.png")), hair);
				acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer2.png"), getCachedTexture(fallbackPrefix + "layer2.png")), b2);
			}
		}

		emitFrostDemonFormLayers(key, b1, consumer);
	}

	private void emitFrostDemonFormLayers(String key, float[] b1, BiConsumer<Identifier, float[]> consumer) {
		String folder = "textures/entity/races/frostdemon/";
		if (key.equals("frostdemon_mecha")) {
			Identifier mecha = getCachedTexture(folder + "mechaform_layer1.png");
			consumer.accept(DMZSkinLayer.getSafeTexture(mecha, mecha), b1);
		}
		if (key.equals("frostdemon_metalcore")) {
			Identifier core1 = getCachedTexture(folder + "metalcore_layer1.png");
			Identifier core2 = getCachedTexture(folder + "metalcore_layer2.png");
			emitFadingLayer(consumer, "metalcore_1", DMZSkinLayer.getSafeTexture(core1, core1), ColorUtils.hexToRgb("#9BA377"));
			emitFadingLayer(consumer, "metalcore_2", DMZSkinLayer.getSafeTexture(core2, core2), ColorUtils.hexToRgb("#20211A"));
		}
	}

    protected void resolveBodyBioAndroid(Character character, String key, float[] b1, float[] b2, float[] b3, float[] hair, BiConsumer<Identifier, float[]> consumer) {
        String phase = switch (key) {
            case "bioandroid_semi" -> "semiperfect";
            case "bioandroid_perfect", "bioandroid_ultra", "bioandroid_xeno", "bioandroid_xenofp" -> "perfect";
            case "bioandroid_base" -> "base";
            case "bioandroid" -> character.hasActiveForm() ? "perfect" : "base";
            default -> "perfect";
        };

        int bodyType = character.getBodyType();

        String currentForm = character.getActiveForm() != null ? character.getActiveForm() : "";
        String formGroup = character.getActiveFormGroup() != null ? character.getActiveFormGroup() : "";

        String prefix = "textures/entity/races/bioandroid/" + phase + "_" + bodyType + "_";
        String fallbackPrefix = "textures/entity/races/bioandroid/" + phase + "_0_";

        acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer1.png"), getCachedTexture(fallbackPrefix + "layer1.png")), b1);
        acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer2.png"), getCachedTexture(fallbackPrefix + "layer2.png")), b2);
        acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer3.png"), getCachedTexture(fallbackPrefix + "layer3.png")), b3);
        acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer4.png"), getCachedTexture(fallbackPrefix + "layer4.png")), hair);

        if (!currentForm.equals("xenomax") && !currentForm.equals("xenofp")) {
            acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer5.png"), getCachedTexture(fallbackPrefix + "layer5.png")), DEFAULT_STINGER_COLOR);
        }

        emitBioAndroidFormLayers(key, formGroup, consumer);
    }

	private void emitBioAndroidFormLayers(String key, String formGroup, BiConsumer<Identifier, float[]> consumer) {
		boolean xenoModel = key.equals("bioandroid_xeno") || key.equals("bioandroid_xenofp");
		if (!formGroup.equals("legendaryforms") && !xenoModel) return;
		emitOverlayLayer(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture("textures/entity/races/bioandroid/xenoform_layer1.png")), WHITE_COLOR);
		if (xenoModel) emitOverlayLayer(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture("textures/entity/races/bioandroid/xenoform_layer2.png")), WHITE_COLOR);
	}

    protected void resolveBodyMajin(Character character, String key, float[] b1, float[] b2, float[] b3, BiConsumer<Identifier, float[]> consumer) {

        if ("janemba_super".equals(key)) {
            String path = "textures/entity/races/majin/janembasuper_0_male_";
            acceptWithShadow(consumer, getCachedTexture(path + "layer1.png"), b1);
            acceptWithShadow(consumer, getCachedTexture(path + "layer2.png"), b2);
            acceptWithShadow(consumer, getCachedTexture(path + "layer3.png"), b3);
            return;
        }

        if ("janemba_imperfect".equals(key)) {
            String path = "textures/entity/races/majin/janemba_0_male_";
            acceptWithShadow(consumer, getCachedTexture(path + "layer1.png"), b1);
            return;
        }

        if ("janemba_fat".equals(key)) {
            String path = "textures/entity/races/majin/janembafat_0_male_";
            acceptWithShadow(consumer, getCachedTexture(path + "layer1.png"), b1);
            acceptWithShadow(consumer, getCachedTexture(path + "layer2.png"), b2);
            return;
        }

        String currentForm = character.getActiveForm();
        String gender = character.getGender().toLowerCase().trim();
        String genderSuffix = gender.equals(Character.GENDER_FEMALE) ? "female" : "male";
        String phase;

        if (Objects.equals(currentForm, MajinForms.KID) || key.equals("majin_kid")) phase = "kid";
        else if (Objects.equals(currentForm, MajinForms.EVIL) || key.equals("majin_evil")) phase = "evil";
        else if (Objects.equals(currentForm, MajinForms.SUPER) || key.equals("majin_super")) phase = "super";
        else if (Objects.equals(currentForm, MajinForms.ULTRA) || key.equals("majin_ultra")) phase = "ultra";
        else if (character.hasActiveForm()) phase = "super";
        else phase = "base";

        int bodyType = character.getBodyType();
        String basePath = "textures/entity/races/majin/bodytype_" + genderSuffix + "_" + bodyType + "_";
        String fallbackPath = "textures/entity/races/majin/bodytype_" + genderSuffix + "_0_";

        Identifier l1 = DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer1.png"), getCachedTexture(fallbackPath + "layer1.png"));
        if (l1 != null) acceptWithShadow(consumer, l1, b1);

        emitShadowLayer(consumer, tryLoadOptionalLayer(basePath + "layer2.png", b2, consumer), b2);
        emitShadowLayer(consumer, tryLoadOptionalLayer(basePath + "layer3.png", b3, consumer), b3);

        if (genderSuffix.equals("female") && (phase.equals("super") || phase.equals("ultra"))) {
            Identifier tailLoc = getCachedTexture("textures/entity/races/tail1.png");
            consumer.accept(DMZSkinLayer.getSafeTexture(tailLoc, tailLoc), b1);
        }
    }

    private Identifier tryLoadOptionalLayer(String path, float[] color, BiConsumer<Identifier, float[]> consumer) {
        Identifier loc = getCachedTexture(path);
        if (Minecraft.getInstance().getResourceManager().getResource(loc).isPresent()) {
            consumer.accept(loc, color);
            return loc;
        }
        return null;
    }

    private Identifier tryLoadOptionalLayerWithFallback(String path, String fallbackPath, float[] color, BiConsumer<Identifier, float[]> consumer) {
        Identifier loc = getCachedTexture(path);
        Identifier fallbackLoc = getCachedTexture(fallbackPath);

        if (Minecraft.getInstance().getResourceManager().getResource(loc).isPresent()) {
            consumer.accept(loc, color);
            return loc;
        }
        else if (Minecraft.getInstance().getResourceManager().getResource(fallbackLoc).isPresent()) {
            consumer.accept(fallbackLoc, color);
            return fallbackLoc;
        }
        return null;
    }
}