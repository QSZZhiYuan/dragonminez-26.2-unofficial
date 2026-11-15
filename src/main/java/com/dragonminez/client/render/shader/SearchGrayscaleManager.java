package com.dragonminez.client.render.shader;

import com.dragonminez.Reference;
import net.minecraft.resources.Identifier;

/**
 * Full-screen grayscale pass applied while the player is scanning with ki sense.
 *
 * <h2>26.2 restoration</h2>
 * 1.20.1 owned a {@code PostChain} and drove it by hand. 26.2 removed chain ownership but kept an
 * id-based selector; {@link PostEffectBridge} wraps it. The chain lives at
 * {@code assets/dragonminez/post_effect/kisense_grayscale.json}.
 *
 * <p>As with the other DMZ post effects, {@code intensity} can no longer scale a runtime uniform
 * (uniform buffers moved into the frame graph). The pass applies at full strength while scanning;
 * the parameter stays in the signature for call-site stability and for a future render-pipeline pass.
 */
public final class SearchGrayscaleManager {

    private static final Identifier EFFECT =
            Identifier.fromNamespaceAndPath(Reference.MOD_ID, "kisense_grayscale");

    private static boolean owned;

    private SearchGrayscaleManager() {}

    /** Kept for call sites. */
    public static void process(float partialTick, float intensity) {
        process(partialTick, intensity, true);
    }

    /** Applies the ki-sense grayscale post effect while scanning. */
    public static void process(float partialTick, float intensity, boolean preserveDepth) {
        if (owned) return;
        owned = PostEffectBridge.acquire(EFFECT);
    }

    /** Releases the effect unless something else took the slot. */
    public static void reset() {
        PostEffectBridge.release(EFFECT, owned);
        owned = false;
    }

    /** True when the grayscale effect is currently bound. */
    public static boolean isActive() {
        return PostEffectBridge.isBound(EFFECT);
    }
}
