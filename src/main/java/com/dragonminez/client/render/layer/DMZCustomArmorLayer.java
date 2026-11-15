package com.dragonminez.client.render.layer;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

import com.dragonminez.Reference;
import com.dragonminez.client.model.DMZPlayerModel;
import com.dragonminez.client.render.compat.CosmeticArmorCompat;
import com.dragonminez.client.util.ArmorTextureResolver;
import com.dragonminez.client.util.SkinGathererProvider;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.armor.DbzArmorTextured;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.neoforge.common.CommonHooks;
import net.minecraft.core.registries.BuiltInRegistries;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Set;

public class DMZCustomArmorLayer<T extends AbstractClientPlayer & GeoAnimatable> extends DMZPlayerGeoLayer<T> {

    private static final Identifier MAJIN_ARMOR_MODEL = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "armor/armormajinfat");
    private static final Identifier MAJIN_SLIM_ARMOR_MODEL = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "armor/armormajinslim");
    private static final Identifier OOZARU_ARMOR_MODEL = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "armor/armoroozaru");

    private static final Set<String> SLIM_SUPPORTED_MODELS = Set.of(
            "majin_evil", "majin_kid", "majin_super", "majin_ultra", "majin", "saiyan", "human", "ssj4gt", "ssj4d"
    );

    public DMZCustomArmorLayer(GeoRenderer<T, ?, ?> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    protected boolean skipInHudPortrait() {
        return false;
    }

    @Override
    protected void render(DMZLayerContext ctx) {
        PoseStack poseStack = ctx.poseStack();
        @SuppressWarnings("unchecked") T animatable = (T) ctx.player();
        var collector = ctx.collector();
        com.geckolib.renderer.base.RenderPassInfo<com.geckolib.renderer.base.GeoRenderState> info = ctx.info();
        float partialTick = ctx.partialTick();
        int packedLight = ctx.packedLight();
        int packedOverlay = ctx.packedOverlay();
        GeoBone playerBone = ctx.model().getBone("body").orElse(null);

        if (animatable == null || playerBone == null) return;
        if (animatable.isSpectator()) return;

        ItemStack stack = resolveChestArmorStack(animatable);
        if (stack.isEmpty() || !(stack.has(DataComponents.EQUIPPABLE))) return;

        StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, animatable).orElse(new StatsData(animatable));
        if (stats.getCharacter().getArmored()) return;

        ArmorRenderContext armorCtx = resolveArmorContext(stats, stack);
        if (!armorCtx.shouldRender()) return;

        if (armorCtx.isDbzArmor()) {
            Identifier texture = getDbzArmorTexture((DbzArmorTextured) stack.getItem(), stack);
            float translateY = resolveCustomArmorTranslateY(armorCtx);
            float inflation = armorCtx.isOozaruTarget() ? 1.021f : 1.035f;

            poseStack.pushPose();
            poseStack.translate(0, translateY, 0);

            // GeckoLib 5: hide the player's own body shells and widen the chest bone through a
            // one-shot BoneUpdater; GeoBone itself no longer carries mutable state.
            info.addBoneUpdater((passInfo, snapshots) -> {
                snapshots.ifPresent("armorBody", snap -> snap.skipRender(true));
                snapshots.ifPresent("armorLeggingsBody", snap -> snap.skipRender(true));
                snapshots.ifPresent("body_layer", snap -> snap.skipRender(true));
                snapshots.ifPresent("boobas", snap -> snap.setScale(
                        snap.getScaleX() * 1.03f,
                        snap.getScaleY() * 1.06f,
                        snap.getScaleZ() * 1.15f));
            });

            renderRootBoneInflated(playerBone, poseStack, collector, info, animatable, texture, partialTick, packedLight, inflation);

            poseStack.popPose();
            return;
        }

        Identifier targetModelLoc = armorCtx.isOozaruTarget() ? OOZARU_ARMOR_MODEL : (armorCtx.isSlimTarget() ? MAJIN_SLIM_ARMOR_MODEL : MAJIN_ARMOR_MODEL);
        BakedGeoModel vanillaArmorModel = getGeoModel().getBakedModel(targetModelLoc);
        if (vanillaArmorModel == null) return;

        GeoBone armorBodyBone = vanillaArmorModel.getBone("body").orElse(null);
        if (armorBodyBone == null && vanillaArmorModel.topLevelBones().length > 0) {
            armorBodyBone = vanillaArmorModel.topLevelBones()[0];
        }
        if (armorBodyBone == null) return;

        Identifier texture = getVanillaArmorTexture(animatable, stack, EquipmentSlot.CHEST, null);

        // GeckoLib 5: chest-bone scaling is registered as a BoneUpdater on the pass's snapshots.
        float boobFactor = resolveBoobScale(stats);
        if (boobFactor != 1.0f) {
            float[] axis = DMZPlayerModel.computeBoobAxisScale(boobFactor);
            info.addBoneUpdater((passInfo, snapshots) ->
                    snapshots.ifPresent("boobas", snap -> snap.setScale(
                            snap.getScaleX() * axis[0] * 1.03f,
                            snap.getScaleY() * axis[1] * 1.06f,
                            snap.getScaleZ() * axis[2] * 1.15f)));
        }

        float translateY = resolveCustomArmorTranslateY(armorCtx);
        poseStack.pushPose();
        poseStack.translate(0, translateY, 0);
        renderRootBoneInflated(armorBodyBone, poseStack, collector, info, animatable, texture, partialTick, packedLight, 1.05f);
        poseStack.popPose();

        if (stack.has(DataComponents.DYED_COLOR) || DyedItemColor.getOrDefault(stack, -1) != -1) {
            Identifier overlayTex = getVanillaArmorTexture(animatable, stack, EquipmentSlot.CHEST, "overlay");

            poseStack.pushPose();
            poseStack.translate(0, translateY, 0);
            renderRootBoneInflated(armorBodyBone, poseStack, collector, info, animatable, overlayTex, partialTick, packedLight, 1.05f);
            poseStack.popPose();
        }

        // submitted via collector
    }

    private float resolveBoobScale(StatsData stats) {
        Character c = stats.getCharacter();
        String gender = c.getGender() != null ? c.getGender().toLowerCase() : "";
        boolean isFemale = gender.equals("female") || c.getBodyType() == 1;
        return isFemale ? Math.max(0.75f, Math.min(1.25f, c.getBoobScale())) : 1.0f;
    }

    private ItemStack resolveChestArmorStack(T animatable) {
        ItemStack stack = animatable.getItemBySlot(EquipmentSlot.CHEST);
        if (CosmeticArmorCompat.isLoaded()) {
            ItemStack cosmeticStack = CosmeticArmorCompat.getCosmeticStack(animatable, EquipmentSlot.CHEST);
            if (cosmeticStack != null) {
                if (cosmeticStack.isEmpty()) return ItemStack.EMPTY;
                stack = cosmeticStack;
            }
        }
        return stack;
    }

    private ArmorRenderContext resolveArmorContext(StatsData stats, ItemStack stack) {
        var character = stats.getCharacter();
        String raceName = character.getRaceName().toLowerCase();
        String gender = character.getGender().toLowerCase();
        int bodyType = character.getBodyType(); // Asegúrate de obtener el bodyType

        String logicKey = character.getRenderLogicKey();

        Identifier itemKey = BuiltInRegistries.ITEM.getKey(stack.getItem());
        boolean isVanilla = itemKey != null && "minecraft".equals(itemKey.getNamespace());
        boolean isDbzArmor = stack.getItem() instanceof DbzArmorTextured;
        boolean isPothala = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().contains("pothala");
        if (isPothala || (!isVanilla && !isDbzArmor)) {
            return new ArmorRenderContext(false, false, false, false, isDbzArmor);
        }

        boolean shouldRender = false;
        boolean isSlimTarget = false;
        boolean isOozaruTarget = false;
        boolean isMajinGordoTarget = false;

        if (character.isOozaruCached() || logicKey.equals("oozaru")) {
            shouldRender = true;
            isOozaruTarget = true;
        }
        else if (SLIM_SUPPORTED_MODELS.contains(logicKey) && gender.equals(Character.GENDER_FEMALE)) {
            shouldRender = true;
            isSlimTarget = true;
        }
        else if (logicKey.contains("buffed") || logicKey.contains("frostdemon_fp") || logicKey.contains("majin_ultra")
                || logicKey.contains("namekian_orange") || logicKey.contains("bioandroid_ultra") || logicKey.contains("ssj4gt") || logicKey.contains("ssj4d")
                || logicKey.contains("frostdemon_fifth") || logicKey.contains("frostdemon_metalcore") || logicKey.contains("namekian_buffed")
                || logicKey.contains("4arms") || logicKey.contains("bioandroid_xeno") || logicKey.equals("janemba_super")) {
            if (isDbzArmor) shouldRender = true;
        }
        else if (logicKey.equals("majin") && gender.equals(Character.GENDER_MALE) && bodyType != 2) {
            shouldRender = true;
            isMajinGordoTarget = true;
        }
        else if (logicKey.equals("janemba_fat")) {
            shouldRender = true;
            isMajinGordoTarget = true;
        }
        else if (SkinGathererProvider.modelFamily(logicKey).equals("custom")) {
            if (isDbzArmor) shouldRender = true;
        }

        return new ArmorRenderContext(shouldRender, isSlimTarget, isOozaruTarget, isMajinGordoTarget, isDbzArmor);
    }

    private float resolveCustomArmorTranslateY(ArmorRenderContext ctx) {
        if (ctx.isOozaruTarget() || ctx.isMajinGordoTarget()) return 0.03f;
        return 0.001f;
    }

    private void renderBaseArmorBodyFromPlayerBone(GeoBone playerBodyBone, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, com.geckolib.renderer.base.RenderPassInfo<com.geckolib.renderer.base.GeoRenderState> info, T animatable, Identifier texture, float partialTick, int packedLight) {
        renderBaseArmorPiece(playerBodyBone, "armorBody", poseStack, collector, info, animatable, texture, partialTick, packedLight);
        renderBaseArmorPiece(playerBodyBone, "armorBody2", poseStack, collector, info, animatable, texture, partialTick, packedLight);
        renderBaseArmorPiece(playerBodyBone, "armorLeggingsBody", poseStack, collector, info, animatable, texture, partialTick, packedLight);
    }

    private void renderBaseArmorPiece(GeoBone playerBodyBone, String boneName, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, com.geckolib.renderer.base.RenderPassInfo<com.geckolib.renderer.base.GeoRenderState> info, T animatable, Identifier texture, float partialTick, int packedLight) {
        GeoBone armorPiece = getChild(playerBodyBone, boneName);
        if (armorPiece == null) return;

        // GeckoLib 5: BoneSnapshots are rebuilt per render pass, so there is nothing to restore.
        // Registering a BoneUpdater that un-skips the armor bone (and its parent chain) for this
        // pass is enough — the player's own shells stay hidden via the root updater.
        info.addBoneUpdater((passInfo, snapshots) -> {
            snapshots.ifPresent(armorPiece.name(), snap -> snap.skipRender(false));
            GeoBone parent = armorPiece.parent();
            while (parent != null) {
                snapshots.ifPresent(parent.name(), snap -> snap.skipRender(false));
                parent = parent.parent();
            }
        });

        renderChildBoneInflated(armorPiece, poseStack, collector, info, animatable, texture, partialTick, packedLight, 1.1f);
    }


    private GeoBone getChild(GeoBone parent, String name) {
        for (GeoBone child : parent.children()) {
            if (child.name().equals(name)) return child;
        }
        return null;
    }

    private static final Set<String> EXCLUDED_BONES = Set.of("tail1", "tail1m", "arm", "arm2", "arm3");

    private void renderRootBoneInflated(GeoBone targetBone, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, com.geckolib.renderer.base.RenderPassInfo<com.geckolib.renderer.base.GeoRenderState> info, T animatable, Identifier texture, float partialTick, int packedLight, float inflation) {
        // GeckoLib 5: GeoBone is immutable model data. Runtime bone transforms are applied by
        // registering a one-shot BoneUpdater that mutates the BoneSnapshot for this render pass.
        info.addBoneUpdater((passInfo, snapshots) -> {
            snapshots.ifPresent(targetBone.name(), snap -> {
                snap.setRotation(0, 0, 0);
                snap.setTranslation(0, 0, 0);
                snap.setScale(inflation, inflation, inflation);
            });
            for (String name : EXCLUDED_BONES) {
                snapshots.ifPresent(name, snap -> snap.skipRender(true));
            }
        });

        RenderType armorRenderType = RenderTypes.armorCutoutNoCull(texture);
        getRenderer().submitRenderTasks(info, collector, armorRenderType);
    }

    private GeoBone findBoneDeep(GeoBone parent, String name) {
        for (GeoBone child : parent.children()) {
            if (child.name().equals(name)) return child;
            GeoBone found = findBoneDeep(child, name);
            if (found != null) return found;
        }
        return null;
    }

    private void renderChildBoneInflated(GeoBone targetBone, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, com.geckolib.renderer.base.RenderPassInfo<com.geckolib.renderer.base.GeoRenderState> info, T animatable, Identifier texture, float partialTick, int packedLight, float inflation) {
        // GeckoLib 5: scale is applied through a one-shot BoneUpdater rather than mutating GeoBone.
        info.addBoneUpdater((passInfo, snapshots) ->
                snapshots.ifPresent(targetBone.name(), snap ->
                        snap.setScale(snap.getScaleX() * inflation,
                                      snap.getScaleY() * inflation,
                                      snap.getScaleZ() * inflation)));

        RenderType armorRenderType = RenderTypes.armorCutoutNoCull(texture);
        getRenderer().submitRenderTasks(info, collector, armorRenderType);
    }

    private Identifier getDbzArmorTexture(DbzArmorTextured item, ItemStack stack) {
        String modId = Reference.MOD_ID;
        if (stack.getItem() instanceof com.dragonminez.common.init.armor.DbzArmorItem dbzItem) {
            modId = dbzItem.getModId();
        }
        return ArmorTextureResolver.resolve(modId, item.getItemId(), EquipmentSlot.CHEST, stack);
    }

    private Identifier getVanillaArmorTexture(LivingEntity entity, ItemStack stack, EquipmentSlot slot, String type) {
        // 26.2: ArmorItem (and ArmorItem#getMaterial) is gone. The armor material now lives on the
        // EQUIPPABLE component as an EquipmentAsset reference; resolve the texture directory from
        // the item's own registry name instead.
        Identifier itemRegistryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String domain = itemRegistryName != null ? itemRegistryName.getNamespace() : "minecraft";
        String materialName = itemRegistryName != null ? itemRegistryName.getPath() : "unknown";
        String typeSuffix = (type == null || type.isEmpty()) ? "" : "_" + type;
        String textureLocation = String.format("%s:textures/models/armor/%s_layer_1%s.png", domain, materialName, typeSuffix);
        return Identifier.parse(textureLocation);
    }

    private record ArmorRenderContext(boolean shouldRender, boolean isSlimTarget, boolean isOozaruTarget,
                                      boolean isMajinGordoTarget, boolean isDbzArmor) {
    }

    private record VisibilityState(java.util.List<GeoBone> parents, java.util.List<Boolean> hiddenStates) {
    }
}

