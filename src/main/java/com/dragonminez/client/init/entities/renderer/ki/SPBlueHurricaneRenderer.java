package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.client.init.entities.model.ki.SPSkillsModel;
import com.dragonminez.common.init.entities.ki.SPBlueHurricaneEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.dragonminez.client.render.GeoKiRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import net.minecraft.util.ARGB;
import com.geckolib.renderer.GeoEntityRenderer;

public class SPBlueHurricaneRenderer<T extends SPBlueHurricaneEntity> extends GeoEntityRenderer<T, GeoKiRenderState> {

    public SPBlueHurricaneRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SPSkillsModel<>());
        this.shadowRadius = 0.8f;
    }

    @Override
    public GeoKiRenderState createRenderState(T entity, Void unused) {
        return new GeoKiRenderState();
    }

    @Override
    public RenderType getRenderType(GeoKiRenderState renderState, Identifier texture) {
        return RenderTypes.entityTranslucentEmissive(texture);
    }

    @Override
    public int getRenderColor(T animatable, Void unused, float partialTick) {
        if (!animatable.isFiring()) {
            return ARGB.colorFromFloat(0.0f, 1.0f, 1.0f, 1.0f);
        }

        float fadeDuration = 10.0f;

        float currentTick = (animatable.tickCount - animatable.getCastTime()) + partialTick;
        float maxLife = 140.0f;
        float remainingLife = maxLife - currentTick;

        float alpha = 1.0f;

        if (currentTick < fadeDuration) {
            alpha = currentTick / fadeDuration;
        } else if (remainingLife < fadeDuration) {
            alpha = remainingLife / fadeDuration;
        }

        alpha = Mth.clamp(alpha, 0.0f, 1.0f);

        return ARGB.colorFromFloat(alpha, 1.0f, 1.0f, 1.0f);
    }
}

