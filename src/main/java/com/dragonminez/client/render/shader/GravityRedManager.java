package com.dragonminez.client.render.shader;

import com.dragonminez.Reference;
import net.minecraft.resources.Identifier;

/**
 * Full-screen red distortion applied while the local player stands inside an active Gravity Device
 * zone.
 *
 * <h2>26.2 restoration</h2>
 * 1.20.1 owned a {@code PostChain} and drove it by hand. 26.2 removed chain ownership entirely, but
 * kept an id-based selector — {@link PostEffectBridge} wraps it. The chain itself lives at
 * {@code assets/dragonminez/post_effect/gravity_red.json} and carries its uniforms in the JSON, so
 * nothing has to be pushed per frame.
 *
 * <p>One behavioural difference remains, and it is inherent to the new model: the {@code intensity}
 * argument can no longer scale a uniform at runtime, because uniform buffers belong to the frame
 * graph. The effect is applied at full strength for as long as the zone is active; {@code intensity}
 * is kept in the signature so call sites and future render-pipeline work do not have to change.
 */
public final class GravityRedManager {

    private static final Identifier EFFECT =
            Identifier.fromNamespaceAndPath(Reference.MOD_ID, "gravity_red");

    private static boolean owned;

    private GravityRedManager() {}

    /** Kept for call sites. */
    public static void process(float partialTick, float intensity) {
        process(partialTick, intensity, true);
    }

    /** Applies the gravity-red post effect while the zone is active. */
    public static void process(float partialTick, float intensity, boolean preserveDepth) {
        if (owned) return;
        owned = PostEffectBridge.acquire(EFFECT);
    }

    /** Releases the effect unless something else (e.g. a creeper shader) took the slot. */
    public static void reset() {
        PostEffectBridge.release(EFFECT, owned);
        owned = false;
    }

    /** True when the gravity-red effect is currently bound. */
    public static boolean isActive() {
        return PostEffectBridge.isBound(EFFECT);
    }
}
