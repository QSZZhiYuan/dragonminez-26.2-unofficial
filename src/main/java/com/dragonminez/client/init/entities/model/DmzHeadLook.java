package com.dragonminez.client.init.entities.model;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.BoneSnapshots;
import net.minecraft.util.Mth;

/**
 * GeckoLib 5 replacement for the removed {@code GeoModel#setCustomAnimations}.
 *
 * <h2>Why this exists</h2>
 * GeckoLib 4 let a model mutate bones while the animation state was being assembled, through
 * {@code setCustomAnimations(T animatable, long instanceId, AnimationState<T>)}. GeckoLib 5 deleted
 * that hook: {@link com.geckolib.model.GeoModel} no longer has it, bones are immutable, and the
 * per-frame render values live on {@code BoneSnapshot} ({@code GeoBone#frameSnapshot}), which
 * {@code AnimationProcessor#createBoneSnapshots} rebuilds before each pass.
 *
 * <p>The replacement is a two-stage hand-off, mirroring how visibility is handled:
 * <ol>
 *   <li>the renderer's {@code addRenderData(T, O, R, float)} still receives the live entity, so it
 *       stashes the look angles on the render state via {@link #HEAD_PITCH} / {@link #HEAD_YAW};</li>
 *   <li>the renderer's {@code adjustModelBonesForRender(RenderPassInfo, BoneSnapshots)} reads them
 *       back and writes the rotation onto the bone snapshot.</li>
 * </ol>
 *
 * <p>This is the same shape as {@code DmzBoneVisibility}: capture while the entity is reachable,
 * apply where the snapshot is reachable.
 */
public final class DmzHeadLook {

    /** Head pitch in degrees, captured from the entity. */
    public static final DataTicket<Float> HEAD_PITCH =
            DataTicket.create("dmz_head_pitch", Float.class);

    /** Net head yaw in degrees, captured from the entity. */
    public static final DataTicket<Float> HEAD_YAW =
            DataTicket.create("dmz_head_yaw", Float.class);
    public static final DataTicket<Boolean> HEAD_LOCKED =
            DataTicket.create("dmz_head_locked", Boolean.class);

    /** Body "boobas" bone scale factor (1.0 = default), captured from the entity. */
    public static final DataTicket<Float> BOOB_SCALE =
            DataTicket.create("dmz_boob_scale", Float.class);

    private DmzHeadLook() {}

    /**
     * Captures the head-look angles (and the body-shape scale factor) from a live entity onto the
     * render state, so {@link #apply} can read them back once the entity is gone.
     *
     * <p>The 1.20.1/GeckoLib-4 version of this logic lived in {@code DMZPlayerModel#applyMolangQueries};
     * see {@code DMZPlayerModel} for why the Molang path could not be kept.
     */
    public static <T extends net.minecraft.world.entity.LivingEntity & com.geckolib.animatable.GeoAnimatable>
    void captureFrom(com.geckolib.renderer.base.GeoRenderState renderState, T animatable, float partialTick) {
        if (renderState == null || animatable == null) return;

        // In GUI screens the O-fields (yBodyRotO, xRotO) are stale world values - the screen only
        // sets the current-tick fields. Forcing pt=1.0 makes the lerp return the current value,
        // so the O-fields are ignored and there is no jitter.
        boolean inScreen = net.minecraft.client.Minecraft.getInstance().gui.screen() != null;
        float pt = inScreen ? 1.0f : partialTick;

        float pitch = Mth.lerp(pt, animatable.xRotO, animatable.getXRot());
        float bodyYaw = Mth.rotLerp(pt, animatable.yBodyRotO, animatable.yBodyRot);
        float headYaw = Mth.rotLerp(pt, animatable.yHeadRotO, animatable.yHeadRot);

        renderState.addGeckolibData(HEAD_LOCKED, animatable instanceof com.dragonminez.client.animation.IPlayerAnimatable playerAnim
                && playerAnim.dragonminez$isPlayingKiAnimation());

        renderState.addGeckolibData(HEAD_PITCH, pitch);
        renderState.addGeckolibData(HEAD_YAW, Mth.wrapDegrees(headYaw - bodyYaw));

        if (animatable instanceof net.minecraft.client.player.AbstractClientPlayer) {
            renderState.addGeckolibData(BOOB_SCALE, boobScaleOf(animatable));
        }
    }

    /** Reads the configured body-shape scale for a player, clamped the same way the 1.20.1 code did. */
    private static <T extends net.minecraft.world.entity.LivingEntity & com.geckolib.animatable.GeoAnimatable>
    float boobScaleOf(T animatable) {
        if (!(animatable instanceof net.minecraft.client.player.AbstractClientPlayer player)) return 1.0f;
        return com.dragonminez.common.stats.StatsProvider
                .get(com.dragonminez.common.stats.StatsCapability.INSTANCE, player)
                .map(data -> {
                    var c = data.getCharacter();
                    String gender = c.getGender() != null ? c.getGender().toLowerCase() : "";
                    boolean isFemale = gender.equals(com.dragonminez.common.stats.character.Character.GENDER_FEMALE)
                            || c.getBodyType() == 1;
                    return isFemale ? Mth.clamp(c.getBoobScale(), 0.75f, 1.25f) : 1.0f;
                })
                .orElse(1.0f);
    }

    /**
     * Applies a head-look rotation to the {@code head} bone of the given snapshot set.
     *
     * <p>No-op when the model has no {@code head} bone or the look data was never captured.
     */
    public static void apply(BoneSnapshots bones, Float pitchDeg, Float yawDeg) {
        if (bones == null || pitchDeg == null || yawDeg == null) return;
        bones.ifPresent("head", snapshot -> {
            // Entity look angles and GeckoLib model rotations use opposite X/Y signs.
            // Remove inherited rotation so looking stays correct while crouching/flying.
            float parentX = 0, parentY = 0;
            for (var parent = snapshot.getBone().parent(); parent != null; parent = parent.parent()) {
                var pose = bones.get(parent.name()).orElse(null);
                if (pose != null) { parentX += pose.getRotX(); parentY += pose.getRotY(); }
            }
            snapshot.setRotX(Mth.clamp(-pitchDeg * Mth.DEG_TO_RAD - parentX, -Mth.HALF_PI, Mth.HALF_PI));
            snapshot.setRotY(-yawDeg * Mth.DEG_TO_RAD - parentY);
        });
    }
}
