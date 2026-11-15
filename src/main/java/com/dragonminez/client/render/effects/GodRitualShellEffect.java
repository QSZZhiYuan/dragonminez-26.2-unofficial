package com.dragonminez.client.render.effects;

import com.dragonminez.client.render.EntityPreviewRenderContext;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.GodRitualHelper;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

/** Captures ritual opacity before extraction ends, then submits a separate luminous skin pass. */
public final class GodRitualShellEffect {
    private static final ContextKey<Shell> SHELL = new ContextKey<>(Identifier.fromNamespaceAndPath("dragonminez", "ritual_shell"));
    private record Shell(int alpha, int color) {}

    private GodRitualShellEffect() {}

    public static void extract(Entity entity, EntityRenderState state, float partialTick) {
        Shell shell = null;
        if (entity instanceof Player player && !EntityPreviewRenderContext.isRendering() && !IrisCompat.isShaderPackInUse()) {
            var data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
            if (data != null) {
                float alpha = GodRitualHelper.shellAlpha(data, player.level().getGameTime() + partialTick);
                if (alpha > 0.003f) {
                    float[] rgb = GodRitualHelper.shellColor();
                    int color = (Math.round(rgb[0] * 31) << 11) | (Math.round(rgb[1] * 63) << 5) | Math.round(rgb[2] * 31);
                    shell = new Shell(Math.round(alpha * 1000), color);
                }
            }
        }
        state.setRenderData(SHELL, shell);
    }

    public static SubmitNodeCollector wrap(EntityRenderState state, SubmitNodeCollector collector) {
        Shell shell = state.getRenderData(SHELL);
        return shell == null ? collector : (SubmitNodeCollector) proxy(collector, shell, SubmitNodeCollector.class);
    }

    private static Object proxy(Object delegate, Shell shell, Class<?> api) {
        return Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[]{api}, (ignored, method, original) -> {
            try {
                if (original == null) return method.invoke(delegate, (Object[]) null);
                if (method.getName().equals("order")) {
                    Object result = method.invoke(delegate, original);
                    return result == null ? null : proxy(result, shell, OrderedSubmitNodeCollector.class);
                }
                String name = method.getName();
                if (!name.equals("submitModel") && !name.equals("submitModelPart") && !name.equals("submitCustomGeometry"))
                    return method.invoke(delegate, original);
                Object[] args = original.clone();
                boolean changed = false;
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof RenderType type) {
                        RenderType replacement = ModRenderTypes.ritualShell(type);
                        if (replacement != null) { args[i] = replacement; changed = true; }
                    }
                }
                if (!changed) return method.invoke(delegate, original);
                // The luminous shell overlays the textured player throughout its fade.
                method.invoke(delegate, original);
                if (name.equals("submitCustomGeometry")) {
                    var geometry = (SubmitNodeCollector.CustomGeometryRenderer) args[2];
                    args[2] = (SubmitNodeCollector.CustomGeometryRenderer) (pose, vertices) ->
                            geometry.render(pose, new ShellConsumer(vertices, shell.alpha(), shell.color()));
                } else if (name.equals("submitModel")) {
                    args[5] = shell.alpha() | (shell.color() << 16);
                } else {
                    args[4] = shell.alpha() | (shell.color() << 16);
                }
                method.invoke(delegate, args);
                return null;
            } catch (InvocationTargetException exception) {
                throw exception.getCause();
            }
        });
    }

    private record ShellConsumer(VertexConsumer delegate, int alpha, int color) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x, float y, float z) { delegate.addVertex(x,y,z); delegate.setUv1(alpha,color); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { delegate.setColor(r,g,b,a); return this; }
        @Override public VertexConsumer setColor(int value) { delegate.setColor(value); return this; }
        @Override public VertexConsumer setUv(float u, float v) { delegate.setUv(u,v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { delegate.setUv1(alpha,color); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { delegate.setUv2(u,v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { delegate.setNormal(x,y,z); return this; }
        @Override public VertexConsumer setLineWidth(float width) { delegate.setLineWidth(width); return this; }
    }
}
