package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.events.LockOnEvent;
import com.dragonminez.client.render.DMZRendererCache;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class AfterimageEffect {
    private static final List<Entry> ACTIVE = new ArrayList<>();
    private static boolean rendering;

    private static final class Entry {
        final int entityId, totalTicks;
        final Vec3[] positions;
        final float[] yaws;
        int left;
        Entry(int entityId, int ticks, Vec3[] positions, float[] yaws) {
            this.entityId = entityId;
            this.totalTicks = this.left = ticks;
            this.positions = positions.clone();
            this.yaws = yaws.clone();
        }
    }

    public static boolean isRendering() { return rendering; }

    public static void start(int id, int ticks, Vec3[] positions, float[] yaws, boolean breakLockOn) {
        if (breakLockOn) {
            var locked = LockOnEvent.getLockedTarget();
            if (locked != null && locked.getId() == id) LockOnEvent.unlock();
        }
        if (ticks <= 0 || positions == null || yaws == null || positions.length == 0 || positions.length != yaws.length) return;
        if (ACTIVE.stream().filter(e -> e.entityId == id).count() >= 8) {
            for (var iterator = ACTIVE.iterator(); iterator.hasNext();) {
                if (iterator.next().entityId == id) { iterator.remove(); break; }
            }
        }
        if (ACTIVE.size() >= 256) ACTIVE.removeFirst();
        ACTIVE.add(new Entry(id, ticks, positions, yaws));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Pre event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) { ACTIVE.clear(); return; }
        if (!mc.isPaused()) ACTIVE.removeIf(e -> --e.left <= 0);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void submit(PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera, float partialTick) {
        var mc = Minecraft.getInstance();
        Vector3f left = new Vector3f(1, 0, 0).rotate(camera.orientation);
        for (Entry entry : ACTIVE) {
            if (!(mc.level.getEntity(entry.entityId) instanceof LivingEntity entity) || !entity.isAlive() || entity.isSpectator()) continue;
            EntityRenderer renderer = entity instanceof AbstractClientPlayer player ? DMZRendererCache.getTPRenderer(player)
                    : mc.getEntityRenderDispatcher().getRenderer(entity);
            if (renderer == null) continue;
            float progress = Mth.clamp((entry.totalTicks - entry.left + partialTick) / entry.totalTicks, 0, 1);
            float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
            rendering = true;
            try {
                EntityRenderState state = renderer.createRenderState(entity, partialTick);
                for (int i = 0; i < entry.positions.length; i++) {
                    Vec3 position = entry.positions[i];
                    if (position.distanceToSqr(camera.pos) > 96 * 96) continue;
                    for (int band = 0; band < 3; band++) {
                        float shift = band == 0 ? 0 : (band == 1 ? 1 : -1) * Mth.lerp(progress, 0.05f, 0.55f);
                        pose.pushPose();
                        pose.translate(position.x - camera.pos.x + left.x * shift,
                                position.y - camera.pos.y + left.y * shift, position.z - camera.pos.z + left.z * shift);
                        pose.mulPose(Axis.YP.rotationDegrees(bodyYaw - entry.yaws[i]));
                        renderer.submit(state, pose, wrap(collector, Math.round(progress * 1000), band, SubmitNodeCollector.class), camera);
                        pose.popPose();
                    }
                }
            } finally { rendering = false; }
        }
    }

    private static <T> T wrap(T delegate, int progress, int band, Class<T> api) {
        return api.cast(Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[]{api}, (ignored, method, original) -> {
            try {
                if (method.getName().equals("order")) {
                    Object ordered = method.invoke(delegate, original);
                    return wrap((OrderedSubmitNodeCollector) ordered, progress, band, OrderedSubmitNodeCollector.class);
                }
                String name = method.getName();
                if (!name.equals("submitModel") && !name.equals("submitModelPart") && !name.equals("submitCustomGeometry")) return null;
                Object[] args = original.clone();
                boolean mapped = false;
                for (int i = 0; i < args.length; i++) if (args[i] instanceof RenderType type) {
                    RenderType ghost = ModRenderTypes.afterimage(type);
                    if (ghost == null) return null;
                    args[i] = ghost;
                    mapped = true;
                }
                if (!mapped) return null;
                if (name.equals("submitCustomGeometry")) {
                    var draw = (SubmitNodeCollector.CustomGeometryRenderer) args[2];
                    args[2] = (SubmitNodeCollector.CustomGeometryRenderer) (pose, vertices) -> draw.render(pose, new GhostConsumer(vertices, progress, band));
                } else args[name.equals("submitModel") ? 5 : 4] = progress | (band << 16);
                return method.invoke(delegate, args);
            } catch (InvocationTargetException exception) { throw exception.getCause(); }
        }));
    }

    private record GhostConsumer(VertexConsumer delegate, int progress, int band) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x, float y, float z) { delegate.addVertex(x,y,z); delegate.setUv1(progress,band); return this; }
        @Override public VertexConsumer setColor(int r,int g,int b,int a) { delegate.setColor(r,g,b,a); return this; }
        @Override public VertexConsumer setColor(int c) { delegate.setColor(c); return this; }
        @Override public VertexConsumer setUv(float u,float v) { delegate.setUv(u,v); return this; }
        @Override public VertexConsumer setUv1(int u,int v) { delegate.setUv1(progress,band); return this; }
        @Override public VertexConsumer setUv2(int u,int v) { delegate.setUv2(u,v); return this; }
        @Override public VertexConsumer setNormal(float x,float y,float z) { delegate.setNormal(x,y,z); return this; }
        @Override public VertexConsumer setLineWidth(float w) { delegate.setLineWidth(w); return this; }
    }
}
