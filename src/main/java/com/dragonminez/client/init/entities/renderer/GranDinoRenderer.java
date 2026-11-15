package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.DinoGlobalModel;
import com.dragonminez.common.init.entities.animal.DinoGlobalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.renderer.GeoEntityRenderer;

public class GranDinoRenderer<T extends DinoGlobalEntity, R extends LivingEntityRenderState & com.geckolib.renderer.base.GeoRenderState> extends GeoEntityRenderer<T, R> {

    public GranDinoRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DinoGlobalModel<>());
        this.shadowRadius = 1.3f;
    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }

    /**
     * 26.2 splits entity rendering into extractRenderState + submit, so the old
     * render(T, ...) override no longer exists. GeckoLib's adjustRenderPose hook is the
     * place to apply the model-space scale/offset that used to live in render().
     */
    @Override
    public void adjustRenderPose(com.geckolib.renderer.base.RenderPassInfo<R> info) {
        super.adjustRenderPose(info);
        info.poseStack().scale(1.5f, 1.5f, 1.5f);
    }
}

