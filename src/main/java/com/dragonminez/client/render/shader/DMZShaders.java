package com.dragonminez.client.render.shader;

import com.dragonminez.Reference;
import com.dragonminez.client.render.util.DmzMaterial;
import net.minecraft.resources.Identifier;

/**
 * Holds the DMZ effect shaders for Minecraft 26.2.
 *
 * <h2>26.2 migration</h2>
 * 1.21 registered core shaders programmatically:
 * <pre>{@code
 * event.registerShader(new ShaderInstance(event.getResourceProvider(),
 *         Identifier.fromNamespaceAndPath(MOD_ID, "aura"), DefaultVertexFormat.POSITION_TEX),
 *         si -> auraShader = si);
 * }</pre>
 * Both the {@code ShaderInstance} class and {@code RegisterShadersEvent} were deleted in 26.2.
 * The replacement is fully data-driven: {@code ShaderManager} scans
 * {@code assets/<namespace>/shaders/**}{@code /*.vsh|.fsh} at resource load and builds a shader for
 * every pair it finds, so a mod only has to ship the GLSL and name it from a
 * {@code RenderPipeline} ({@code withVertexShader} / {@code withFragmentShader}).
 *
 * <p>Consequently there is nothing to register and no per-shader handle to keep. The public fields
 * below are retained as <b>presence markers</b> so existing call sites keep compiling and keep
 * their "is the custom shader available?" guards meaningful. They are non-null whenever the mod's
 * GLSL is on the classpath, which it always is in a packaged build.
 *
 * <p>Per-draw parameters no longer flow through {@code safeGetUniform(...)}; they are written into
 * the {@link DmzMaterial} std140 block and bound by the pipeline. See
 * {@link com.dragonminez.client.render.util.ModRenderTypes} for the pipeline definitions.
 */
public final class DMZShaders {

	/** Shader resource names as shipped under {@code assets/dragonminez/shaders/core/}. */
	public static final Identifier AURA_SHADER = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "core/aura");
	public static final Identifier LIGHTNING_SHADER = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "core/lightning");
	public static final Identifier OUTLINE_SHADER = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "core/transformation_mask");
	public static final Identifier OUTLINE_MASK_TEX_SHADER = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "core/transformation_mask_tex");
	public static final Identifier KI_3D_SHADER = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "core/kiattack");

	/**
	 * Presence markers. Non-null: the shader assets are bundled with the mod, and 26.2's
	 * {@code ShaderManager} loads them automatically. Call sites that guarded against a
	 * registration failure still short-circuit correctly because the value is only ever used as a
	 * null check.
	 */
	public static final Object ki3dShader = KI_3D_SHADER;
	public static final Object auraShader = AURA_SHADER;
	public static final Object lightningShader = LIGHTNING_SHADER;
	public static final Object outlineShader = OUTLINE_SHADER;
	public static final Object outlineMaskTexShader = OUTLINE_MASK_TEX_SHADER;

	private DMZShaders() {
	}
}
