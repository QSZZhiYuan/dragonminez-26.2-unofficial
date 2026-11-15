package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.GeoKiRenderState;
import com.dragonminez.common.init.MainEffects;
import com.geckolib.cache.GeckoLibResources;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class CandyEffect {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/candy");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/candy.png");
    private static final ContextKey<CandyState> CANDY = new ContextKey<>(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "candy_entity"));
    private static final Map<Integer, Long> CANDIED = new HashMap<>();
    private static final List<Bolt> BOLTS = new ArrayList<>();
    private record CandyState(float scale, float yaw) {}
    private static final class Bolt {
        final int caster, target, lifetime;
        int age;
        Bolt(int caster, int target, int lifetime) { this.caster = caster; this.target = target; this.lifetime = lifetime; }
    }

    public static void apply(int caster, int target, int candyTicks, int boltTicks) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (candyTicks > 0) CANDIED.put(target, mc.level.getGameTime() + candyTicks);
        if (caster >= 0 && boltTicks > 0) BOLTS.add(new Bolt(caster, target, boltTicks));
    }

    public static boolean isCandy(LivingEntity entity) {
        if (entity == null) return false;
        if (entity.hasEffect(MainEffects.CANDY)) return true;
        return CANDIED.getOrDefault(entity.getId(), 0L) > entity.level().getGameTime();
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Pre event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) { CANDIED.clear(); BOLTS.clear(); return; }
        if (mc.isPaused()) return;
        long now = mc.level.getGameTime();
        CANDIED.values().removeIf(until -> until <= now);
        BOLTS.removeIf(bolt -> ++bolt.age >= bolt.lifetime);
    }

    public static void extract(Entity entity, EntityRenderState state, float partialTick) {
        state.setRenderData(CANDY, entity instanceof LivingEntity living && !(entity instanceof Player) && isCandy(living)
                ? new CandyState(Math.max(0.6f, living.getBbHeight() / 1.8f), Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot)) : null);
    }

    public static boolean submitInstead(EntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        CandyState candy = state.getRenderData(CANDY);
        if (candy == null) return false;
        BakedGeoModel model = GeckoLibResources.getBakedModels().getModel(MODEL);
        if (model == null || model.isMissingno()) return false;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180 - candy.yaw));
        pose.scale(candy.scale, candy.scale, candy.scale);
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TEXTURE), (drawPose, vertices) -> {
            PoseStack captured = new PoseStack(); captured.last().set(drawPose);
            model.render(new StaticCandyPass(captured, model, camera), vertices, state.lightCoords, OverlayTexture.NO_OVERLAY, -1);
        });
        pose.popPose();
        return true;
    }

    private static final class StaticCandyPass extends RenderPassInfo<GeoKiRenderState> {
        StaticCandyPass(PoseStack pose, BakedGeoModel model, CameraRenderState camera) {
            super(null, new GeoKiRenderState(), pose, model, camera, true);
        }
    }

    public static void submitBolts(PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera, float partialTick) {
        var mc = Minecraft.getInstance();
        for (Bolt bolt : BOLTS) {
            var caster = mc.level.getEntity(bolt.caster);
            var target = mc.level.getEntity(bolt.target);
            if (caster == null || target == null) continue;
            var start = caster.getEyePosition(partialTick);
            var end = target.getPosition(partialTick).add(0, target.getBbHeight() * 0.5, 0);
            var delta = end.subtract(start);
            float length = (float) delta.length();
            if (length < 0.01f) continue;
            pose.pushPose();
            pose.translate(start.x - camera.pos.x, start.y - camera.pos.y, start.z - camera.pos.z);
            pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), delta.toVector3f().normalize()));
            float age = bolt.age + partialTick;
            LightningBoltRenderer.submitColumn(pose, collector, camera, bolt.caster * 31 + bolt.target, age, length, 0.3f,
                    new float[]{1, 0.4f, 0.8f}, true, 1.8f, Mth.clamp((bolt.lifetime - age) / 4, 0, 1));
            pose.popPose();
        }
    }
}
