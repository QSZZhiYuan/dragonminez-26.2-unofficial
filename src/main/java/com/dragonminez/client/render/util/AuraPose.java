package com.dragonminez.client.render.util;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;

/** Captures a torso's position and up axis without retaining a camera/entity render matrix. */
public record AuraPose(Vector3fc offset, Vector3fc up) {
    public static AuraPose upright() {
        return new AuraPose(new Vector3f(), new Vector3f(0, 1, 0));
    }

    public static AuraPose capture(RenderPassInfo<GeoRenderState> source) {
        var pose = new PoseStack();
        pose.mulPose(new Matrix4f(source.getPreRenderMatrixState()).invert().mul(source.poseStack().last().pose()));
        GeoBone torso = source.model().getBone("body")
                .or(() -> source.model().getBone("waist"))
                .or(() -> source.model().getBone("root")).orElse(null);
        if (torso != null) {
            var ancestors = new ArrayList<GeoBone>();
            for (GeoBone bone = torso; bone != null; bone = bone.parent()) ancestors.add(bone);
            Runnable capture = () -> {
                for (GeoBone bone : ancestors.reversed()) RenderUtil.prepMatrixForBone(pose, bone);
            };
            // A layer may also run inside an existing posed pass. Do not clean up
            // that pass's snapshots by nesting renderPosed.
            if (source.model().boneLookup().get().values().stream().anyMatch(bone -> bone.frameSnapshot != null))
                capture.run();
            else source.renderPosed(capture);
        }
        return fromTransform(pose.last().pose());
    }

    public static AuraPose fromTransform(Matrix4fc transform) {
        var offset = transform.getTranslation(new Vector3f());
        var up = transform.transformDirection(new Vector3f(0, 1, 0));
        if (!offset.isFinite() || !up.isFinite() || up.lengthSquared() < 1.0e-8f) return upright();
        return new AuraPose(offset, up.normalize());
    }

    /** Applies an extra model-local transform, such as the renderer's fast-flight bank. */
    public AuraPose transformedBy(Matrix4fc transform) {
        var transformedOffset = transform.transformPosition(new Vector3f(offset));
        var transformedUp = transform.transformDirection(new Vector3f(up));
        if (!transformedOffset.isFinite() || !transformedUp.isFinite() || transformedUp.lengthSquared() < 1.0e-8f)
            return this;
        return new AuraPose(transformedOffset, transformedUp.normalize());
    }

    /** Cylindrical billboard: faces the camera while its vertical axis follows the torso. */
    public Matrix4f billboard(Quaternionfc cameraRotation) {
        var view = cameraRotation.transform(new Vector3f(0, 0, 1));
        var right = new Vector3f(up).cross(view);
        if (right.lengthSquared() < 1.0e-6f) {
            right = cameraRotation.transform(new Vector3f(1, 0, 0));
            right.sub(new Vector3f(up).mul(right.dot(up)));
        }
        right.normalize();
        var normal = new Vector3f(right).cross(up).normalize();
        return new Matrix4f()
                .m00(right.x).m01(right.y).m02(right.z)
                .m10(up.x()).m11(up.y()).m12(up.z())
                .m20(normal.x).m21(normal.y).m22(normal.z);
    }

    public Quaternionf rotation() {
        return new Quaternionf().rotationTo(new Vector3f(0, 1, 0), up);
    }

    public float endOnAmount(Quaternionfc cameraRotation) {
        float dot = Math.abs(cameraRotation.transform(new Vector3f(0, 0, 1)).dot(up));
        float angle = (float) Math.toDegrees(Math.asin(Math.clamp(dot, 0, 1)));
        float t = Math.clamp((angle - 45) / 45, 0, 1);
        return t * t;
    }
}
