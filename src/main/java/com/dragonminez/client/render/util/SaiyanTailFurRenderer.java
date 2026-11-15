package com.dragonminez.client.render.util;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsData;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoQuad;
import com.geckolib.cache.model.GeoVertex;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.cache.model.cuboid.GeoCube;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Draws the animated tail over its single-texel skin mask with a mip-safe fur texture. */
public final class SaiyanTailFurRenderer {
	private static final Identifier FUR_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/tail_fur.png");
	private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(FUR_TEXTURE);
	private SaiyanTailFurRenderer() {}

	public static void submit(RenderPassInfo<GeoRenderState> info, SubmitNodeCollector collector,
			PoseStack entityPose, Player player, StatsData stats, int packedLight, float alpha) {
		var character = stats.getCharacter();
		String race = character.getRaceName().toLowerCase(java.util.Locale.ROOT);
		var raceConfig = ConfigManager.getRaceCharacter(race);
		boolean hasTailRace = race.equals("saiyan")
				|| (raceConfig != null && Boolean.TRUE.equals(raceConfig.getHasSaiyanTail()));
		String activeForm = character.getActiveForm();
		boolean ssj4Tail = activeForm != null && (activeForm.contains("supersaiyan4") || activeForm.contains("ssj4"));
		if (!hasTailRace || !character.isHasSaiyanTail() || (!stats.getStatus().isTailVisible() && !ssj4Tail)
				|| hidesTail(character) || player.isSpectator()) return;

		float[] tailColor = tailColor(stats);
		int color = ARGB.colorFromFloat(alpha, tailColor[0], tailColor[1], tailColor[2]);
		List<GeoBone> tailBones = info.model().boneLookup().get().values().stream()
				.filter(SaiyanTailFurRenderer::isTailBone)
				.filter(bone -> bone instanceof CuboidGeoBone cuboid && cuboid.cubes.length > 0)
				.toList();
		if (tailBones.isEmpty()) return;

		Runnable captureAndSubmit = () -> {
			for (GeoBone bone : tailBones) {
				if (isHiddenInHierarchy(bone)) continue;
				List<GeoBone> ancestors = ancestors(bone);
				PoseStack bonePose = new PoseStack();
				bonePose.last().set(entityPose.last());
				for (GeoBone ancestor : ancestors) RenderUtil.prepMatrixForBone(bonePose, ancestor);

				for (GeoCube cube : ((CuboidGeoBone) bone).cubes) {
					PoseStack cubePose = new PoseStack();
					cubePose.last().set(bonePose.last());
					cube.translateToPivotPoint(cubePose);
					cube.rotate(cubePose);
					cube.translateAwayFromPivotPoint(cubePose);
					collector.submitCustomGeometry(cubePose, RENDER_TYPE,
							(pose, vertices) -> emitCube(cube, pose, vertices, packedLight, color));
				}
			}
		};

		if (isPosed(info)) captureAndSubmit.run();
		else info.renderPosed(captureAndSubmit);
	}

	private static void emitCube(GeoCube cube, PoseStack.Pose pose, VertexConsumer vertices, int light, int color) {
		for (GeoQuad quad : cube.quads()) {
			if (quad == null) continue;
			Vector3f normal = quad.normalVec().normalize();
			GeoVertex[] corners = quad.vertices();
			for (int i = 0; i < corners.length; i++) {
				GeoVertex vertex = corners[i];
				float u = (i == 1 || i == 2) ? 1.0f : 0.0f;
				float v = (i >= 2) ? 1.0f : 0.0f;
				vertices.addVertex(pose, vertex.posX(), vertex.posY(), vertex.posZ())
						.setColor(color)
						.setUv(u, v)
						.setOverlay(OverlayTexture.NO_OVERLAY)
						.setLight(light)
						.setNormal(pose, normal.x, normal.y, normal.z);
			}
		}
	}

	private static boolean isPosed(RenderPassInfo<GeoRenderState> info) {
		return info.model().boneLookup().get().values().stream().anyMatch(bone -> bone.frameSnapshot != null);
	}

	private static boolean isHiddenInHierarchy(GeoBone bone) {
		for (GeoBone current = bone; current != null; current = current.parent()) {
			if (current.frameSnapshot != null && (current.frameSnapshot.isHidden()
					|| (current != bone && current.frameSnapshot.areChildrenHidden()))) return true;
		}
		return false;
	}

	private static List<GeoBone> ancestors(GeoBone bone) {
		List<GeoBone> result = new ArrayList<>();
		for (GeoBone current = bone; current != null; current = current.parent()) result.add(current);
		java.util.Collections.reverse(result);
		return result;
	}

	private static boolean isTailBone(GeoBone bone) {
		String name = bone.name();
		if (name.length() <= 4 || !name.startsWith("tail")) return false;
		for (int i = 4; i < name.length(); i++) if (!Character.isDigit(name.charAt(i))) return false;
		return true;
	}

	private static boolean hidesTail(com.dragonminez.common.stats.character.Character character) {
		var form = character.getActiveFormData();
		var stack = character.getActiveStackFormData();
		return (form != null && form.hidesTail()) || (stack != null && stack.hidesTail());
	}

	private static float[] tailColor(StatsData stats) {
		var character = stats.getCharacter();
		float[] color = character.getBodyColor2() != null && !character.getBodyColor2().isEmpty()
				? character.getRgbBodyColor2() : new float[]{0.341f, 0.129f, 0.090f};
		var form = character.getActiveFormData();
		if (form != null && form.getBodyColor2() != null && !form.getBodyColor2().isEmpty()) color = form.getRgbBodyColor2();
		var stack = character.getActiveStackFormData();
		if (stack != null && stack.getBodyColor2() != null && !stack.getBodyColor2().isEmpty()) color = stack.getRgbBodyColor2();
		return color;
	}
}
