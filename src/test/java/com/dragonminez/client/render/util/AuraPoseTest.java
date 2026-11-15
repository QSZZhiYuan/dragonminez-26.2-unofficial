package com.dragonminez.client.render.util;

import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoLocator;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuraPoseTest {
    @Test
    void standingAuraStaysVerticalWhenTheCameraLooksUpOrDown() {
        var pose = AuraPose.upright();
        for (float pitch : new float[]{-1.5707963f, -0.8f, 0, 0.8f, 1.5707963f}) {
            var camera = new Quaternionf().rotationYXZ(0.8f, pitch, 0.3f);
            var billboard = pose.billboard(camera);
            assertDirection(new Vector3f(0, 1, 0), billboard.transformDirection(new Vector3f(0, 1, 0)));
            assertTrue(billboard.isFinite());
            assertEquals(1, billboard.determinant(), 0.0001);
        }
    }

    @Test
    void flyingAndBankingTiltTheAuraAndSparksWithTheBody() {
        var body = new Matrix4f().rotationYXZ(0.9f, -1.2f, 0.6f).scale(0.7f, 1.1f, 0.9f);
        var expected = body.transformDirection(new Vector3f(0, 1, 0)).normalize();
        var pose = AuraPose.fromTransform(body);
        body.identity();
        for (var camera : new Quaternionf[]{new Quaternionf(), new Quaternionf().rotationYXZ(-0.4f, 0.8f, 0)}) {
            assertDirection(expected, pose.billboard(camera).transformDirection(new Vector3f(0, 1, 0)));
            assertDirection(expected, pose.rotation().transform(new Vector3f(0, 1, 0)));
        }
    }

    @Test
    void fastFlightRollIsAppliedAroundThePlayersModelPivot() {
        var pose = new AuraPose(new Vector3f(0, 0.5f, 0), new Vector3f(0, 1, 0));
        var flight = new Matrix4f().translate(0, 0.5f, 0)
                .rotateX((float) Math.toRadians(-35))
                .rotateZ((float) Math.toRadians(50))
                .translate(0, -0.5f, 0);

        var transformed = pose.transformedBy(flight);

        assertEquals(0, transformed.offset().x(), 0.0001);
        assertEquals(0.5, transformed.offset().y(), 0.0001);
        assertEquals(0, transformed.offset().z(), 0.0001);
        assertDirection(flight.transformDirection(new Vector3f(0, 1, 0)).normalize(), transformed.up());
    }

    @Test
    void captureIncludesAnimatedRootAndWaistButRemovesTheOuterRenderTransform() {
        var rootChildren = new GeoBone[1];
        var root = bone(null, "root", rootChildren, 0);
        var waistChildren = new GeoBone[1];
        var waist = bone(root, "waist", waistChildren, 12);
        rootChildren[0] = waist;
        var body = bone(waist, "body", new GeoBone[0], 24);
        waistChildren[0] = body;
        var model = new BakedGeoModel(new GeoBone[]{root}, Map.of(), null);
        var input = new PoseStack();
        input.translate(1000, 100, -500);
        input.mulPose(new Quaternionf().rotationX(0.3f));
        var pass = new Pass(model, input);
        input.mulPose(new Quaternionf().rotationY(0.8f));
        input.scale(0.8f, 0.8f, 0.8f);
        pass.addBoneUpdater((info, bones) -> {
            bones.get(root).setRotation(1.0f, 0, 0);
            bones.get(waist).setRotation(0.3f, 0, 0);
        });
        var pose = AuraPose.capture(pass);
        var expected = new Quaternionf().rotationY(0.8f).rotateX(1.3f).transform(new Vector3f(0, 1, 0));
        assertDirection(expected, pose.up());
        assertTrue(pose.offset().length() < 5, "Camera/player translation must not be applied a second time");
        assertNull(root.frameSnapshot);
        assertNull(waist.frameSnapshot);
    }

    @Test
    void capturingInsideAPosedPassKeepsThePlayerSnapshots() {
        var body = bone(null, "body", new GeoBone[0], 24);
        var pass = new Pass(new BakedGeoModel(new GeoBone[]{body}, Map.of(), null), new PoseStack());
        pass.addBoneUpdater((info, bones) -> bones.get(body).setRotation(0.8f, 0, 0));
        pass.renderPosed(() -> {
            var before = body.frameSnapshot;
            var pose = AuraPose.capture(pass);
            assertSame(before, body.frameSnapshot);
            assertDirection(new Quaternionf().rotationX(0.8f).transform(new Vector3f(0, 1, 0)), pose.up());
        });
        assertNull(body.frameSnapshot);
    }

    @Test
    void endOnCrossSectionTracksTheBodyAxis() {
        var upright = AuraPose.upright();
        var flying = AuraPose.fromTransform(new Matrix4f().rotationX((float)Math.PI / 2));
        var levelCamera = new Quaternionf();
        var overheadCamera = new Quaternionf().rotationX((float)Math.PI / 2);
        assertEquals(0, upright.endOnAmount(levelCamera), 0.001);
        assertEquals(1, flying.endOnAmount(levelCamera), 0.001);
        assertEquals(1, upright.endOnAmount(overheadCamera), 0.001);
        assertEquals(0, flying.endOnAmount(overheadCamera), 0.001);
        assertTrue(flying.billboard(levelCamera).isFinite());
    }

    private static void assertDirection(Vector3fc expected, Vector3fc actual) {
        assertEquals(expected.x(), actual.x(), 0.0001);
        assertEquals(expected.y(), actual.y(), 0.0001);
        assertEquals(expected.z(), actual.z(), 0.0001);
    }

    private static GeoBone bone(GeoBone parent, String name, GeoBone[] children, float pivotY) {
        return new GeoBone(parent, name, children, new GeoLocator[0], 0, pivotY, 0, 0, 0, 0) {
            @Override
            public <R extends GeoRenderState> void render(RenderPassInfo<R> info, PoseStack pose,
                    VertexConsumer vertices, int light, int overlay, int color) {}
        };
    }

    private static class Pass extends RenderPassInfo<GeoRenderState> {
        Pass(BakedGeoModel model, PoseStack pose) {
            super(null, new GeoRenderState.Impl(), pose, model, new CameraRenderState(), true);
        }
    }
}
