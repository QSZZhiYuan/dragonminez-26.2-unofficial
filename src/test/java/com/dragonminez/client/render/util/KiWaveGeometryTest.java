package com.dragonminez.client.render.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KiWaveGeometryTest {
    @Test
    void shaftRimsOverlapTheirEndBallsForEveryWaveWidth() {
        for (float radius : new float[]{0.08f, 0.4f, 1.2f, 3, 12}) {
            for (float ballRadius : new float[]{radius * 1.25f, radius * 1.5f, radius * 2.5f}) {
                float inset = KiMeshFactory.beamEndInset(radius, ballRadius);
                assertTrue(inset * inset + radius * radius < ballRadius * ballRadius,
                        "The full end rim must overlap the ball, or a ring-shaped gap appears");
            }
        }
        assertEquals(0, KiMeshFactory.beamEndInset(2, 1));
    }

    @Test
    void cylinderCapsFaceOutwardAndBothAreComplete() {
        var mesh = KiMeshFactory.cylinderMesh();
        for (int i = 0; i < mesh.size(); i += 12) {
            for (int cap : new int[]{4, 8}) {
                var a = mesh.get(i + cap);
                var b = mesh.get(i + cap + 1);
                var c = mesh.get(i + cap + 2);
                float zCross = (b.x() - a.x()) * (c.y() - a.y())
                        - (b.y() - a.y()) * (c.x() - a.x());
                assertTrue(zCross * a.nz() > 0, "Cap winding must agree with its outward normal");
            }
        }
    }

    @Test
    void spiralKeepsTubeCoordinatesAndPaletteIndependentOfItsWorldLength() {
        var pose = new PoseStack();
        var out = new Capture();
        var mesh = KiSpiralMesh.get(1, 1, 10, 0.4f, 0.08f, 0.05f, 0.9f, true);
        KiMeshFactory.emitKiSpiral(mesh, pose.last(), out.consumer, 0xFFFFFFFF, 0xFFFFB020, 0xFFFFFF00);
        assertEquals(10, out.vertices.stream().mapToDouble(v -> v.z).max().orElseThrow(), 0.09);
        assertTrue(out.vertices.stream().allMatch(v -> v.color == 0xFFFFFFFF));
        assertTrue(out.vertices.stream().allMatch(v -> Math.abs(v.u) <= 1 && Math.abs(v.v) <= 1));
        assertTrue(out.vertices.stream().anyMatch(v -> v.z > 1));
    }

    @Test
    void bakedPosePreservesTheIndependentPaletteAndLocalCoordinates() {
        var pose = new PoseStack();
        pose.translate(3, 4, 5);
        pose.scale(2, 3, 4);
        var out = new Capture();
        KiMeshFactory.emitKiWave(List.of(new KiMeshFactory.Vertex(
                0.25f, -0.5f, -0.75f, 0, 0, 0, 0, 1, -1)),
                pose.last(), out.consumer, 0x801122CC, 0xFFCCEEFF, 0xFF11FFFF);
        Sample v = out.vertices.getFirst();
        assertEquals(3.5f, v.x, 0.0001f);
        assertEquals(2.5f, v.y, 0.0001f);
        assertEquals(2.0f, v.z, 0.0001f);
        assertEquals(0.25f, v.u, 0.0001f);
        assertEquals(-0.5f, v.v, 0.0001f);
        assertEquals(0x801122CC, v.color);
        assertEquals(0xCCEEFF, (v.overlayX & 65535) | ((v.overlayY & 255) << 16));
        assertEquals(0x11FFFF, (v.lightX & 65535) | ((v.lightY & 255) << 16));
        int localZ = ((v.overlayY >> 8) & 255) | (((v.lightY >> 8) & 255) << 8);
        assertEquals(-0.75f, localZ / 65535.0f * 2 - 1, 0.00004f);
    }

    @Test
    void shaftEndsStayInsideTheTwoBallsAfterScalingAndHaveNoExposedCaps() {
        var pose = new PoseStack();
        pose.translate(3, 4, 5);
        pose.scale(2, 2, 10);
        var out = new Capture();
        KiMeshFactory.emitKiWaveCylinder(pose.last(), out.consumer, -1, -1, -1, 0.15f, 0.75f);
        assertEquals(32 * 4, out.vertices.size());
        assertEquals(6.5, out.vertices.stream().mapToDouble(v -> v.z).min().orElseThrow(), 0.0001);
        assertEquals(12.5, out.vertices.stream().mapToDouble(v -> v.z).max().orElseThrow(), 0.0001);
        for (Sample v : out.vertices) {
            assertEquals(2, Math.hypot(v.x - 3, v.y - 4), 0.0001);
            assertEquals(0, v.nz, 0.0001f);
        }
    }

    private static final class Sample {
        float x, y, z, u, v, nz;
        int color, overlayX, overlayY, lightX, lightY;
    }

    private static final class Capture {
        final List<Sample> vertices = new ArrayList<>();
        final VertexConsumer consumer = (VertexConsumer) Proxy.newProxyInstance(
                VertexConsumer.class.getClassLoader(), new Class<?>[]{VertexConsumer.class},
                (proxy, method, args) -> {
                    if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
                    if (method.getName().equals("addVertex")) {
                        Sample v = new Sample();
                        v.x = (float) args[0]; v.y = (float) args[1]; v.z = (float) args[2];
                        vertices.add(v);
                    } else {
                        Sample v = vertices.getLast();
                        switch (method.getName()) {
                            case "setColor" -> v.color = (int) args[0];
                            case "setUv" -> { v.u = (float) args[0]; v.v = (float) args[1]; }
                            case "setUv1" -> { v.overlayX = (short) (int) args[0]; v.overlayY = (short) (int) args[1]; }
                            case "setUv2" -> { v.lightX = (short) (int) args[0]; v.lightY = (short) (int) args[1]; }
                            case "setNormal" -> v.nz = (float) args[2];
                            default -> throw new AssertionError(method.getName());
                        }
                    }
                    return proxy;
                });
    }
}
