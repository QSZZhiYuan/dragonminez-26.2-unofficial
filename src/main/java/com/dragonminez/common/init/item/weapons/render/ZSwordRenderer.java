package com.dragonminez.common.init.item.weapons.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.dragonminez.common.init.item.weapons.ZSwordItem;
import com.dragonminez.common.init.item.weapons.model.ZSwordModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.renderer.GeoItemRenderer;

public class ZSwordRenderer extends GeoItemRenderer<ZSwordItem> {

    public ZSwordRenderer() {
        super(new ZSwordModel());
    }

    @Override
    public RenderType getRenderType(com.geckolib.renderer.base.GeoRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}

