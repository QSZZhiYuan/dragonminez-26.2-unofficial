package com.dragonminez.client.init.entities.renderer.sagas;

import com.dragonminez.client.init.entities.model.BlackNimbusModel;
import com.dragonminez.client.init.entities.model.FlyingNimbusModel;
import com.dragonminez.common.init.entities.BlackNimbusEntity;
import com.dragonminez.common.init.entities.FlyingNimbusEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.renderer.GeoEntityRenderer;

public class BlackNimbusRenderer extends GeoEntityRenderer<BlackNimbusEntity, LivingEntityRenderState> {

    public BlackNimbusRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BlackNimbusModel<>());
    }

//    @Override
//    public void render(FlyingNimbusEntity entity, float entityYaw, float partialTick, PoseStack poseStack, SubmitNodeCollector bufferSource, int packedLight) {
//        poseStack.pushPose();
//        poseStack.scale(1.1f,1.1f,1.1f);
//        super.extractRenderState(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
//        poseStack.popPose();
//    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }
}

