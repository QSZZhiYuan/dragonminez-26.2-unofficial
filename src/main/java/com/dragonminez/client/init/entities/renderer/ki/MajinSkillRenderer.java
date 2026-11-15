package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.client.init.entities.model.DinoGlobalModel;
import com.dragonminez.client.init.entities.model.MajinSkillModel;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.entities.MajinSkillEntity;
import com.dragonminez.common.init.entities.animal.DinoGlobalEntity;
import com.dragonminez.common.init.entities.animal.SabertoothEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import net.minecraft.util.ARGB;
import com.geckolib.renderer.GeoEntityRenderer;

public class MajinSkillRenderer<T extends MajinSkillEntity> extends GeoEntityRenderer<T, LivingEntityRenderState> {

    public MajinSkillRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MajinSkillModel<>());
        this.shadowRadius = 0.4f;
    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }

    @Override
    public int getRenderColor(T animatable, Void unused, float partialTick) {
        int colorInt = animatable.getBodyColor();

        float[] rgb = ColorUtils.rgbIntToFloat(colorInt);

        return ARGB.colorFromFloat(1.0f, rgb[0], rgb[1], rgb[2]);

    }
}

