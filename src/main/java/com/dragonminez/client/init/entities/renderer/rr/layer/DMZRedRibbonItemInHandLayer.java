package com.dragonminez.client.init.entities.renderer.rr.layer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.mojang.math.Axis;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.dragonminez.common.init.entities.redribbon.RedRibbonEntity;

import java.util.function.BiConsumer;

/**
 * Draws the item held in a Red Ribbon soldier's right hand on its {@code right_hand_item} bone.
 *
 * <h2>GeckoLib 5 migration</h2>
 * 1.20.1 overrode {@code renderForBone(...)} and called {@code ItemRenderer#renderStatic} inline. Both
 * are gone in 26.2:
 * <ul>
 *   <li>the bone callback is registered through
 *       {@link #addPerBoneRender(RenderPassInfo, BiConsumer)};</li>
 *   <li>an item is drawn by filling an {@link ItemStackRenderState} through the item model resolver and
 *       submitting it;</li>
 *   <li>{@code renderForBone} used to receive the live animatable, {@code addPerBoneRender} does not -
 *       so the held stack is captured in {@link #addRenderData} and read back from the render state.</li>
 * </ul>
 */
public class DMZRedRibbonItemInHandLayer<T extends RedRibbonEntity, R extends GeoRenderState>
        extends GeoRenderLayer<T, Void, R> {

    private static final DataTicket<ItemStack> DMZ_HELD_ITEM =
            DataTicket.create("dmz_rr_held_item", ItemStack.class);

    public DMZRedRibbonItemInHandLayer(GeoRenderer<T, Void, R> entityRendererIn) {
        super(entityRendererIn);
    }

    /** Runs with the live entity, so this is where the held stack is captured. */
    @Override
    public void addRenderData(T animatable, Void unused, R renderState, float partialTick) {
        super.addRenderData(animatable, unused, renderState, partialTick);
        renderState.addGeckolibData(DMZ_HELD_ITEM, animatable.getItemBySlot(EquipmentSlot.MAINHAND));
    }

    @Override
    public void addPerBoneRender(RenderPassInfo<R> info, BiConsumer<GeoBone, PerBoneRender<R>> registrar) {
        if (info == null || !info.willRender()) return;
        GeoBone bone = info.model().getBone("right_hand_item").orElse(null);
        if (bone == null) return;

        registrar.accept(bone, (passInfo, geoBone, collector) -> submitHeldItem(passInfo, collector));
    }

    private void submitHeldItem(RenderPassInfo<R> info, SubmitNodeCollector collector) {
        ItemStack held = info.renderState().getGeckolibData(DMZ_HELD_ITEM);
        if (held == null || held.isEmpty()) return;

        var poseStack = info.poseStack();
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-90f));
        poseStack.translate(0.4D, 0.1D, 0.73D);

        ItemStackRenderState itemState = new ItemStackRenderState();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.getItemModelResolver().updateForLiving(itemState, held,
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, mc.player);
        }
        itemState.submit(poseStack, collector, info.packedLight(), info.packedOverlay(), 0);

        poseStack.popPose();
    }
}
