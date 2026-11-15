package com.dragonminez.client.render.util;

import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Draws an accessory subtree with an independent, captured player pose. */
public final class GeoAttachmentRenderer {
    private GeoAttachmentRenderer() {}

    public static void submit(RenderPassInfo<GeoRenderState> source, PoseStack pose,
            SubmitNodeCollector collector, GeoBone target, RenderType type,
            int light, int overlay, int color) {
        submit(source, pose, collector, target, type, light, overlay, color, Map.of(), Set.of());
    }

    public static void submit(RenderPassInfo<GeoRenderState> source, PoseStack pose,
            SubmitNodeCollector collector, GeoBone target, RenderType type,
            int light, int overlay, int color, Map<String, String> poseSources, Set<String> hidden) {
        submit(source, pose, collector, target, type, light, overlay, color, poseSources, hidden, true);
    }

    /** Mounts a stowed weapon to the torso, independently of its legacy hand rig. */
    public static void submitMounted(RenderPassInfo<GeoRenderState> source, PoseStack pose,
            SubmitNodeCollector collector, GeoBone target, GeoBone mount, RenderType type,
            int light, int overlay, int color, float scale, Matrix4fc localTransform, Set<String> hidden) {
        var ancestors = new ArrayList<GeoBone>();
        for (GeoBone bone = mount; bone != null; bone = bone.parent()) ancestors.add(bone);
        var animated = new PoseStack();
        Runnable capture = () -> {
            for (GeoBone bone : ancestors.reversed()) RenderUtil.prepMatrixForBone(animated, bone);
        };
        if (isPosed(source)) capture.run();
        else source.renderPosed(capture);

        // Copying torso angles onto the hand rig rotates around the hand's pivot.
        // Apply the torso's model-space delta instead, then keep the weapon rig at bind pose.
        var bind = new PoseStack();
        for (GeoBone bone : ancestors.reversed()) {
            bone.translateToPivotPoint(bind);
            RenderUtil.optionalRotateZYX(bind, bone.baseRotZ(), bone.baseRotY(), bone.baseRotX());
            bone.translateAwayFromPivotPoint(bind);
        }
        var mounted = new PoseStack();
        mounted.last().set(pose.last());
        mounted.mulPose(new Matrix4f(animated.last().pose()).mul(new Matrix4f(bind.last().pose()).invert()));
        mounted.mulPose(localTransform);
        mounted.scale(scale, scale, scale);
        submit(source, mounted, collector, target, type, light, overlay, color, Map.of(), hidden, false);
    }

    /** Draws an attachment from a per-bone callback whose pose is already at the mount bone. */
    public static void submitAttached(RenderPassInfo<GeoRenderState> source, PoseStack pose,
            SubmitNodeCollector collector, GeoBone target, RenderType type,
            int light, int overlay, int color, float scale, Matrix4fc localTransform, Set<String> hidden) {
        var attached = new PoseStack();
        attached.last().set(pose.last());
        attached.mulPose(localTransform);
        attached.scale(scale, scale, scale);
        submit(source, attached, collector, target, type, light, overlay, color, Map.of(), hidden, false);
    }

    private static boolean isPosed(RenderPassInfo<GeoRenderState> source) {
        return source.model().boneLookup().get().values().stream().anyMatch(bone -> bone.frameSnapshot != null);
    }

    private static void submit(RenderPassInfo<GeoRenderState> source, PoseStack pose,
            SubmitNodeCollector collector, GeoBone target, RenderType type,
            int light, int overlay, int color, Map<String, String> poseSources, Set<String> hidden, boolean copyPose) {
        GeoBone root = target;
        while (root.parent() != null) root = root.parent();
        var model = new BakedGeoModel(new GeoBone[]{root}, Map.of(), null);
        Map<String, BonePose> captured = new HashMap<>();
        Runnable capture = () -> model.boneLookup().get().forEach((name, bone) ->
                source.model().getBone(poseSources.getOrDefault(name, name)).ifPresent(playerBone -> {
                    if (playerBone.frameSnapshot != null)
                        captured.put(name, BonePose.capture(playerBone.frameSnapshot));
                }));
        // Per-bone callbacks already run inside the player's posed pass. Nesting renderPosed
        // would clean up its snapshots and remove the pose from subsequent accessories.
        if (copyPose) {
            if (isPosed(source)) capture.run();
            else source.renderPosed(capture);
        }

        PoseStack attachmentPose = new PoseStack();
        attachmentPose.last().set(pose.last());
        var pass = new AttachmentPass(source, attachmentPose, model);
        pass.addBoneUpdater((info, snapshots) -> model.boneLookup().get().forEach((name, bone) -> {
            BoneSnapshot snapshot = snapshots.get(bone);
            BonePose capturedPose = captured.get(name);
            if (capturedPose != null) capturedPose.apply(snapshot);
            if (hidden.contains(name)) {
                snapshot.skipRender(true);
                snapshot.skipChildrenRender(true);
            }
        }));
        collector.submitCustomGeometry(attachmentPose, type, (drawPose, vertices) -> {
            attachmentPose.pushPose();
            try {
                attachmentPose.last().set(drawPose);
                pass.renderPosed(() -> {
                    var parents = new ArrayList<GeoBone>();
                    for (GeoBone parent = target.parent(); parent != null; parent = parent.parent())
                        parents.add(parent);
                    for (GeoBone parent : parents.reversed()) RenderUtil.prepMatrixForBone(attachmentPose, parent);
                    target.positionAndRender(pass, vertices, light, overlay, color);
                });
            } finally {
                attachmentPose.popPose();
            }
        });
    }

    private static final class AttachmentPass extends RenderPassInfo<GeoRenderState> {
        AttachmentPass(RenderPassInfo<GeoRenderState> source, PoseStack pose, BakedGeoModel model) {
            super(source.renderer(), source.renderState(), pose, model, source.cameraState(), true);
        }
    }

    private record BonePose(float x, float y, float z, float tx, float ty, float tz,
                            float sx, float sy, float sz) {
        static BonePose capture(BoneSnapshot pose) {
            return new BonePose(pose.getRotX(), pose.getRotY(), pose.getRotZ(),
                    pose.getTranslateX(), pose.getTranslateY(), pose.getTranslateZ(),
                    pose.getScaleX(), pose.getScaleY(), pose.getScaleZ());
        }
        void apply(BoneSnapshot pose) {
            pose.setRotation(x, y, z);
            pose.setTranslation(tx, ty, tz);
            pose.setScale(sx, sy, sz);
        }
    }
}
