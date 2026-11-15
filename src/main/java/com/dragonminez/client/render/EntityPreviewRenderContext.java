package com.dragonminez.client.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Marks entity renders performed by DMZ menus so first-person-only layer rules do
 * not accidentally hide parts of the character preview.
 */
public final class EntityPreviewRenderContext {
	private static int renderDepth;
	private static int hudPortraitDepth;

	private EntityPreviewRenderContext() {
	}

	public static boolean isRendering() {
		return renderDepth > 0;
	}

	public static void renderEntityInInventory(GuiGraphicsExtractor graphics, int x, int y, int scale,
			Vector3f translation, Quaternionf pose, Quaternionf cameraOrientation, LivingEntity entity) {
		renderEntityInInventory(graphics, x, y, (float) scale, translation, pose, cameraOrientation, entity);
	}

	public static void renderEntityInInventory(GuiGraphicsExtractor graphics, int x, int y, float scale,
			Vector3f translation, Quaternionf pose, Quaternionf cameraOrientation, LivingEntity entity) {
		renderDepth++;
		try {
			if (!(scale > 0.0F)) return;
			EntityRenderer<? super LivingEntity, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
			EntityRenderState state = renderer.createRenderState(entity, 1.0F);
			state.shadowPieces.clear();
			state.outlineColor = 0;
			state.lightCoords = 15728880;
			state.nameTag = null;
			Character appearance = StatsProvider.get(StatsCapability.INSTANCE, entity)
					.map(stats -> copyCharacter(stats.getCharacter())).orElse(null);
			graphics.submitPictureInPictureRenderState(EntityPreviewRenderState.create(
					graphics, entity, state, appearance, isHudPortrait(), x, y, scale,
					translation, pose, cameraOrientation));
		} finally {
			renderDepth--;
		}
	}

	private static Character copyCharacter(Character source) {
		Character copy = new Character();
		copy.copyFrom(source);
		return copy;
	}

	/** Keep appearance and preview flags active until deferred geometry has actually been drawn. */
	static void withAppearance(EntityPreviewRenderState state, Runnable render) {
		Character live = StatsProvider.get(StatsCapability.INSTANCE, state.entity())
				.map(stats -> stats.getCharacter()).orElse(null);
		Character original = live == null ? null : copyCharacter(live);
		boolean previousFormPreview = com.dragonminez.client.render.layer.DMZSkinLayer.PREVIEW_MODE;
		renderDepth++;
		if (state.hudPortrait()) hudPortraitDepth++;
		try {
			com.dragonminez.client.render.layer.DMZSkinLayer.PREVIEW_MODE = state.formPreview();
			if (live != null && state.appearance() != null) live.copyFrom(state.appearance());
			render.run();
		} finally {
			com.dragonminez.client.render.layer.DMZSkinLayer.PREVIEW_MODE = previousFormPreview;
			if (live != null && original != null) live.copyFrom(original);
			if (state.hudPortrait()) hudPortraitDepth--;
			renderDepth--;
		}
	}

	public static boolean isHudPortrait() {
		return hudPortraitDepth > 0;
	}

	public static void renderHeadPortrait(GuiGraphicsExtractor graphics, LivingEntity player,
			float x, float y, float size, float partialTick, boolean mirrored) {
		// GeoEntityRenderer rotates the extracted state by 180 degrees in 26.2.
		renderHeadPortrait(graphics, player, x, y, size, partialTick, mirrored ? 204 : 156);
	}

	public static void renderHeadPortrait(GuiGraphicsExtractor graphics, LivingEntity player,
			float x, float y, float size, float partialTick, float yawDegrees) {
		renderDepth++;
		hudPortraitDepth++;
		try {
			var left = graphics.pose().transformPosition(x, y, new org.joml.Vector2f());
			var right = graphics.pose().transformPosition(x + size, y + size, new org.joml.Vector2f());
			int x0 = Math.round(Math.min(left.x, right.x)), y0 = Math.round(Math.min(left.y, right.y));
			int x1 = Math.round(Math.max(left.x, right.x)), y1 = Math.round(Math.max(left.y, right.y));
			if (x1 <= x0 || y1 <= y0) return;
			var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
			var state = renderer.createRenderState(player, partialTick);
			state.shadowPieces.clear(); state.outlineColor = 0; state.nameTag = null; state.lightCoords = 15728880;
			if (state instanceof net.minecraft.client.renderer.entity.state.LivingEntityRenderState living) {
				living.bodyRot = living.yRot = living.xRot = 0;
				living.walkAnimationPos = living.walkAnimationSpeed = living.deathTime = 0;
				living.pose = net.minecraft.world.entity.Pose.STANDING;
				living.isUpsideDown = living.isAutoSpinAttack = false;
			}
			var geo = (com.geckolib.renderer.base.GeoRenderState) state;
			geo.addGeckolibData(com.geckolib.constant.DataTickets.ENTITY_BODY_YAW, 0.0f);
			geo.addGeckolibData(com.geckolib.constant.DataTickets.ENTITY_YAW, 0.0f);
			geo.addGeckolibData(com.geckolib.constant.DataTickets.ENTITY_PITCH, 0.0f);
			geo.addGeckolibData(com.dragonminez.client.init.entities.model.DmzHeadLook.HEAD_PITCH, 0.0f);
			geo.addGeckolibData(com.dragonminez.client.init.entities.model.DmzHeadLook.HEAD_YAW, 0.0f);
			Character appearance = StatsProvider.get(StatsCapability.INSTANCE, player)
					.map(data -> copyCharacter(data.getCharacter())).orElse(null);
			float scale = (x1 - x0) * 0.62f / 0.5f;
			var rotation = new Quaternionf().rotateZ((float) Math.PI)
					.rotateX((float) Math.toRadians(6)).rotateY((float) Math.toRadians(yawDegrees));
			var area = new net.minecraft.client.gui.navigation.ScreenRectangle(x0, y0, x1 - x0, y1 - y0);
			var clip = graphics.peekScissorStack();
			if (clip != null) area = clip.intersection(area);
			graphics.submitPictureInPictureRenderState(new EntityPreviewRenderState(player, state, appearance, true, false,
					new Vector3f(0, -(y1 - y0) / (2f * scale), 0), rotation, null,
					x0, y0, x1, y1, scale, area, area));
		} finally {
			hudPortraitDepth--;
			renderDepth--;
		}
	}

	public static void renderHudPortrait(GuiGraphicsExtractor graphics, int x, int y, float scale,
			Vector3f translation, Quaternionf pose, Quaternionf cameraOrientation, LivingEntity entity) {
		hudPortraitDepth++;
		try {
			renderEntityInInventory(graphics, x, y, scale, translation, pose, cameraOrientation, entity);
		} finally {
			hudPortraitDepth--;
		}
	}
}

