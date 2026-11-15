package com.dragonminez.client.init.entities.renderer.sagas;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.NamekFrogModel;
import com.dragonminez.client.init.entities.model.sagas.NamekianModel;
import com.dragonminez.common.init.entities.animal.NamekFrogEntity;
import com.dragonminez.common.init.entities.animal.NamekFrogGinyuEntity;
import com.dragonminez.common.init.entities.namek.NamekTraderEntity;
import com.dragonminez.common.init.entities.namek.NamekVillagerEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.PathfinderMob;
import org.jetbrains.annotations.Nullable;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.GeoEntityRenderer;

public class NamekianRenderer extends GeoEntityRenderer<NamekTraderEntity, LivingEntityRenderState> {

    public NamekianRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new NamekianModel<>());
        this.shadowRadius = 0.4f;
    }

    @Override
    public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.LivingEntityRenderState renderState) {
        if (renderState instanceof NamekianRenderState dmzState && dmzState.dmzTexture != null) {
            return dmzState.dmzTexture;
        }
        return super.getTextureLocation(renderState);
    }

    /**
     * 26.2 chooses the texture from the render state only, so the entity is captured here
     * (addRenderData still receives the live animatable) and read back above.
     */
    @Override
    public void addRenderData(NamekTraderEntity animatable, Void unused,
                              net.minecraft.client.renderer.entity.state.LivingEntityRenderState renderState,
                              float partialTick) {
        super.addRenderData(animatable, unused, renderState, partialTick);
        if (renderState instanceof NamekianRenderState dmzState) {
            dmzState.dmzTexture = animatable.getCurrentTexture();
        }
    }

    /** Carries the per-entity texture choice into the render state. */
    public static class NamekianRenderState extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public Identifier dmzTexture;
    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}

