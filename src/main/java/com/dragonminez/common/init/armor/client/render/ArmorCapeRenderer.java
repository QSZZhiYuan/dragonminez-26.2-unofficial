package com.dragonminez.common.init.armor.client.render;

import com.dragonminez.Reference;
import com.dragonminez.client.util.ArmorTextureResolver;
import com.dragonminez.common.init.armor.DbzArmorCapeItem;
import com.dragonminez.common.init.armor.client.model.ArmorCapeModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import com.geckolib.renderer.GeoArmorRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/**
 * Cape / accessory armor renderer.
 *
 * <h2>GeckoLib 5 migration</h2>
 * Two GeckoLib-4 hooks were removed:
 * <ul>
 *   <li>{@code renderCubesOfBone(PoseStack, GeoBone, VertexConsumer, ...)} — GeckoLib 5 submits bones
 *       through the render pass, and visibility is expressed on {@code BoneSnapshots} instead of by
 *       filtering draws. The "only draw bones named cape*" filter is now applied in
 *       {@code adjustModelBonesForRender};</li>
 *   <li>{@code getCurrentSlot()} / {@code getCurrentStack()} — the current armor piece travels in
 *       {@link GeoArmorRenderer.RenderData}, which is exposed as context on the render state rather
 *       than as mutable renderer fields. {@code getTextureLocation} only receives the render state,
 *       so the slot/stack are captured during {@code addRenderData}.</li>
 * </ul>
 */
public class ArmorCapeRenderer extends GeoArmorRenderer<DbzArmorCapeItem, com.dragonminez.client.render.GeoHumanoidRenderState> {

    /**
     * Slot + stack captured in {@code addRenderData}, where the live animatable (the armor item and
     * the render data) are still reachable. {@code getTextureLocation} runs later with only the state.
     */
    private static final com.geckolib.constant.dataticket.DataTicket<EquipmentSlot> DMZ_ARMOR_SLOT =
            com.geckolib.constant.dataticket.DataTicket.create("dmz_cape_armor_slot", EquipmentSlot.class);
    private static final com.geckolib.constant.dataticket.DataTicket<ItemStack> DMZ_ARMOR_STACK =
            com.geckolib.constant.dataticket.DataTicket.create("dmz_cape_armor_stack", ItemStack.class);

    public ArmorCapeRenderer() {
        super(new ArmorCapeModel());
    }

    @Override
    public void addRenderData(DbzArmorCapeItem animatable, GeoArmorRenderer.RenderData renderData,
                              com.dragonminez.client.render.GeoHumanoidRenderState renderState, float partialTick) {
        super.addRenderData(animatable, renderData, renderState, partialTick);
        if (renderData == null) return;
        renderState.addGeckolibData(DMZ_ARMOR_SLOT, renderData.slot());
        renderState.addGeckolibData(DMZ_ARMOR_STACK, renderData.itemStack());
    }

    /**
     * Only the {@code cape*} bones are drawn for this armor piece; everything else is skipped for
     * this frame. GeckoLib 4 did this with an early return in {@code renderCubesOfBone}.
     */
    @Override
    public void adjustModelBonesForRender(
            com.geckolib.renderer.base.RenderPassInfo<com.dragonminez.client.render.GeoHumanoidRenderState> info,
            com.geckolib.renderer.base.BoneSnapshots bones) {
        super.adjustModelBonesForRender(info, bones);
        if (bones == null) return;

        var model = info.model();
        if (model == null) return;
        for (var topLevel : model.topLevelBones()) {
            skipNonCapeBones(topLevel, bones);
        }
    }

    private static void skipNonCapeBones(com.geckolib.cache.model.GeoBone bone,
                                         com.geckolib.renderer.base.BoneSnapshots bones) {
        String name = bone.name();
        boolean isCape = name != null && name.startsWith("cape");
        if (!isCape) {
            bones.ifPresent(name, snapshot -> snapshot.skipRender(true));
        }
        for (var child : bone.children()) {
            skipNonCapeBones(child, bones);
        }
    }

    @Override
    public Identifier getTextureLocation(com.dragonminez.client.render.GeoHumanoidRenderState renderState) {
        EquipmentSlot slot = renderState.getGeckolibData(DMZ_ARMOR_SLOT);
        ItemStack stack = renderState.getGeckolibData(DMZ_ARMOR_STACK);
        if (stack == null || stack.isEmpty()) {
            return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/armor/blank.png");
        }
        DbzArmorCapeItem animatable = (DbzArmorCapeItem) stack.getItem();
        String modId = animatable.getModId();
        String itemId = animatable.getItemId();

        if (itemId.contains("pothala") || itemId.contains("scouter"))
            return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/armor/blank.png");

        return ArmorTextureResolver.resolve(modId, itemId, slot, stack);
    }

    @Override
    public RenderType getRenderType(com.dragonminez.client.render.GeoHumanoidRenderState renderState, Identifier texture) {
        return RenderTypes.armorCutoutNoCull(texture);
    }
}
