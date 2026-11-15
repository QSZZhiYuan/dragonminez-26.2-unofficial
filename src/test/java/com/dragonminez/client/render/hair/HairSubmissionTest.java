package com.dragonminez.client.render.hair;

import com.dragonminez.client.render.util.VertexSink;
import com.dragonminez.common.hair.CustomHair;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HairSubmissionTest {
    @Test
    void queuedHairUsesItsHeadPoseAfterTheSharedStackHasChanged() {
        CustomHair hair = hair();
        PoseStack shared = new PoseStack();
        shared.translate(3, 4, 5);
        shared.scale(0.75f, 0.75f, 0.75f);
        List<Queued> queued = submit(shared, hair);
        shared.translate(100, -200, 300);
        shared.scale(10, 10, 10);

        List<float[]> positions = new ArrayList<>();
        queued.getFirst().draw().accept(queued.getFirst().pose(), vertices(positions, new ArrayList<>()));

        assertEquals(24, positions.size());
        for (float[] position : positions) {
            assertTrue(position[0] > 2.5f && position[0] < 3.5f);
            assertTrue(position[1] > 4 && position[1] < 4.75f);
            assertTrue(position[2] > 4.5f && position[2] < 5.5f);
        }
        assertEquals(78, shared.last().pose().m30(), 0.0001f);
        assertEquals(-146, shared.last().pose().m31(), 0.0001f);
    }

    @Test
    void queuedStrandsKeepIndependentColorsWhenDrawnLater() {
        List<Queued> queued = submit(new PoseStack(), hair());
        assertEquals(2, queued.size());
        List<int[]> firstColors = new ArrayList<>();
        List<int[]> secondColors = new ArrayList<>();
        queued.get(1).draw().accept(queued.get(1).pose(), vertices(new ArrayList<>(), secondColors));
        queued.get(0).draw().accept(queued.get(0).pose(), vertices(new ArrayList<>(), firstColors));
        assertEquals(24, firstColors.size());
        assertEquals(24, secondColors.size());
        firstColors.forEach(color -> assertArrayEquals(new int[]{255, 0, 0, 255}, color));
        secondColors.forEach(color -> assertArrayEquals(new int[]{0, 255, 0, 255}, color));
    }

    private static CustomHair hair() {
        CustomHair hair = new CustomHair();
        hair.getStrand(CustomHair.HairFace.TOP, 0).setLength(1);
        hair.getStrand(CustomHair.HairFace.TOP, 0).setColor("#FF0000");
        hair.getStrand(CustomHair.HairFace.TOP, 1).setLength(1);
        hair.getStrand(CustomHair.HairFace.TOP, 1).setColor("#00FF00");
        return hair;
    }

    private static List<Queued> submit(PoseStack shared, CustomHair hair) {
        List<Queued> queued = new ArrayList<>();
        VertexSink sink = (pose, type, draw) -> {
            PoseStack.Pose captured = new PoseStack.Pose();
            captured.set(pose.last());
            queued.add(new Queued(captured, draw));
        };
        float[] color = {1, 1, 1};
        HairRenderer.render(shared, sink, hair, hair, 0, null, null, null,
                color, color, false, false, 0, 123, 456, 1, 1, 0);
        return queued;
    }

    private static VertexConsumer vertices(List<float[]> positions, List<int[]> colors) {
        return (VertexConsumer) Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(),
                new Class<?>[]{VertexConsumer.class}, (proxy, method, args) -> {
                    if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
                    if (method.getName().equals("addVertex")) {
                        positions.add(new float[]{(float) args[0], (float) args[1], (float) args[2]});
                    } else if (method.getName().equals("setColor") && args.length == 4) {
                        colors.add(new int[]{(int) args[0], (int) args[1], (int) args[2], (int) args[3]});
                    }
                    return method.getReturnType() == void.class ? null : proxy;
                });
    }

    private record Queued(PoseStack.Pose pose, VertexSink.Consumer draw) {}
}
