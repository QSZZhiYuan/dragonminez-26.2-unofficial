package com.dragonminez.client.render.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.util.List;

/**
 * Pre-baked unit meshes for the ki attack effects.
 *
 * <h2>26.2 migration</h2>
 * These factories used to build a {@code com.mojang.blaze3d.vertex.VertexBuffer} once and let
 * callers re-draw it with {@code bind()/draw()/unbind()}. That class was removed in 26.2; its
 * replacement ({@code StagedVertexBuffer}) is a staged, deferred submission API rather than a
 * reusable GPU buffer, so the bind/draw model does not carry over.
 *
 * <p>Instead each mesh is described once as plain vertex data ({@link Vertex}) and replayed into
 * whatever {@link VertexConsumer} the frame's {@code SubmitNodeCollector} hands us — see
 * {@link ImmediateGeometry#emit}. The meshes are tiny (32×16 sphere, 32-segment cylinder), so
 * replaying is cheaper than the old GPU-side caching it replaces.
 */
public final class KiMeshFactory {

    /** One vertex of a pre-baked mesh. */
    public record Vertex(
            float x, float y, float z,
            float u, float v,
            float nx, float ny, float nz,
            int color
    ) {
        static final int WHITE = 0xFFFFFFFF;

        void emit(PoseStack.Pose pose, VertexConsumer consumer) {
            consumer.addVertex(pose, x, y, z)
                    .setColor(color)
                    .setUv(u, v)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(15728880)
                    .setNormal(pose, nx, ny, nz);
        }
    }

    private static List<Vertex> sphereMesh;
    private static List<Vertex> cylinderMesh;

    private KiMeshFactory() {
    }

    /** Unit sphere (32 segments × 16 rings), UV-mapped, quoted for the NEW_ENTITY pipeline. */
    public static List<Vertex> sphereMesh() {
        if (sphereMesh == null) {
            var out = new java.util.ArrayList<Vertex>(32 * 16 * 4);
            int segments = 32;
            int rings = 16;

            for (int i = 0; i < rings; i++) {
                float theta1 = (float) (Math.PI * i / rings);
                float theta2 = (float) (Math.PI * (i + 1) / rings);

                for (int j = 0; j < segments; j++) {
                    float phi1 = (float) (2.0 * Math.PI * j / segments);
                    float phi2 = (float) (2.0 * Math.PI * (j + 1) / segments);
                    float u1 = (float) j / segments;
                    float u2 = (float) (j + 1) / segments;
                    float v1 = (float) i / rings;
                    float v2 = (float) (i + 1) / rings;

                    float x1 = (float) (Math.sin(theta1) * Math.cos(phi1));
                    float y1 = (float) Math.cos(theta1);
                    float z1 = (float) (Math.sin(theta1) * Math.sin(phi1));

                    float x2 = (float) (Math.sin(theta1) * Math.cos(phi2));
                    float y2 = (float) Math.cos(theta1);
                    float z2 = (float) (Math.sin(theta1) * Math.sin(phi2));

                    float x3 = (float) (Math.sin(theta2) * Math.cos(phi2));
                    float y3 = (float) Math.cos(theta2);
                    float z3 = (float) (Math.sin(theta2) * Math.sin(phi2));

                    float x4 = (float) (Math.sin(theta2) * Math.cos(phi1));
                    float y4 = (float) Math.cos(theta2);
                    float z4 = (float) (Math.sin(theta2) * Math.sin(phi1));

                    out.add(new Vertex(x1, y1, z1, u1, v1, x1, y1, z1, Vertex.WHITE));
                    out.add(new Vertex(x2, y2, z2, u2, v1, x2, y2, z2, Vertex.WHITE));
                    out.add(new Vertex(x3, y3, z3, u2, v2, x3, y3, z3, Vertex.WHITE));
                    out.add(new Vertex(x4, y4, z4, u1, v2, x4, y4, z4, Vertex.WHITE));
                }
            }
            sphereMesh = List.copyOf(out);
        }
        return sphereMesh;
    }

    /** Unit cylinder (32 segments), UV-mapped, quoted for the NEW_ENTITY pipeline. */
    public static List<Vertex> cylinderMesh() {
        if (cylinderMesh == null) {
            var out = new java.util.ArrayList<Vertex>(32 * 12);
            int segments = 32;

            for (int i = 0; i < segments; i++) {
                float theta1 = (float) (2.0 * Math.PI * i / segments);
                float theta2 = (float) (2.0 * Math.PI * (i + 1) / segments);

                float x1 = (float) Math.cos(theta1);
                float y1 = (float) Math.sin(theta1);
                float x2 = (float) Math.cos(theta2);
                float y2 = (float) Math.sin(theta2);

                float u1 = (float) i / segments;
                float u2 = (float) (i + 1) / segments;

                // Side wall
                out.add(new Vertex(x1, y1, 0, u1, 0.0f, x1, y1, 0, Vertex.WHITE));
                out.add(new Vertex(x2, y2, 0, u2, 0.0f, x2, y2, 0, Vertex.WHITE));
                out.add(new Vertex(x2, y2, 1, u2, 1.0f, x2, y2, 0, Vertex.WHITE));
                out.add(new Vertex(x1, y1, 1, u1, 1.0f, x1, y1, 0, Vertex.WHITE));

                // Near cap (z = 0)
                out.add(new Vertex(0, 0, 0, 0.5f, 0.5f, 0, 0, -1, Vertex.WHITE));
                out.add(new Vertex(x2, y2, 0, x2 * 0.5f + 0.5f, y2 * 0.5f + 0.5f, 0, 0, -1, Vertex.WHITE));
                out.add(new Vertex(x1, y1, 0, x1 * 0.5f + 0.5f, y1 * 0.5f + 0.5f, 0, 0, -1, Vertex.WHITE));
                out.add(new Vertex(0, 0, 0, 0.5f, 0.5f, 0, 0, -1, Vertex.WHITE));

                // Far cap (z = 1)
                out.add(new Vertex(0, 0, 1, 0.5f, 0.5f, 0, 0, 1, Vertex.WHITE));
                out.add(new Vertex(x1, y1, 1, x1 * 0.5f + 0.5f, y1 * 0.5f + 0.5f, 0, 0, 1, Vertex.WHITE));
                out.add(new Vertex(x2, y2, 1, x2 * 0.5f + 0.5f, y2 * 0.5f + 0.5f, 0, 0, 1, Vertex.WHITE));
                out.add(new Vertex(0, 0, 1, 0.5f, 0.5f, 0, 0, 1, Vertex.WHITE));
            }
            cylinderMesh = List.copyOf(out);
        }
        return cylinderMesh;
    }

    /** Replays a pre-baked mesh into {@code consumer}. */
    public static void emit(List<Vertex> mesh, PoseStack.Pose pose, VertexConsumer consumer) {
        for (Vertex v : mesh) {
            v.emit(pose, consumer);
        }
    }

    /**
     * Replays a pre-baked mesh with every vertex colour multiplied by {@code tintArgb}.
     *
     * <p>26.2 has no way to set a per-draw uniform from inside
     * {@code SubmitNodeCollector#submitCustomGeometry} — the callback only receives a
     * {@link VertexConsumer} (see {@code CustomFeatureRenderer#buildGroup}). The effect colours that
     * the 1.21 code pushed through {@code shader.safeGetUniform(...)} therefore have to ride on the
     * vertex data instead, which is what this overload is for. Use {@link net.minecraft.util.ARGB#colorFromFloat}
     * to build the tint from the entity's 0..1 rgb + alpha.
     */
    public static void emit(List<Vertex> mesh, PoseStack.Pose pose, VertexConsumer consumer, int tintArgb) {
        for (Vertex v : mesh) {
            consumer.addVertex(pose, v.x(), v.y(), v.z())
                    .setColor(net.minecraft.util.ARGB.multiply(v.color(), tintArgb))
                    .setUv(v.u(), v.v())
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(15728880)
                    .setNormal(pose, v.nx(), v.ny(), v.nz());
        }
    }

    /**
     * Wave shader attributes: Color carries core/alpha, UV0 carries unit-local x/y,
     * and the unused overlay/light attributes carry the border/outline palettes.
     * Their spare high bytes preserve unit-local z after the pose is baked on the CPU.
     * This keeps every queued wave independent without unbound per-draw uniforms.
     */
    public static void emitKiWave(List<Vertex> mesh, PoseStack.Pose pose, VertexConsumer consumer,
                                  int core, int border, int outline) {
        for (Vertex v : mesh) {
            emitKiWaveVertex(v, v.z(), pose, consumer, core, border, outline);
        }
    }

    /** Keep the entire shaft rim inside the ball, rather than clipping at its axial radius. */
    public static float beamEndInset(float shaftRadius, float ballRadius) {
        float intersection = (float) Math.sqrt(Math.max(0, ballRadius * ballRadius - shaftRadius * shaftRadius));
        return Math.max(0, intersection - Math.max(0.002f, shaftRadius * 0.02f));
    }

    /** Spiral vertices store tube-local coordinates in their colour, independently of their world size. */
    public static void emitKiSpiral(List<Vertex> mesh, PoseStack.Pose pose, VertexConsumer consumer,
                                    int core, int border, int outline) {
        for (Vertex v : mesh) {
            float x = ((v.color() >>> 16) & 255) / 255.0f * 2 - 1;
            float y = ((v.color() >>> 8) & 255) / 255.0f * 2 - 1;
            float z = (v.color() & 255) / 255.0f;
            emitKiVertex(v, v.z(), x, y, z, pose, consumer, core, border, outline);
        }
    }

    /** Emits only the shaft between its two end balls; hidden end caps are omitted. */
    public static void emitKiWaveCylinder(PoseStack.Pose pose, VertexConsumer consumer,
                                          int core, int border, int outline, float near, float far) {
        List<Vertex> mesh = cylinderMesh();
        for (int segment = 0; segment < mesh.size(); segment += 12) {
            for (int corner = 0; corner < 4; corner++) {
                Vertex v = mesh.get(segment + corner);
                emitKiWaveVertex(v, near + v.z() * (far - near), pose, consumer, core, border, outline);
            }
        }
    }

    private static void emitKiWaveVertex(Vertex v, float z, PoseStack.Pose pose, VertexConsumer consumer,
                                          int core, int border, int outline) {
        emitKiVertex(v, z, v.x(), v.y(), z, pose, consumer, core, border, outline);
    }

    private static void emitKiVertex(Vertex v, float positionZ, float localX, float localY, float localZValue,
                                     PoseStack.Pose pose, VertexConsumer consumer, int core, int border, int outline) {
        int localZ = Math.round(Math.clamp(localZValue * 0.5f + 0.5f, 0.0f, 1.0f) * 65535.0f);
        consumer.addVertex(pose, v.x(), v.y(), positionZ)
                .setColor(core)
                .setUv(localX, localY)
                .setOverlay((border & 0xFFFFFF) | ((localZ & 255) << 24))
                .setLight((outline & 0xFFFFFF) | ((localZ >>> 8) << 24))
                .setNormal(pose, v.nx(), v.ny(), v.nz());
    }
}
