package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.client.init.entities.model.ki.SPDragonFistModel;
import com.dragonminez.client.init.entities.model.ki.SPOzaruFistModel;
import com.dragonminez.common.init.entities.ki.OzaruFistEntity;
import com.dragonminez.common.init.entities.ki.SPDragonFistEntity;
import com.mojang.blaze3d.vertex.PoseStack;
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

public class SPOzaruFistRenderer<T extends OzaruFistEntity> extends GeoEntityRenderer<T, GeoKiRenderState> {

    public SPOzaruFistRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SPOzaruFistModel<>());
        this.shadowRadius = 0.8f;
    }

    @Override
    public GeoKiRenderState createRenderState(T entity, Void unused) {
        return new GeoKiRenderState();
    }

    /**
     * 26.2 splits entity rendering into extractRenderState + submit, so the old
     * render(T, ...) override no longer exists. GeckoLib's adjustRenderPose hook is the
     * place to apply the model-space scale/offset that used to live in render().
     */
    @Override
    public void adjustRenderPose(com.geckolib.renderer.base.RenderPassInfo<GeoKiRenderState> info) {
        super.adjustRenderPose(info);
        info.poseStack().scale(5.0f, 5.0f, 5.0f);
        info.poseStack().translate(0.0f, -7.0f, -1.0f);
    }

    @Override
    public RenderType getRenderType(GeoKiRenderState renderState, Identifier texture) {
        return RenderTypes.entityTranslucentEmissive(texture);
    }

    @Override
    public int getRenderColor(T animatable, Void unused, float partialTick) {
        float fadeDuration = 10.0f;
        float activeTick = animatable.tickCount + partialTick;

        float maxActiveLife = animatable.getMaxLife();
        float remainingLife = maxActiveLife - activeTick;
        float maxAlpha = 0.75f;

        float alpha;

        if (activeTick < fadeDuration) {
            alpha = (activeTick / fadeDuration) * maxAlpha;
        }
        else if (remainingLife < fadeDuration) {
            alpha = (remainingLife / fadeDuration) * maxAlpha;
        }
        else {
            alpha = maxAlpha;
        }

        alpha = Mth.clamp(alpha, 0.0f, maxAlpha);

        return ARGB.colorFromFloat(alpha, 1.0f, 1.0f, 1.0f);
    }
}
