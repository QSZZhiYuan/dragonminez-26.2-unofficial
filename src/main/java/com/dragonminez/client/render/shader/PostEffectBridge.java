package com.dragonminez.client.render.shader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.FileToIdConverter;
import org.jetbrains.annotations.Nullable;

/**
 * Shared plumbing for DMZ's full-screen post effects.
 *
 * <h2>26.2 migration</h2>
 * 1.20.1 gave every effect its own {@code PostChain} object that it built, resized, fed uniforms and
 * processed by hand. 26.2 deleted that whole surface — the {@code PostChain} constructor,
 * {@code resize}, {@code getTempTarget}, {@code PostPass#getEffect} and {@code RenderTarget#bindWrite}
 * are all gone, and {@code PostChain} is now driven by the frame graph.
 *
 * <p>What replaced it is an <em>id-based</em> selection, and the selector is public:
 *
 * <pre>{@code
 * GameRenderer#setPostEffect(Identifier)     // store the id
 * GameRenderer#clearPostEffect()             // release it
 * GameRenderer#currentPostEffect()           // read back what is bound
 * }</pre>
 *
 * <p>The render loop then resolves the id through
 * {@code Minecraft#getShaderManager().getPostChain(id, LevelTargetBundle.MAIN_TARGETS)} and processes
 * it against the main target. So a mod can still apply its own effect — it just hands over an id
 * instead of owning the chain.
 *
 * <h2>What is still not possible</h2>
 * Per-frame uniform writes ({@code PostPass#getEffect()}) have no replacement: uniform values now
 * come from the post-chain JSON and the frame graph owns the buffers. Effects that only need a static
 * uniform set (which is how DMZ's post chains are authored — see {@code assets/dragonminez/post_effect/})
 * work unchanged; anything that had to animate a uniform per frame would need a render-pipeline pass.
 */
public final class PostEffectBridge {
    private static final FileToIdConverter EFFECT_FILES = FileToIdConverter.json("post_effect");

    private PostEffectBridge() {}

    /** Missing optional effects must never reach ShaderManager's world-closing recovery path. */
    public static @Nullable PostChain getIfPresent(Identifier effect) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameRenderer == null
                || mc.getResourceManager().getResource(EFFECT_FILES.idToFile(effect)).isEmpty()) return null;
        return mc.getShaderManager().getPostChain(effect, LevelTargetBundle.MAIN_TARGETS);
    }

    /**
     * Binds {@code effect} as the active post effect, but only when nothing else meaningful is
     * already bound. Vanilla's entity shaders (creeper/spider/enderman) take the same slot, and DMZ
     * must not stomp them just because a scan or gravity zone is active.
     *
     * @return true when this call is what bound the effect
     */
    public static boolean acquire(Identifier effect) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameRenderer == null) return false;
        // setPostEffect only stores the id. A missing chain fails in a later render frame,
        // where ShaderManager starts resource recovery and can close an integrated world.
        // Check the actual 26.2 resource location before giving the id to GameRenderer.
        if (getIfPresent(effect) == null) return false;

        Identifier current = mc.gameRenderer.currentPostEffect();
        if (current != null && !current.equals(effect)) return false;

        mc.gameRenderer.setPostEffect(effect);
        return true;
    }

    /**
     * Releases {@code effect}, but only if it is still the effect in the slot — an entity shader that
     * took over while DMZ's effect was active must survive.
     *
     * @param owned whether this class bound the effect (see {@link #acquire})
     */
    public static void release(Identifier effect, boolean owned) {
        if (!owned) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameRenderer == null) return;

        if (effect.equals(mc.gameRenderer.currentPostEffect())) mc.gameRenderer.clearPostEffect();
    }

    /** True when {@code effect} is the currently bound post effect. */
    public static boolean isBound(Identifier effect) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameRenderer == null) return false;
        return effect.equals(mc.gameRenderer.currentPostEffect());
    }
}
