package com.dragonminez.client.render.util;

import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoLocator;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.junit.jupiter.api.Test;
import org.joml.Matrix4f;
import com.mojang.math.Axis;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GeoAttachmentRendererTest {
    @Test
    void recursiveWeaponDrawKeepsEachQueuedPoseAndScabbardVisibilityIndependent() {
        var draws = new ArrayList<Draw>();
        var sourceBone = new ProbeBone(null, "body", new GeoBone[0], draws);
        var sourceModel = new BakedGeoModel(new GeoBone[]{sourceBone}, Map.of(), null);
        var children = new GeoBone[2];
        var anchor = new ProbeBone(null, "weapon_anchor", children, draws);
        children[0] = new ProbeBone(anchor, "blade", new GeoBone[0], draws);
        children[1] = new ProbeBone(anchor, "scabbard", new GeoBone[0], draws);
        var queued = new ArrayList<Runnable>();
        var collector = collector(queued);
        var pose = new PoseStack();
        pose.translate(3, 0, 0);
        var first = new Pass(sourceModel, sourceBone, 16);
        var second = new Pass(sourceModel, sourceBone, 48);
        var sources = Map.of("weapon_anchor", "body");

        GeoAttachmentRenderer.submit(first, pose, collector, anchor, null, 1, 2, -1, sources, Set.of("blade"));
        GeoAttachmentRenderer.submit(second, pose, collector, anchor, null, 1, 2, -1, sources, Set.of());
        assertTrue(draws.isEmpty());
        pose.translate(1000, 0, 0);
        queued.forEach(Runnable::run);

        assertEquals(List.of("scabbard", "blade", "scabbard"), draws.stream().map(Draw::name).toList());
        assertEquals(2, draws.get(0).x(), 0.0001);
        assertEquals(0, draws.get(1).x(), 0.0001);
        assertEquals(0, draws.get(2).x(), 0.0001);
        assertNull(anchor.frameSnapshot);
        assertNull(sourceBone.frameSnapshot);
    }

    @Test
    void capturingAnAttachmentInsideAPosedPassDoesNotClearItsPlayerPose() {
        var draws = new ArrayList<Draw>();
        var source = new ProbeBone(null, "body", new GeoBone[0], draws);
        var model = new BakedGeoModel(new GeoBone[]{source}, Map.of(), null);
        var pass = new Pass(model, source, 32);
        var target = new ProbeBone(null, "body", new GeoBone[0], draws);
        boolean[] kept = {false};
        pass.renderPosed(() -> {
            var before = source.frameSnapshot;
            GeoAttachmentRenderer.submit(pass, new PoseStack(), collector(new ArrayList<>()), target, null, 1, 2, -1);
            kept[0] = before != null && source.frameSnapshot == before;
        });
        assertTrue(kept[0], "Capturing an accessory cannot clean up the outer player render pass");
    }

    @Test
    void attachedWeaponKeepsThePerBonePoseWithoutApplyingTheArmTwice() {
        var draws = new ArrayList<Draw>();
        var sourceBone = new ProbeBone(null, "body", new GeoBone[0], draws);
        var sourceModel = new BakedGeoModel(new GeoBone[]{sourceBone}, Map.of(), null);
        var weapon = new ProbeBone(null, "weapon", new GeoBone[0], draws);
        var queued = new ArrayList<Runnable>();
        var pose = new PoseStack();
        pose.translate(3, 0, 0);
        var local = new Matrix4f().translation(0.5f, 0.25f, 0);

        GeoAttachmentRenderer.submitAttached(new Pass(sourceModel, sourceBone, 0), pose,
                collector(queued), weapon, null, 1, 2, -1, 1, local, Set.of());
        pose.translate(100, 0, 0);
        local.translation(100, 100, 100);
        queued.forEach(Runnable::run);

        assertEquals(1, draws.size());
        assertEquals(3.5f, draws.getFirst().x(), 0.0001f);
        assertEquals(0.25f, draws.getFirst().pose().m31(), 0.0001f);
    }

    @Test
    void stowedWeaponFollowsTorsoPivotAndIgnoresChangingHandPose() {
        var draws = new ArrayList<Draw>();
        var arms = new GeoBone[1];
        var torso = new ProbeBone(null, "body", arms, draws, 0, 12, 0, 0.35f);
        var hands = new GeoBone[1];
        arms[0] = new ProbeBone(torso, "right_arm", hands, draws, -5, 22, 0, 0);
        hands[0] = new ProbeBone(arms[0], "right_hand_item", new GeoBone[0], draws, -6, 14, 0, 0);
        var source = new BakedGeoModel(new GeoBone[]{torso}, Map.of(), null);

        var weaponHands = new GeoBone[1];
        var weaponArm = new ProbeBone(null, "right_arm", weaponHands, draws, -5, 22, 0, 0);
        var weaponAnchors = new GeoBone[1];
        weaponHands[0] = new ProbeBone(weaponArm, "right_hand_item", weaponAnchors, draws, -6, 14, 0, 0);
        var blades = new GeoBone[1];
        var anchor = new ProbeBone(weaponHands[0], "weapon_anchor", blades, draws);
        weaponAnchors[0] = anchor;
        blades[0] = new ProbeBone(anchor, "blade", new GeoBone[0], draws);
        var queued = new ArrayList<Runnable>();
        var local = new Matrix4f().translation(0.5f, 0, 0.25f);
        var world = new PoseStack();
        world.translate(3, 0, 0);

        for (float turn : new float[]{0, (float)Math.PI / 2}) {
            var pass = new Pass(source, torso, 0);
            pass.addBoneUpdater((info, bones) -> {
                bones.get(torso).setRotation(0, 0, turn);
                bones.get(arms[0]).setRotation(1.4f - turn, 0, 0);
                bones.get(hands[0]).setRotation(0, 2.2f - turn, 0);
            });
            GeoAttachmentRenderer.submitMounted(pass, world, collector(queued), anchor, torso, null,
                    1, 2, -1, 1, local, Set.of());
        }
        local.translation(999, 999, 999);
        world.translate(999, 999, 999);
        queued.forEach(Runnable::run);
        assertEquals(2, draws.size());
        assertTrue(new Matrix4f().translation(3.5f, 0, 0.25f).equals(draws.get(0).pose(), 0.0001f),
                "The bind pose must preserve the original stowed placement");
        var expected = new PoseStack();
        expected.translate(3, 0, 0);
        torso.translateToPivotPoint(expected);
        expected.mulPose(Axis.ZP.rotation((float)Math.PI / 2));
        torso.translateAwayFromPivotPoint(expected);
        expected.translate(0.5, 0, 0.25);
        assertTrue(expected.last().pose().equals(draws.get(1).pose(), 0.0001f),
                "The blade must turn around the torso, never the hand rig pivots");
        assertNull(torso.frameSnapshot);
        assertNull(weaponArm.frameSnapshot);
    }

    @Test
    void mountingInsideAPosedPassKeepsTheOuterSnapshot() {
        var draws = new ArrayList<Draw>();
        var torso = new ProbeBone(null, "body", new GeoBone[0], draws);
        var pass = new Pass(new BakedGeoModel(new GeoBone[]{torso}, Map.of(), null), torso, 16);
        var target = new ProbeBone(null, "weapon_anchor", new GeoBone[0], draws);
        pass.renderPosed(() -> {
            var before = torso.frameSnapshot;
            GeoAttachmentRenderer.submitMounted(pass, new PoseStack(), collector(new ArrayList<>()),
                    target, torso, null, 1, 2, -1, 1, new Matrix4f(), Set.of());
            assertSame(before, torso.frameSnapshot);
        });
    }

    private static SubmitNodeCollector collector(List<Runnable> queued) {
        return (SubmitNodeCollector) Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(),
                new Class<?>[]{SubmitNodeCollector.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("submitCustomGeometry")) throw new AssertionError(method.getName());
                    var pose = new PoseStack.Pose();
                    pose.set(((PoseStack) args[0]).last());
                    var callback = (SubmitNodeCollector.CustomGeometryRenderer) args[2];
                    queued.add(() -> callback.render(pose, null));
                    return null;
                });
    }

    private record Draw(String name, float x, Matrix4f pose) {}

    private static class Pass extends RenderPassInfo<GeoRenderState> {
        Pass(BakedGeoModel model, GeoBone source, float translation) {
            super(null, new GeoRenderState.Impl(), new PoseStack(), model, new CameraRenderState(), true);
            addBoneUpdater((info, snapshots) -> snapshots.get(source).setTranslation(translation, 0, 0));
        }
    }

    private static class ProbeBone extends GeoBone {
        private final List<Draw> draws;
        ProbeBone(GeoBone parent, String name, GeoBone[] children, List<Draw> draws) {
            this(parent, name, children, draws, 0, 0, 0, 0);
        }
        ProbeBone(GeoBone parent, String name, GeoBone[] children, List<Draw> draws,
                  float px, float py, float pz, float rz) {
            super(parent, name, children, new GeoLocator[0], px, py, pz, 0, 0, rz);
            this.draws = draws;
        }
        @Override
        public <R extends GeoRenderState> void render(RenderPassInfo<R> info, PoseStack pose, VertexConsumer consumer, int light, int overlay, int color) {
            if (!name().equals("weapon_anchor") && frameSnapshot != null && !frameSnapshot.isHidden())
                draws.add(new Draw(name(), pose.last().pose().m30(), new Matrix4f(pose.last().pose())));
        }
    }
}
