package com.dragonminez.client.render.shader;

import com.dragonminez.Reference;
import com.dragonminez.common.init.block.entity.GravityDeviceBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Toggleable client-side visualization (shift + right-click on a Gravity Device) that draws the
 * bounds of the device's detected room. Green when the room is valid, red when it is not.
 */
@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class GravityRangeRenderer {

	private static final Set<BlockPos> SHOWN = new HashSet<>();

	private GravityRangeRenderer() {}

	public static void toggle(BlockPos pos) {
		BlockPos key = pos.immutable();
		if (!SHOWN.remove(key)) SHOWN.add(key);
	}

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent.AfterTranslucentBlocks event) {
		// 26.2: stage is guaranteed by the event subclass
		if (SHOWN.isEmpty()) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;

		Vec3 cam = mc.gameRenderer.mainCamera().position();
		PoseStack poseStack = event.getPoseStack();
		// 26.2: no Minecraft#renderBuffers(); boxes go through submitShapeOutline on the frame's
		// collector. LevelRenderer.renderLineBox is gone as well.
		var collector = com.dragonminez.client.render.util.RenderBufferUtil.frameCollector();

		poseStack.pushPose();
		poseStack.translate(-cam.x, -cam.y, -cam.z);

		Iterator<BlockPos> it = SHOWN.iterator();
		while (it.hasNext()) {
			BlockPos pos = it.next();
			BlockEntity be = mc.level.getBlockEntity(pos);
			if (!(be instanceof GravityDeviceBlockEntity device)) {
				it.remove();
				continue;
			}

			boolean valid = device.isRoomValid();
			AABB box;
			if (valid) {
				BlockPos min = device.getRoomMin();
				BlockPos max = device.getRoomMax();
				box = new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1.0, max.getY() + 1.0, max.getZ() + 1.0);
			} else {
				box = new AABB(pos).inflate(0.02);
			}

			int color = valid ? 0x3366_FF66 : 0xE633_3333;
			collector.submitShapeOutline(poseStack, net.minecraft.world.phys.shapes.Shapes.create(box), RenderTypes.lines(),
					color, 1.0f, false);
		}

		poseStack.popPose();
	}
}

