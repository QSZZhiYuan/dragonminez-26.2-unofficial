package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.QuestNPCModel;
import com.dragonminez.common.init.entities.questnpc.QuestNPCEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.renderer.GeoEntityRenderer;

public class QuestNPCRenderer extends GeoEntityRenderer<QuestNPCEntity, LivingEntityRenderState> {

	public QuestNPCRenderer(EntityRendererProvider.Context renderManager) {
		super(renderManager, new QuestNPCModel());
		this.shadowRadius = 0.4f;
	}

	@Override
	public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
		return RenderTypes.entityCutout(texture);
	}
}


