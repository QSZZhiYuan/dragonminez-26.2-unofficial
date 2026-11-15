package com.dragonminez.client.init.entities.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.MajinSkillEntity;
import com.dragonminez.common.init.entities.animal.NamekFrogEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import com.geckolib.constant.DataTickets;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
public class MajinSkillModel<T extends MajinSkillEntity> extends GeoModel<T> {

    @Override
    public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/skills/majinskill");
    }

    @Override
    public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser.png");
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/skills/majinskill");
    }

}
