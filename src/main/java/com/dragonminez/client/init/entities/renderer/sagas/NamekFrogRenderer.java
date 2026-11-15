package com.dragonminez.client.init.entities.renderer.sagas;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.DinoGlobalModel;
import com.dragonminez.client.init.entities.model.NamekFrogModel;
import com.dragonminez.common.init.entities.animal.DinoGlobalEntity;
import com.dragonminez.common.init.entities.animal.NamekFrogEntity;
import com.dragonminez.common.init.entities.animal.NamekFrogGinyuEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.renderer.GeoEntityRenderer;

import javax.naming.Name;

public class NamekFrogRenderer<T extends NamekFrogEntity> extends GeoEntityRenderer<T, LivingEntityRenderState> {

    private static final Identifier GINYU_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/animal/namekfrog_0.png");

    public NamekFrogRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new NamekFrogModel<>());
        this.shadowRadius = 0.25f;
    }

    @Override
    public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.LivingEntityRenderState renderState) {
        if (renderState instanceof NamekFrogRenderState dmzState && dmzState.dmzTexture != null) {
            return dmzState.dmzTexture;
        }
        return super.getTextureLocation(renderState);
    }

    /**
     * 26.2 picks the texture from the render state only; the entity is captured here
     * (addRenderData still receives the live animatable) and read back above.
     */
    @Override
    public void addRenderData(T animatable, Void unused,
                              net.minecraft.client.renderer.entity.state.LivingEntityRenderState renderState,
                              float partialTick) {
        super.addRenderData(animatable, unused, renderState, partialTick);
        if (renderState instanceof NamekFrogRenderState dmzState) {
            dmzState.dmzTexture = (animatable instanceof NamekFrogGinyuEntity) ? GINYU_TEXTURE : animatable.getCurrentTexture();
        }
    }

    /** Carries the per-entity texture choice into the render state. */
    public static class NamekFrogRenderState extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public Identifier dmzTexture;
    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }

}

