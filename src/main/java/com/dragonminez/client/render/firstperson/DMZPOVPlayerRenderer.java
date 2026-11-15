package com.dragonminez.client.render.firstperson;

import com.dragonminez.client.render.DMZPlayerRenderer;
import com.dragonminez.client.render.EntityPreviewRenderContext;
import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.joml.Vector3f;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;

/**
 * Renders the body at the player's world position with the head hidden. The listener already
 * subtracts the actual camera position; applying the old camera offset again puts the arms
 * behind the camera.
 */
public class DMZPOVPlayerRenderer<T extends AbstractClientPlayer & GeoAnimatable> extends DMZPlayerRenderer<T> {

    public DMZPOVPlayerRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
        super(renderManager, model);
    }

    @Override
    public void addRenderData(T player, Void unused, com.dragonminez.client.render.GeoEntityRenderState state, float partialTick) {
        super.addRenderData(player, unused, state, partialTick);
        state.firstPersonBody = !EntityPreviewRenderContext.isRendering()
                && FirstPersonManager.shouldRenderFirstPerson(player);
        // A small body-only setback keeps raised forearms from crossing the eye plane.
        if (state.firstPersonBody) state.firstPersonOffset = new Vector3f(0, 0, 0.25F);
    }

    /**
     * Any per-frame body offset is local to the model and applied exactly once.
     */
    @Override
    public void adjustRenderPose(RenderPassInfo<com.dragonminez.client.render.GeoEntityRenderState> info) {
        super.adjustRenderPose(info);

        // World translation is applied once by the listener, before model rotation and scale.
        if (!info.renderState().firstPersonBody) return;
        final Vector3f offset = info.renderState().firstPersonOffset;
        info.poseStack().translate(offset.x(), offset.y(), offset.z());
    }

    /**
     * Hides the head bone while the camera sits inside it.
     *
     * <p>GeckoLib 4 did this in {@code renderRecursively} by toggling {@code GeoBone#setHidden} and
     * restoring it afterwards. GeckoLib 5 bones are immutable and visibility lives on the per-frame
     * {@code BoneSnapshot}, so the snapshot is written once here.
     */
    @Override
    public void adjustModelBonesForRender(RenderPassInfo<com.dragonminez.client.render.GeoEntityRenderState> info, BoneSnapshots bones) {
        super.adjustModelBonesForRender(info, bones);
        if (bones == null) return;

        if (!info.renderState().firstPersonBody) return;

        bones.ifPresent("head", snapshot -> snapshot.skipRender(true).skipChildrenRender(true));
    }

    @Override
    public boolean shouldRender(T pLivingEntity, Frustum pCamera, double pCamX, double pCamY, double pCamZ) {
        if (pLivingEntity == Minecraft.getInstance().player) return !pLivingEntity.isSleeping();
        return super.shouldRender(pLivingEntity, pCamera, pCamX, pCamY, pCamZ) && !pLivingEntity.isSleeping();
    }
}
