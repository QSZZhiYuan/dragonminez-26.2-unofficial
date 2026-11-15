package com.dragonminez.client.render.shader;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.client.render.EntityPreviewRenderContext;
import com.dragonminez.client.render.util.PlayerEffectQueue.KiRenderTask;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.List;

/** Replays each visible ki mesh into the emission target before bloom composition. */
public final class KiBloomRenderer {
    private static final List<EffectGeometry> GEOMETRY = new ArrayList<>();
    private static boolean failed;
    private KiBloomRenderer() {}
    public static void beginFrame() { GEOMETRY.clear(); }
    public static void capture(PoseStack pose, RenderType type, SubmitNodeCollector.CustomGeometryRenderer draw) {
        if (failed || EntityPreviewRenderContext.isRendering()) return;
        String path = type.pipeline().getLocation().getPath();
        String name = type.toString();
        if ((type.pipeline().getLocation().getNamespace().equals("dragonminez") && (path.startsWith("ki_") || path.startsWith("lightning_bolt")))
                || name.startsWith("RenderType[dmz_ki_") || name.startsWith("RenderType[dmz_glow_ki")) {
            GEOMETRY.add(new EffectGeometry(pose, type, draw));
        }
    }
    public static void flush() {
        if (failed || GEOMETRY.isEmpty()) return;
        try {
            var target = EffectBloomRenderer.captureTarget();
            if (target == null) return;
            for (var geometry : GEOMETRY) geometry.draw(target, null, null);
            EffectBloomRenderer.markDrawn();
        } catch (Exception exception) {
            failed = true;
            LogUtil.warn(Env.CLIENT, "Skipping ki bloom capture until resource reload: {}", exception.toString());
        } finally { GEOMETRY.clear(); }
    }
    public static void render(List<KiRenderTask> tasks, PoseStack poseStack, Matrix4f projectionMatrix, float partialTick) {}
    public static void reset() { GEOMETRY.clear(); failed = false; }
}
