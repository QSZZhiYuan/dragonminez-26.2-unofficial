package com.dragonminez.client.render.util;

/**
 * Cached quad geometry for aura effects.
 *
 * <h2>26.2 migration note — temporarily non-caching</h2>
 * The original implementation baked the billboard / ground quads into static
 * {@code com.mojang.blaze3d.vertex.VertexBuffer} objects and re-drew them each frame. That class
 * was removed in 26.2; its replacement, {@code StagedVertexBuffer}, works in terms of
 * {@code appendDraw(...)} + {@code upload()} + {@code ExecuteInfo}, which is a different rendering
 * model (a staged, deferred submission rather than an immediately reusable GPU buffer). Migrating
 * to it means rewriting every caller's draw path, not just this factory.
 *
 * <p>Until that happens these helpers describe their quads as plain vertex data and let the caller
 * emit them through the {@code SubmitNodeCollector}/{@code VertexConsumer} pipeline. The public
 * shape below is intentionally minimal so callers can be switched over incrementally.
 */
public final class AuraMeshFactory {

    /** UV column count in the aura spritesheet. */
    public static final int FRAME_COUNT = 4;

    private AuraMeshFactory() {
    }

    /** Billboard quad: unit square in XY, UVs (0,1)-(1,0). */
    public static float[][] billboardQuad() {
        return new float[][]{
                {-1.0f, -1.0f, 0.0f, 0.0f, 1.0f},
                {1.0f, -1.0f, 0.0f, 1.0f, 1.0f},
                {1.0f, 1.0f, 0.0f, 1.0f, 0.0f},
                {-1.0f, 1.0f, 0.0f, 0.0f, 0.0f}
        };
    }

    /** Ground quad: unit square in XZ at y=0. */
    public static float[][] groundQuad() {
        return new float[][]{
                {-1.0f, 0.0f, -1.0f, 0.0f, 0.0f},
                {-1.0f, 0.0f, 1.0f, 0.0f, 1.0f},
                {1.0f, 0.0f, 1.0f, 1.0f, 1.0f},
                {1.0f, 0.0f, -1.0f, 1.0f, 0.0f}
        };
    }

    /** Billboard quad with UVs sliced to one column of the 1x4 spritesheet. */
    public static float[][] billboardQuadFrame(int frame) {
        int f = ((frame % FRAME_COUNT) + FRAME_COUNT) % FRAME_COUNT;
        float u0 = f / (float) FRAME_COUNT;
        float u1 = (f + 1) / (float) FRAME_COUNT;
        return new float[][]{
                {-1.0f, -1.0f, 0.0f, u0, 1.0f},
                {1.0f, -1.0f, 0.0f, u1, 1.0f},
                {1.0f, 1.0f, 0.0f, u1, 0.0f},
                {-1.0f, 1.0f, 0.0f, u0, 0.0f}
        };
    }

    /** Ground quad with UVs sliced to one column of the 1x4 spritesheet. */
    public static float[][] groundQuadFrame(int frame) {
        int f = ((frame % FRAME_COUNT) + FRAME_COUNT) % FRAME_COUNT;
        float u0 = f / (float) FRAME_COUNT;
        float u1 = (f + 1) / (float) FRAME_COUNT;
        return new float[][]{
                {-1.0f, 0.0f, -1.0f, u0, 0.0f},
                {-1.0f, 0.0f, 1.0f, u0, 1.0f},
                {1.0f, 0.0f, 1.0f, u1, 1.0f},
                {1.0f, 0.0f, -1.0f, u1, 0.0f}
        };
    }
}
