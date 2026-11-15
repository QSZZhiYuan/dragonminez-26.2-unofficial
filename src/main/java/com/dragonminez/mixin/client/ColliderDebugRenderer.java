package com.dragonminez.mixin.client;

import com.dragonminez.client.collision.OrientedBoundingBox;
import com.dragonminez.client.collision.TargetFinder;
import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import com.dragonminez.common.combat.util.Minecraft_DMZ;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Draws DMZ's attack-collider debug geometry (the oriented bounding box plus the target hitboxes).
 *
 * <h2>26.2 migration</h2>
 * 1.21 injected at the tail of {@code DebugRenderer#render(PoseStack, MultiBufferSource, double,
 * double, double)} and emitted {@code VertexConsumer} lines directly. 26.2 removed that method
 * entirely: the debug renderer now runs through {@code emitGizmos(Frustum, double, double, double,
 * float)} and per-renderer {@code DebugRenderer.SimpleDebugRenderer#emitGizmos(...)}, and there is no
 * {@code PoseStack} or {@code VertexConsumer} in the signature at all.
 *
 * <p>The replacement is the gizmo system: {@code Gizmos.line(Vec3, Vec3, int argb)} queues a world
 * space line that the renderer draws for us, so this class no longer needs a matrix, a buffer source
 * or manual camera-relative subtraction — the arguments are absolute world positions.
 *
 * <p>The old code also passed a {@code null} {@code VertexConsumer} into its draw helpers (a leftover
 * from the in-progress port), so nothing was actually being drawn. Moving to gizmos fixes that as a
 * side effect.
 */
@Mixin(DebugRenderer.class)
public class ColliderDebugRenderer {

	private static final int OBB_COLOR = 0xFF00FFFF;   // cyan, opaque
	private static final int TARGET_COLOR = 0xFFFF0000; // red, opaque

	@Inject(method = "emitGizmos", at = @At("TAIL"))
	private void dragonminez$renderColliderDebug(Frustum frustum, double cameraX, double cameraY,
												 double cameraZ, float partialTick, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		if (!client.debugEntries.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES)) return;
		LocalPlayer player = client.player;
		if (player == null) return;

		var mcDMZ = (Minecraft_DMZ) client;
		var hand = PlayerAttackHelper.getCurrentAttack(player, mcDMZ.getComboCount());
		if (hand == null) return;

		var attack = hand.attack();
		var attributes = hand.attributes();
		double attackRange = PlayerAttackHelper.getEffectiveAttackRange(player, attributes.attackRange());
		var cursorTarget = mcDMZ.getCursorTarget();

		TargetFinder.TargetResult targetResult =
				TargetFinder.findAttackTargetResult(player, cursorTarget, attack, attackRange);

		OrientedBoundingBox obb = targetResult.obb;
		// vertex1..8 are only filled in by updateVertex(); guard so a partially built box does not NPE.
		if (obb != null && obb.vertex1 != null && obb.vertex8 != null) {
			Vec3[] v = {obb.vertex1, obb.vertex2, obb.vertex3, obb.vertex4,
					obb.vertex5, obb.vertex6, obb.vertex7, obb.vertex8};
			dragonminez$box(v, OBB_COLOR);
		}

		List<AABB> targetBoxes = targetResult.entities.stream().map(Entity::getBoundingBox).toList();
		for (AABB box : targetBoxes) {
			dragonminez$box(dragonminez$vertices(box), TARGET_COLOR);
		}
	}

	/** Emits the twelve edges of a box given its eight corners (same vertex order as vanilla's AABB). */
	private static void dragonminez$box(Vec3[] v, int argb) {
		int[][] edges = {
				{0, 1}, {1, 2}, {2, 3}, {3, 0},   // bottom
				{4, 5}, {5, 6}, {6, 7}, {7, 4},   // top
				{0, 4}, {1, 5}, {2, 6}, {3, 7}    // pillars
		};
		for (int[] e : edges) {
			Gizmos.line(v[e[0]], v[e[1]], argb);
		}
	}

	private static Vec3[] dragonminez$vertices(AABB box) {
		return new Vec3[]{
				new Vec3(box.minX, box.minY, box.minZ),
				new Vec3(box.maxX, box.minY, box.minZ),
				new Vec3(box.maxX, box.minY, box.maxZ),
				new Vec3(box.minX, box.minY, box.maxZ),
				new Vec3(box.minX, box.maxY, box.minZ),
				new Vec3(box.maxX, box.maxY, box.minZ),
				new Vec3(box.maxX, box.maxY, box.maxZ),
				new Vec3(box.minX, box.maxY, box.maxZ)
		};
	}
}
