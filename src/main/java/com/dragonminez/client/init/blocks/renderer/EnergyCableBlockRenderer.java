package com.dragonminez.client.init.blocks.renderer;

import com.dragonminez.client.util.BoneVisibilityHandler;
import com.dragonminez.client.render.GeoBlockEntityRenderState;

import com.dragonminez.client.init.blocks.model.EnergyCableBlockModel;
import com.dragonminez.common.init.block.custom.EnergyCableBlock;
import com.dragonminez.common.init.block.entity.EnergyCableBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.RenderPassInfo;

/**
 * Energy cable: hides the bone for each direction the cable is <em>not</em> connected in.
 *
 * <h2>GeckoLib 5 migration</h2>
 * GeckoLib 4 did this from {@code preRender(PoseStack, T, BakedGeoModel, ...)}, which still received
 * the live block entity and could look bones up with {@code GeoModel#getBone(String)}.
 * GeckoLib 5 removed both:
 * <ul>
 *   <li>{@code preRender} is gone — {@code preRenderPass(RenderPassInfo, SubmitNodeCollector)} replaced it;</li>
 *   <li>bones are immutable and live on the per-frame snapshot set, so visibility is written through
 *       {@link BoneVisibilityHandler} onto {@code BoneSnapshots} in
 *       {@code adjustModelBonesForRender}.</li>
 * </ul>
 * The connected-direction flags travel from the block entity to the render pass on the render state.
 */
public class EnergyCableBlockRenderer extends GeoBlockRenderer<EnergyCableBlockEntity, GeoBlockEntityRenderState> {

	public EnergyCableBlockRenderer(BlockEntityRendererProvider.Context context) {
		super(context, new EnergyCableBlockModel());
	}

	@Override
	public GeoBlockEntityRenderState createRenderState() {
		// The block dispatcher uses this factory; GeckoLib defaults to the vanilla state.
		return new GeoBlockEntityRenderState();
	}

	@Override
	public void addRenderData(EnergyCableBlockEntity animatable, Void unused,
			GeoBlockEntityRenderState renderState, float partialTick) {
		super.addRenderData(animatable, unused, renderState, partialTick);
		if (animatable == null) return;

		BlockState state = animatable.getBlockState();
		if (!(state.getBlock() instanceof EnergyCableBlock)) return;

		for (Direction dir : Direction.values()) {
			if (!EnergyCableBlock.PROPERTY_BY_DIRECTION.containsKey(dir)) continue;
			com.dragonminez.client.init.blocks.model.EnergyCableBlockModel.putConnection(
					renderState, dir, state.getValue(EnergyCableBlock.PROPERTY_BY_DIRECTION.get(dir)));
		}
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<GeoBlockEntityRenderState> info,
			com.geckolib.renderer.base.BoneSnapshots bones) {
		super.adjustModelBonesForRender(info, bones);
		if (bones == null) return;

		for (Direction dir : Direction.values()) {
			Boolean connected = com.dragonminez.client.init.blocks.model.EnergyCableBlockModel
					.getConnection(info.renderState(), dir);
			if (connected == null) continue;
			bones.ifPresent(dir.getName().toLowerCase(),
					snapshot -> snapshot.skipRender(!connected.booleanValue()));
		}
	}
}
