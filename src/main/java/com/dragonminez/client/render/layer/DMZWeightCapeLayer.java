package com.dragonminez.client.render.layer;

import com.dragonminez.client.render.DMZPlayerRenderData;
import com.dragonminez.common.init.item.WeightItem;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.item.ItemStack;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.ItemArmorGeoLayer;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

/**
 * Renders the Piccolo-style weighted cape (a Curios "weights" slot item) on the {@code armorBody} bone.
 *
 * <h2>GeckoLib 5 migration</h2>
 * Same shape as {@link DMZCapeLayer} — {@code ItemArmorGeoLayer} now takes its bone bindings from
 * {@code getRelevantBones}, and the Curios "is a PICCOLO_CAPE actually equipped" check moved into
 * {@code submitRenderTask}. That check reads the player from {@link DMZPlayerRenderData}, never
 * {@code Minecraft.player}, so other players render with <i>their</i> own cape.
 *
 * <p>See {@link DMZCapeLayer} for the generic-bound workaround used by the bone declaration.
 */
public class DMZWeightCapeLayer<T extends AbstractClientPlayer & GeoAnimatable>
        extends ItemArmorGeoLayer<T, Void, LivingEntityRenderState> {

    /**
	 * @param geoRenderer the owning renderer
	 * @param context     vanilla's renderer context — must not be null
	 *
	 * <p>Same requirement as {@link DMZPlayerArmorLayer}: GeckoLib's {@code ItemArmorGeoLayer}
	 * constructor reads {@code equipmentRenderer} / {@code equipmentAssets} /
	 * {@code playerSkinRenderCache} / {@code itemModelResolver} off the context immediately, so a
	 * {@code null} here crashes the player render on the first frame.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public DMZWeightCapeLayer(GeoRenderer<T, ?, ?> geoRenderer,
			  net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
		super((GeoRenderer) geoRenderer, context);
	}

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected List getRelevantBones(RenderPassInfo info) {
        return List.of(RenderData.body("armorBody"));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void submitRenderTask(RenderPassInfo info, SubmitNodeCollector collector) {
        DMZPlayerRenderData data = ((GeoRenderState) info.renderState())
                .getGeckolibData(DMZPlayerRenderData.TICKET);
        if (data == null || data.spectator()) return;
        if (!isPiccoloCapeEquipped(data.player())) return;
        super.submitRenderTask(info, collector);
    }

    /** True when this player's Curios "weights" slot holds a rendered PICCOLO_CAPE. */
    private boolean isPiccoloCapeEquipped(AbstractClientPlayer player) {
        if (player == null) return false;

        ItemStack stack = getWeightStack(player);
        if (stack.isEmpty() || !(stack.getItem() instanceof WeightItem weightItem)) return false;
        return weightItem.getWeightType() == WeightItem.WeightType.PICCOLO_CAPE;
    }

    private ItemStack getWeightStack(AbstractClientPlayer player) {
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) return ItemStack.EMPTY;

        var stacksHandler = inventory.getCurios().get("weights");
        if (stacksHandler == null) return ItemStack.EMPTY;

        if (!stacksHandler.getRenders().get(0)) return ItemStack.EMPTY;

        return stacksHandler.getStacks().getStackInSlot(0);
    }
}
