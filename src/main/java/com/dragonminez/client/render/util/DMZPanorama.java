package com.dragonminez.client.render.util;

import com.dragonminez.mixin.client.GuiGraphicsStateAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Per-screen rotating panorama backdrop.
 *
 * <h2>26.2 migration</h2>
 * 1.21 shipped {@code net.minecraft.client.renderer.PanoramaRenderer}, constructed as
 * {@code new PanoramaRenderer(new CubeMap(texture))} and drawn with
 * {@code render(graphics, width, height, alpha, partialTick)}. That class was deleted. 26.2 splits the
 * job in two:
 * <ul>
 *   <li>{@link CubeMap} survived, but only as the raw 6-face renderer:
 *       {@code render(float rotXInDegrees, float rotYInDegrees)};</li>
 *   <li>the new {@code net.minecraft.client.renderer.Panorama} owns the spin timer and the overlay
 *       blit, but takes <b>no</b> texture — the cube map is a single global owned by the GUI renderer,
 *       so it cannot be pointed at a per-screen / per-race texture.</li>
 * </ul>
 * DMZ needs a different panorama per race, so this wrapper keeps the old shape: it owns one
 * {@link CubeMap} per instance, advances its own spin, and draws the vanilla overlay on top. The
 * {@code alpha} parameter is accepted for call-site compatibility but cannot be honoured — a
 * {@link CubeMap} draw has no blend argument, so cross-fades between two panoramas are no longer
 * expressible.
 */
public final class DMZPanorama implements AutoCloseable {

	/** Vanilla spins the panorama about X by 10 degrees; matched here. */
	private static final float TILT_X = 10.0F;
	private static final float SPIN_PER_TICK = 0.1F;

	private final CubeMap cubeMap;
	private float spin;

	/**
	 * @param cubeMap the cube map to own; its location is used to register the texture
	 * @param location the cube-map texture identifier (e.g. {@code dragonminez:saiyan_panorama})
	 */
	public DMZPanorama(CubeMap cubeMap, Identifier location) {
		this.cubeMap = cubeMap;

		// 26.2: CubeMap#registerTextures uses TextureManager#register, which only files the texture in
		// the lookup map and never uploads it. The upload lives in registerAndLoad:
		//     tex.apply(this.loadContentsSafe(id, tex));   // assigns texture / textureView
		//     this.register(id, tex);
		// Vanilla gets away with register() because its CubeMap is registered before a resource
		// reload runs; DMZ creates panoramas lazily inside a screen's init(), long after the last
		// reload, so textureView stayed null and CubeMap#render threw
		//     IllegalStateException: Texture view does not exist
		// Registering through registerAndLoad performs the upload synchronously, so the texture is
		// usable on the very first frame.
		Minecraft.getInstance().getTextureManager()
				.registerAndLoad(location, new net.minecraft.client.renderer.texture.CubeMapTexture(location));
	}

	/**
	 * Draws the panorama behind the current GUI contents.
	 *
	 * @param graphics    the screen's graphics helper
	 * @param width       viewport width
	 * @param height      viewport height
	 * @param alpha       ignored — see the class note
	 * @param partialTick partial tick, used to advance the spin
	 */
	public void render(GuiGraphicsExtractor graphics, int width, int height, float alpha, float partialTick) {
		if (alpha <= 0.0F) return;
		this.spin = Mth.wrapDegrees(this.spin + Minecraft.getInstance().getDeltaTracker().getRealtimeDeltaTicks() * SPIN_PER_TICK);
		StateAccess state = (StateAccess) ((GuiGraphicsStateAccessor) graphics).dmz$getGuiRenderState();
		state.dmz$setPanorama(new Frame(this.cubeMap, -this.spin));
		graphics.blit(RenderPipelines.GUI_TEXTURED,
				Identifier.withDefaultNamespace("textures/gui/title/background/panorama_overlay.png"),
				0, 0, 0.0F, 0.0F, width, height, 16, 128, 16, 128);
	}

	public record Frame(CubeMap cubeMap, float spin) {
		public void render() {
			cubeMap.render(TILT_X, spin);
		}
	}

	public interface StateAccess {
		Frame dmz$getPanorama();
		void dmz$setPanorama(Frame frame);
	}

	@Override
	public void close() {
		this.cubeMap.close();
	}
}
