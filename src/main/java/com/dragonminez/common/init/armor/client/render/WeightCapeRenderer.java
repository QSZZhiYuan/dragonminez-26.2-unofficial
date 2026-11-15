package com.dragonminez.common.init.armor.client.render;

import com.dragonminez.Reference;
import com.dragonminez.common.init.armor.client.model.WeightCapeModel;
import com.dragonminez.common.init.item.WeightItem;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.renderer.GeoArmorRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public class WeightCapeRenderer extends GeoArmorRenderer<WeightItem, HumanoidRenderState> {
    public WeightCapeRenderer() {
        super(new WeightCapeModel());
    }

    @Override
    public RenderType getRenderType(HumanoidRenderState renderState, Identifier texture) {
        return RenderTypes.armorCutoutNoCull(texture);
    }
}

