package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.client.init.entities.model.ki.SPDragonFistModel;
import com.dragonminez.common.init.entities.ki.SPDragonFistEntity;
import com.dragonminez.client.render.GeoKiRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.base.BoneSnapshots;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.ARGB;
import com.geckolib.renderer.GeoEntityRenderer;

public class SPDragonFistRenderer<T extends SPDragonFistEntity> extends GeoEntityRenderer<T, GeoKiRenderState> {

    public SPDragonFistRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SPDragonFistModel<>());
        this.shadowRadius = 0.8f;
    }

    @Override
    public GeoKiRenderState createRenderState(T entity, Void unused) {
        return new GeoKiRenderState();
    }

    @Override
    public void addRenderData(T entity, Void unused, GeoKiRenderState state, float partialTick) {
        super.addRenderData(entity, unused, state, partialTick);
        state.lockedYaw = entity.getLockedYaw();
        state.lockedPitch = entity.getLockedPitch();
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<GeoKiRenderState> info) {
        super.adjustRenderPose(info);
        info.poseStack().scale(5.0f, 5.0f, 5.0f);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<GeoKiRenderState> info, BoneSnapshots bones) {
        super.adjustModelBonesForRender(info, bones);
        bones.ifPresent("root", root -> root.setRotation(
                -info.renderState().lockedPitch * Mth.DEG_TO_RAD,
                -info.renderState().lockedYaw * Mth.DEG_TO_RAD, root.getRotZ()));
    }

    @Override
    public RenderType getRenderType(GeoKiRenderState renderState, Identifier texture) {
        return RenderTypes.entityTranslucentEmissive(texture);
    }

    @Override
    public int getRenderColor(T animatable, Void unused, float partialTick) {
        if (!animatable.isFiring()) return 0x00FFFFFF;
        float fadeDuration = 10.0f;
        float activeTick = animatable.tickCount + partialTick;

        float maxActiveLife = animatable.getMaxLife();
        float remainingLife = maxActiveLife - activeTick;

        float alpha = 0.5f;

        if (activeTick < fadeDuration) {
            alpha = (activeTick / fadeDuration) * 0.8f;
        }
        else if (remainingLife < fadeDuration) {
            alpha = (remainingLife / fadeDuration) * 0.8f;
        }
        else {
            alpha = 0.8f;
        }

        alpha = Mth.clamp(alpha, 0.0f, 0.8f);

        return ARGB.colorFromFloat(alpha, 1.0f, 1.0f, 1.0f);
    }
}
