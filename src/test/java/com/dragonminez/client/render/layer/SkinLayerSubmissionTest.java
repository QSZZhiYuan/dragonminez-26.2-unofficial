package com.dragonminez.client.render.layer;

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

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SkinLayerSubmissionTest {
    @Test
    void deferredSkinDrawKeepsItsCapturedPoseTintAndLight() {
        List<Draw> draws = new ArrayList<>();
        ProbeBone head = new ProbeBone(null, "head", new GeoBone[0], draws);
        var model = new BakedGeoModel(new GeoBone[]{head}, Map.of(), null);
        var info = new TestPass(model);
        info.poseStack().translate(3, 4, 5);
        List<Queued> queued = new ArrayList<>();

        DMZSkinLayer.submitSkinGeometry(info, collector(queued), null, 123, 456, 0x704080C0, 1);
        assertTrue(draws.isEmpty(), "Submission must leave drawing to the deferred callback");
        info.poseStack().translate(1000, 1000, 1000);
        queued.getFirst().render();

        assertEquals(1, draws.size());
        Draw draw = draws.getFirst();
        assertEquals(0x704080C0, draw.color());
        assertEquals(123, draw.light());
        assertEquals(456, draw.overlay());
        assertEquals(3, draw.x(), 0.0001f);
        assertEquals(1003, info.poseStack().last().pose().m30(), 0.0001f,
                "Drawing must restore the shared pose");
    }

    @Test
    void headOverlaysKeepBodyArmorAndInflationIsolatedAcrossDeferredPasses() {
        List<Draw> draws = new ArrayList<>();
        GeoBone[] children = new GeoBone[3];
        ProbeBone root = new ProbeBone(null, "root", children, draws);
        ProbeBone head = new ProbeBone(root, "head", new GeoBone[0], draws);
        ProbeBone body = new ProbeBone(root, "body", new GeoBone[0], draws);
        ProbeBone armor = new ProbeBone(root, "armorBody", new GeoBone[0], draws);
        children[0] = head; children[1] = body; children[2] = armor;
        var model = new BakedGeoModel(new GeoBone[]{root}, Map.of(), null);
        var bodyPass = new TestPass(model);
        var headPass = new TestPass(model);
        bodyPass.addBoneUpdater((info, snapshots) -> DMZSkinLayer.configureSkinBones(info, snapshots, false, 0));
        headPass.addBoneUpdater((info, snapshots) -> DMZSkinLayer.configureSkinBones(info, snapshots, true, 0.006f));
        List<Queued> queued = new ArrayList<>();
        var collector = collector(queued);

        DMZSkinLayer.submitSkinGeometry(bodyPass, collector, null, 1, 2, 0xFF112233, 1);
        DMZSkinLayer.submitSkinGeometry(headPass, collector, null, 1, 2, 0x80112233, 1);
        DMZSkinLayer.submitSkinGeometry(bodyPass, collector, null, 1, 2, 0xFF445566, 1);
        queued.forEach(Queued::render);

        assertEquals(List.of("head", "body", "head", "head", "body"),
                draws.stream().map(Draw::bone).toList());
        assertEquals(1, draws.get(0).scaleX(), 0.0001f);
        assertEquals(1.006f, draws.get(2).scaleX(), 0.0001f);
        assertEquals(1, draws.get(3).scaleX(), 0.0001f);
        assertNull(head.frameSnapshot);
        assertNull(body.frameSnapshot);
        assertNull(armor.frameSnapshot);
    }

    private static SubmitNodeCollector collector(List<Queued> queued) {
        return (SubmitNodeCollector) Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(),
                new Class<?>[]{SubmitNodeCollector.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("submitCustomGeometry")) {
                        throw new AssertionError("Unexpected rendering operation: " + method.getName());
                    }
                    PoseStack.Pose pose = new PoseStack.Pose();
                    pose.set(((PoseStack) args[0]).last());
                    queued.add(new Queued(pose, (SubmitNodeCollector.CustomGeometryRenderer) args[2]));
                    return null;
                });
    }

    private record Queued(PoseStack.Pose pose, SubmitNodeCollector.CustomGeometryRenderer renderer) {
        void render() { renderer.render(pose, null); }
    }

    private record Draw(String bone, int color, int light, int overlay, float x, float scaleX) {}

    private static final class TestPass extends RenderPassInfo<GeoRenderState> {
        TestPass(BakedGeoModel model) {
            super(null, new GeoRenderState.Impl(), new PoseStack(), model, new CameraRenderState(), true);
        }
    }

    private static final class ProbeBone extends GeoBone {
        private final List<Draw> draws;

        ProbeBone(GeoBone parent, String name, GeoBone[] children, List<Draw> draws) {
            super(parent, name, children, new GeoLocator[0], 0, 0, 0, 0, 0, 0);
            this.draws = draws;
        }

        @Override
        public <R extends GeoRenderState> void render(RenderPassInfo<R> info, PoseStack pose,
                VertexConsumer vertices, int light, int overlay, int color) {
            if (name().equals("root") || frameSnapshot != null && frameSnapshot.isHidden()) return;
            draws.add(new Draw(name(), color, light, overlay, pose.last().pose().m30(),
                    frameSnapshot == null ? 1 : frameSnapshot.getScaleX()));
        }
    }
}
