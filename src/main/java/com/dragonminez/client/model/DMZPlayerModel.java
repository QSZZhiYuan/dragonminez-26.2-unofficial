package com.dragonminez.client.model;

import net.minecraft.util.Mth;
import com.dragonminez.Reference;
import com.dragonminez.client.animation.IPlayerAnimatable;
import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.client.render.util.RenderUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.util.lists.SaiyanForms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.loading.math.MolangQueries;
import com.geckolib.model.GeoModel;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DMZPlayerModel<T extends AbstractClientPlayer & GeoAnimatable> extends GeoModel<T> {

    private static final Identifier BASE_DEFAULT = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/human");
    private static final Identifier BASE_SLIM = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/human_slim");
    private static final Identifier MAJIN_FAT = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/majin");
    private static final Identifier MAJIN_SLIM = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/majin_slim");
    private static final Identifier JANEMBA_SUPER = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/janemba_super");
    private static final Identifier JANEMBA_FAT = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/janemba_fat");

    private static final Identifier FROST_DEMON = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/frostdemon");
    private static final Identifier FROST_DEMON_SECOND = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/frostdemon_second");
    private static final Identifier FROST_DEMON_THIRD = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/frostdemon_third");
    private static final Identifier FROST_DEMON_FIFTH = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/frostdemon_fifth");
    private static final Identifier FROSTDEMON_BUFFED = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/frostdemon_fp");
    private static final Identifier FROSTDEMON_METALCORE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/frostdemon_metalcore");

    private static final Identifier BIO_ANDROID = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/bioandroid");
    private static final Identifier BIO_ANDROID_SEMI = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/bioandroid_semi");
    private static final Identifier BIO_ANDROID_PERFECT = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/bioandroid_perfect");
    private static final Identifier BIO_ANDROID_ULTRA = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/bioandroid_ultra");
    private static final Identifier BIO_ANDROID_XENO = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/bioandroid_xeno");

    private static final Identifier OOZARU = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/oozaru");
    private static final Identifier HUMAN_SAIYAN_BUFFED = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/hbuffed");
    private static final Identifier HUMAN_SAIYAN_SLIM_BUFFED = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/hbuffed_slim");
    private static final Identifier HUMAN_SAIYAN_FEMALE_BUFFED = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/hbuffed_fem");

    private static final Identifier HUMAN_SAIYAN_4ARMS = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/h4arms");
    private static final Identifier HUMAN_SAIYAN_4ARMS_SLIM = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/h4armsslim");
    private static final Identifier HUMAN_SAIYAN_4ARMS_FEM = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/h4armsfem");

    private static final Identifier JANEMBA_IMPERFECT = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/janemba_imperfect");
    private static final Identifier BIO_ANDROID_XENO_FP = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/bioandroid_xenofp");
    private static final Identifier HUMAN_SAIYAN_BUFFED_G3 = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/hbuffedg3");
    private static final Identifier HUMAN_SAIYAN_SLIM_BUFFED_G3 = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/hbuffed_slimg3");
    private static final Identifier HUMAN_SAIYAN_FEMALE_BUFFED_G3 = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/hbuffed_femg3");
    private static final Identifier GLIND_TRUE_FORM = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/glindtrueform");
    private static final Identifier GLIND_TRANSCENDED = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/trascended");
    private static final Identifier GLIND_TRANSCENDED_FEMALE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/trascended_fem");
    private static final Identifier GLIND_DARK = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/glinddark");

    private static final Identifier CANDY_MODEL = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/candy");
    private static final Identifier CANDY_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/candy.png");

    private static final Map<Identifier, Boolean> FILE_EXISTS_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Identifier> MODEL_RESOLUTION_CACHE = new ConcurrentHashMap<>();

    private static final Identifier ANIM_PRIMARY = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/movement");
    private static final Identifier[] ANIM_FALLBACKS = {
        Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/combat"),
        Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/ki"),
        Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/transf"),
        Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/skp"),
        Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/evs")
    };

    private final Identifier textureLocation;
    private final String customModel;

    /**
     * GeckoLib 5: {@code getModelResource} / {@code getTextureResource} only receive a
     * {@link com.geckolib.renderer.base.GeoRenderState}, so every player-derived value they need is captured
     * while the live player is reachable ({@link #addAdditionalStateData}) and read back here.
     * {@code null} means "no capture happened" and falls through to the default model.
     */
    public static final com.geckolib.constant.dataticket.DataTicket<AbstractClientPlayer> DMZ_PLAYER =
            com.geckolib.constant.dataticket.DataTicket.create("dmz_player_model_player", AbstractClientPlayer.class);
    private static final com.geckolib.constant.dataticket.DataTicket<Boolean> DMZ_CANDY =
            com.geckolib.constant.dataticket.DataTicket.create("dmz_player_model_candy", Boolean.class);

    public DMZPlayerModel(String raceName, String customModel) {
        this.customModel = customModel;
        this.textureLocation = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png");
    }

    @Override
    public void addAdditionalStateData(T animatable, Object unused, com.geckolib.renderer.base.GeoRenderState renderState) {
        renderState.addGeckolibData(DMZ_PLAYER, (AbstractClientPlayer) animatable);
        renderState.addGeckolibData(DMZ_CANDY, com.dragonminez.client.render.effects.CandyEffect.isCandy(animatable));
    }

    @Override
    public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        AbstractClientPlayer player = renderState.getGeckolibData(DMZ_PLAYER);
        if (player == null) {
            return BASE_DEFAULT;
        }
        if (com.dragonminez.client.render.effects.CandyEffect.isCandy(player)) {
            return CANDY_MODEL;
        }

        return StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> {
            Character character = data.getCharacter();
            String race = character.getRaceName().toLowerCase();
            String gender = character.getGender().toLowerCase();
            String currentForm = character.getActiveForm();
            int bodyType = character.getBodyType();
            String playerModelName = player.getSkin().model().getSerializedName();

            RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(race);
            var activeStackFormData = character.getActiveStackFormData();
            var activeFormData = character.getActiveFormData();
            String activeCustomModel;
            if (activeStackFormData != null && Boolean.TRUE.equals(activeStackFormData.hasCustomModel()) && !activeStackFormData.getCustomModel().isEmpty()) {
                activeCustomModel = activeStackFormData.getCustomModel().toLowerCase();
            } else if (activeFormData != null && activeFormData.hasCustomModel() && !activeFormData.getCustomModel().isEmpty()) {
                activeCustomModel = activeFormData.getCustomModel().toLowerCase();
            } else {
                activeCustomModel = "";
            }
            String raceCustomModel = (raceConfig != null && raceConfig.hasCustomModel()) ? raceConfig.getCustomModel().toLowerCase() : "";
            String fallbackCustomModel = this.customModel != null ? this.customModel.toLowerCase() : "";
            String formKey = currentForm != null ? currentForm.toLowerCase() : "";
            boolean slimBody = bodyType == 0 ? playerModelName.contains("slim") : raceConfig != null && raceConfig.isSlimBodyType(bodyType);
            String stateKey = String.join("|",
                    race,
                    gender,
                    formKey,
                    Integer.toString(bodyType),
                    playerModelName,
                    activeCustomModel,
                    raceCustomModel,
                    fallbackCustomModel,
                    Boolean.toString(raceConfig != null && raceConfig.getHasGender()),
                    Boolean.toString(slimBody)
            );

            return MODEL_RESOLUTION_CACHE.computeIfAbsent(stateKey, ignored -> {
                boolean isMale = gender.equals(Character.GENDER_MALE);
                boolean isSlimSkin = playerModelName.contains("slim");
                boolean isBaseForm = currentForm == null || currentForm.isEmpty() || currentForm.equalsIgnoreCase("base");

                if (race.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU))) {
                    return OOZARU;
                }

                String modelKey = "";
                if (!activeCustomModel.isEmpty()) {
                    modelKey = activeCustomModel;
                } else if (!raceCustomModel.isEmpty()) {
                    modelKey = raceCustomModel;
                } else if (!fallbackCustomModel.isEmpty()) {
                    modelKey = fallbackCustomModel;
                }

                if (!modelKey.isEmpty()) {
                    String customRaceGender = (raceConfig != null && raceConfig.getHasGender()) ? gender : "";
                    if (modelKey.equals("finalbase")) {
                        switch (race) {
                            case "human", "saiyan":
                                if (!isMale) return MAJIN_SLIM;
                                return slimBody ? BASE_SLIM : BASE_DEFAULT;
                            case "majin": return isMale ? BASE_DEFAULT : MAJIN_SLIM;
                            case "namekian": return BASE_DEFAULT;
                            case "frostdemon": return FROST_DEMON;
                            case "bioandroid": return BIO_ANDROID_PERFECT;
                        }
                    }
                    return resolveCustomModel(modelKey, isSlimSkin, slimBody, isMale, bodyType, customRaceGender);
                }

                if (race.equals("bioandroid")) return isBaseForm ? BIO_ANDROID : BIO_ANDROID_PERFECT;
                if (race.equals("frostdemon")) return FROST_DEMON;
                if (race.equals("namekian")) return BASE_DEFAULT;

                if (race.equals("majin")) {
                    if (bodyType == 2) return isMale ? BASE_DEFAULT : MAJIN_SLIM;
                    if (!isMale) return MAJIN_SLIM;
                    return MAJIN_FAT;
                }

                if (race.equals("human") || race.equals("saiyan")) {
                    if (!isMale) return MAJIN_SLIM;
                    return slimBody ? BASE_SLIM : BASE_DEFAULT;
                }

                if (!isMale) return MAJIN_SLIM;
                return isSlimSkin ? BASE_SLIM : BASE_DEFAULT;
            });
        }).orElse(BASE_DEFAULT);
    }

    private static Identifier humanoidVariant(boolean slimBody, boolean isMale, int bodyType, Identifier wide, Identifier slim, Identifier female) {
        if (bodyType != 0 && !isMale) return female;
        return slimBody ? slim : wide;
    }

    private Identifier resolveCustomModel(String modelName, boolean isSlimSkin, boolean slimBody, boolean isMale, int bodyType, String customRaceGender) {
        String key = modelName.toLowerCase();

        switch (key) {
            // HUMAN & SAIYAN
            case "human":
            case "saiyan":
                if (!isMale) return MAJIN_SLIM;
                return slimBody ? BASE_SLIM : BASE_DEFAULT;
            case "oozaru": return OOZARU;
            case "ssj4gt":
            case "buffed":
                return humanoidVariant(slimBody, isMale, bodyType, HUMAN_SAIYAN_BUFFED, HUMAN_SAIYAN_SLIM_BUFFED, HUMAN_SAIYAN_FEMALE_BUFFED);
            case "ssj4d":
            case "buffedg3":
                return humanoidVariant(slimBody, isMale, bodyType, HUMAN_SAIYAN_BUFFED_G3, HUMAN_SAIYAN_SLIM_BUFFED_G3, HUMAN_SAIYAN_FEMALE_BUFFED_G3);
            case "4arms":
                return humanoidVariant(slimBody, isMale, bodyType, HUMAN_SAIYAN_4ARMS, HUMAN_SAIYAN_4ARMS_SLIM, HUMAN_SAIYAN_4ARMS_FEM);
            case "glindtrueform": return GLIND_TRUE_FORM;
            case "trascended": return isMale ? GLIND_TRANSCENDED : GLIND_TRANSCENDED_FEMALE;
            case "glinddark": return GLIND_DARK;

            // NAMEKIAN
            case "namekian": return BASE_DEFAULT;
            case "namekian_orange": case "namekian_buffed": return HUMAN_SAIYAN_BUFFED;

            // MAJIN
            case "majin":
                if (bodyType == 2) return isMale ? BASE_DEFAULT : MAJIN_SLIM;
                if(!isMale) return MAJIN_SLIM;
                return MAJIN_FAT;
            case "majin_super": return isMale ? BASE_DEFAULT : MAJIN_SLIM;
            case "majin_ultra": return isMale ? HUMAN_SAIYAN_BUFFED : HUMAN_SAIYAN_FEMALE_BUFFED;
            case "majin_evil": case "majin_kid": return isMale ? BASE_SLIM : MAJIN_SLIM;
            case "janemba_imperfect": return JANEMBA_IMPERFECT;
            case "janemba_fat": return JANEMBA_FAT;
            case "janemba_super": return JANEMBA_SUPER;

            // FROSTDEMON
            case "frostdemon": case "frostdemon_final": case "frostdemon_mecha": return FROST_DEMON;
            case "frostdemon_second": return FROST_DEMON_SECOND;
            case "frostdemon_fifth": return FROST_DEMON_FIFTH;
            case "frostdemon_fp": return FROSTDEMON_BUFFED;
            case "frostdemon_third": return FROST_DEMON_THIRD;
            case "frostdemon_metalcore": return FROSTDEMON_METALCORE;

            // BIOANDROID
            case "bioandroid": return BIO_ANDROID;
            case "bioandroid_base": return BIO_ANDROID;
            case "bioandroid_semi": return BIO_ANDROID_SEMI;
            case "bioandroid_perfect": return BIO_ANDROID_PERFECT;
            case "bioandroid_ultra": return BIO_ANDROID_ULTRA;
            case "bioandroid_xeno": return BIO_ANDROID_XENO;
            case "bioandroid_xenofp": return BIO_ANDROID_XENO_FP;

        }

        String suffix = (customRaceGender != null && !customRaceGender.isEmpty()) ? "_" + customRaceGender : "";
        Identifier customLoc = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/" + modelName + suffix);

        if (fileExists(customLoc)) return customLoc;
        return isSlimSkin ? BASE_SLIM : BASE_DEFAULT;
    }

    @Override
    public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        if (Boolean.TRUE.equals(renderState.getGeckolibData(DMZ_CANDY))) {
            return CANDY_TEXTURE;
        }
        return textureLocation;
    }

    @Override
    public Identifier getAnimationResource(T t) {
        return ANIM_PRIMARY;
    }

    @Override
    public Identifier[] getAnimationResourceFallbacks(T t) {
        return ANIM_FALLBACKS;
    }

    /**
     * GeckoLib 5 removed {@code GeoModel#setCustomAnimations} and there is no per-frame model hook
     * that still sees the animatable, so the body-shape scale is applied from
     * {@code DMZPlayerRenderer#adjustModelBonesForRender} via {@code DmzHeadLook.BOOB_SCALE}.
     * Kept here only as the shared maths helper ({@code DMZCustomArmorLayer} uses it too).
     */
    public static float[] computeBoobAxisScale(float factor) {
        float delta = factor - 1.0f;
        return new float[]{
                1.0f + delta * 0.15f,
                1.0f + delta * 0.3f,
                1.0f + delta * 1.4f
        };
    }

    private boolean fileExists(Identifier location) {
        return FILE_EXISTS_CACHE.computeIfAbsent(location, loc ->
                Minecraft.getInstance().getResourceManager().getResource(Identifier.fromNamespaceAndPath(loc.getNamespace(), "geo/" + loc.getPath() + ".geo.json")).isPresent()
        );
    }

    /*
     * GeoModel#applyMolangQueries(AnimationTest, double) does not exist in GeckoLib 5, so the 1.20.1
     * override that pushed query.head_x_rotation / query.head_y_rotation was removed.
     *
     * 26.2 computes those queries natively (MolangQueries.HEAD_X_ROTATION / HEAD_Y_ROTATION are
     * built-in), which leaves only the "freeze the head while a transformation or ki animation is
     * playing" rule. That rule now lives in DmzHeadLook#captureFrom, applied through
     * DMZPlayerRenderer#adjustModelBonesForRender.
     */
}
