package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.DinoGlobalModel;
import com.dragonminez.common.init.entities.animal.DinoGlobalEntity;
import com.dragonminez.common.init.entities.animal.SabertoothEntity;
import com.dragonminez.common.init.entities.sagas.SagaNappaEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.renderer.GeoEntityRenderer;

public class DinosRenderer<T extends DinoGlobalEntity> extends GeoEntityRenderer<T, LivingEntityRenderState> {

    public DinosRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DinoGlobalModel<>());
        this.shadowRadius = 0.8f;
    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }

    /**
     * 26.2: texture choice moved from getTextureLocation(T) to getTextureLocation(R renderState).
     * Only the render state is available at texture time, so the per-entity decision is captured in
     * addRenderData (which still receives the live entity) and read back here.
     */
    @Override
    public void addRenderData(T animatable, Void unused, LivingEntityRenderState renderState, float partialTick) {
        super.addRenderData(animatable, unused, renderState, partialTick);
        if (animatable instanceof SabertoothEntity tigre && renderState instanceof DinosRenderState state) {
            state.dmzTexture = tigre.getCurrentTexture();
        }
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState renderState) {
        if (renderState instanceof DinosRenderState state && state.dmzTexture != null) {
            return state.dmzTexture;
        }
        return super.getTextureLocation(renderState);
    }

    /** Carries the per-entity texture choice into the render state (see addRenderData). */
    public static class DinosRenderState extends LivingEntityRenderState {
        @Nullable
        public Identifier dmzTexture;
    }
}

