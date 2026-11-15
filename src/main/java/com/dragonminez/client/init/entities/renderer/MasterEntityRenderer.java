package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.MasterGlobalModel;
import com.dragonminez.common.init.entities.MastersEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;

public class MasterEntityRenderer<T extends MastersEntity, R extends LivingEntityRenderState & com.geckolib.renderer.base.GeoRenderState> extends GeoEntityRenderer<T, R> {
    private static final com.geckolib.constant.dataticket.DataTicket<Float> MASTER_SCALE =
            com.geckolib.constant.dataticket.DataTicket.create("dmz_master_scale", Float.class);

    public MasterEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MasterGlobalModel<>());
        this.shadowRadius = 0.4f;
    }

    /**
     * 26.2 splits entity rendering into extractRenderState + submit, so the old
     * render(T, ...) override no longer exists. GeckoLib's adjustRenderPose hook is the
     * place to apply the model-space scale/offset that used to live in render().
     */
    @Override
    public void adjustRenderPose(com.geckolib.renderer.base.RenderPassInfo<R> info) {
        super.adjustRenderPose(info);
    }

    @Override
    public void addRenderData(T entity, Void unused, R state, float partialTick) {
        super.addRenderData(entity, unused, state, partialTick);
        state.addGeckolibData(MASTER_SCALE, entity.getModelScale());
        com.dragonminez.client.init.entities.model.DmzHeadLook.captureFrom(state, entity, partialTick);
    }

    @Override
    public void scaleModelForRender(com.geckolib.renderer.base.RenderPassInfo<R> info, float width, float height) {
        float scale = info.renderState().getOrDefaultGeckolibData(MASTER_SCALE, 1f);
        super.scaleModelForRender(info, width * scale, height * scale);
    }

    @Override
    public void adjustModelBonesForRender(com.geckolib.renderer.base.RenderPassInfo<R> info, com.geckolib.renderer.base.BoneSnapshots bones) {
        super.adjustModelBonesForRender(info, bones);
        com.dragonminez.client.init.entities.model.DmzHeadLook.apply(bones,
                info.renderState().getGeckolibData(com.dragonminez.client.init.entities.model.DmzHeadLook.HEAD_PITCH),
                info.renderState().getGeckolibData(com.dragonminez.client.init.entities.model.DmzHeadLook.HEAD_YAW));
    }



    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}

