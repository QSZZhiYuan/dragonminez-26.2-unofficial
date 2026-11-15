package com.dragonminez.common.init.item.weapons.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.dragonminez.common.init.item.weapons.BraveSwordItem;
import com.dragonminez.common.init.item.weapons.DimensionalSwordItem;
import com.dragonminez.common.init.item.weapons.model.BraveSwordModel;
import com.dragonminez.common.init.item.weapons.model.DimensionalSwordModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.renderer.GeoItemRenderer;

public class DimensionalSwordRenderer extends GeoItemRenderer<DimensionalSwordItem> {

	public DimensionalSwordRenderer() {
		super(new DimensionalSwordModel());
	}

    /**
     * Hides the scabbard while the sword is held.
     *
     * <h2>GeckoLib 5 migration</h2>
     * The 1.20.1 code overrode {@code actuallyRender}, hid the bone, called super and restored it.
     * In GeckoLib 5 the bone walk is driven by a {@code BoneSnapshots} set, so the bone is simply
     * marked {@code skipRender} for this pass - no save/restore dance needed.
     */
    @Override
    public void adjustModelBonesForRender(com.geckolib.renderer.base.RenderPassInfo<com.geckolib.renderer.base.GeoRenderState> info,
                                          com.geckolib.renderer.base.BoneSnapshots bones) {
        super.adjustModelBonesForRender(info, bones);
        bones.ifPresent("cubretodo", snapshot -> snapshot.skipRender(true));
    }

	@Override
	public RenderType getRenderType(com.geckolib.renderer.base.GeoRenderState renderState, Identifier texture) {
		return RenderTypes.entityCutout(texture);
	}
}
