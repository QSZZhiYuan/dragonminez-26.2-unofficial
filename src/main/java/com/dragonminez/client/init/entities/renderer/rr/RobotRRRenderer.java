package com.dragonminez.client.init.entities.renderer.rr;

import com.dragonminez.client.init.entities.model.rr.RedRibbonRobotModel;
import com.dragonminez.common.init.entities.redribbon.RedRibbonEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.renderer.GeoEntityRenderer;

public class RobotRRRenderer<T extends RedRibbonEntity> extends GeoEntityRenderer<T, LivingEntityRenderState> {

    public RobotRRRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RedRibbonRobotModel<>());
        this.shadowRadius = 0.8f;
    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}

