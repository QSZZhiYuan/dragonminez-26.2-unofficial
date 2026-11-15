package com.dragonminez.client.init.entities.renderer.sagas;

import com.dragonminez.client.init.entities.model.sagas.NamekianModel;
import com.dragonminez.common.init.entities.namek.NamekTraderEntity;
import com.dragonminez.common.init.entities.namek.NamekWarriorEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.renderer.GeoEntityRenderer;

public class NamekianWarriorRenderer extends GeoEntityRenderer<NamekWarriorEntity, LivingEntityRenderState> {

    public NamekianWarriorRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new NamekianModel<>());
        this.shadowRadius = 0.4f;
    }

    @Override
    public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.LivingEntityRenderState renderState) {
        if (renderState instanceof NamekianWarriorRenderState dmzState && dmzState.dmzTexture != null) {
            return dmzState.dmzTexture;
        }
        return super.getTextureLocation(renderState);
    }

    /**
     * 26.2 chooses the texture from the render state only, so the entity is captured here
     * (addRenderData still receives the live animatable) and read back above.
     */
    @Override
    public void addRenderData(NamekWarriorEntity animatable, Void unused,
                              net.minecraft.client.renderer.entity.state.LivingEntityRenderState renderState,
                              float partialTick) {
        super.addRenderData(animatable, unused, renderState, partialTick);
        if (renderState instanceof NamekianWarriorRenderState dmzState) {
            dmzState.dmzTexture = animatable.getCurrentTexture();
        }
    }

    /** Carries the per-entity texture choice into the render state. */
    public static class NamekianWarriorRenderState extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public Identifier dmzTexture;
    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}

