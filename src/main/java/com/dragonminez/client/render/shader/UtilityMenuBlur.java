package com.dragonminez.client.render.shader;

import com.dragonminez.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Blur post-effect control for the utility menu.
 *
 * <h2>26.2 restoration</h2>
 * 1.21 loaded a {@code PostChain} by hand and owned its lifetime via
 * {@code GameRenderer#loadEffect}/{@code #shutdownEffect}. Both are gone, which originally looked like
 * the feature was removed outright. It is not: 26.2 selects post effects by <em>id</em> instead of by
 * ownership, and the selector is public. {@link PostEffectBridge} wraps that, including the
 * "don't stomp an entity shader" check this class needs.
 *
 * <p>The chain itself moved to {@code assets/dragonminez/post_effect/utility_blur.json} and carries
 * its uniforms in JSON, so nothing has to be pushed per frame.
 */
public final class UtilityMenuBlur {

	/** Feature switch; also lets players disable the blur through config. */
	public static boolean ENABLED = true;

	/** The DMZ blur post chain, resolved by {@code ShaderManager} from id. */
	public static final Identifier EFFECT =
			Identifier.fromNamespaceAndPath(Reference.MOD_ID, "utility_blur");

	private static boolean active;

	private UtilityMenuBlur() {}

	/** Binds the DMZ blur effect if the menu wants it and nothing else already owns the slot. */
	public static void start() {
		if (!ENABLED || active) return;
		active = PostEffectBridge.acquire(EFFECT);
	}

	/**
	 * Releases the blur, but only if it is still the effect in the slot. Vanilla's entity shaders
	 * (creeper/spider/enderman) can take the slot while the menu is open; clearing unconditionally
	 * would silently drop them.
	 */
	public static void stop() {
		PostEffectBridge.release(EFFECT, active);
		active = false;
	}

	/** True when the blur is currently bound by this class. */
	public static boolean isActive() {
		return active;
	}

	/** True when the blur would be active on a version that supports it. */
	public static boolean isRequested() {
		return ENABLED && Minecraft.getInstance().gameRenderer != null;
	}
}
