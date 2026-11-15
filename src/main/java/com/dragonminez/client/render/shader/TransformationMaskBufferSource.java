package com.dragonminez.client.render.shader;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Pass-through collector for the transformation-mask path.
 *
 * <h2>26.2 migration</h2>
 * The 1.20.1 version implemented {@code SubmitNodeCollector#getBuffer(RenderType)} to intercept every
 * buffer requested during entity rendering, build the same geometry twice (original + a colour-packed
 * "mask" copy), and hand the mask batch to a temporary target owned by the transformation post-effect
 * chain.
 *
 * <p>Two things removed that design in 26.2:
 * <ol>
 *   <li>{@code SubmitNodeCollector#getBuffer} is gone — the collector is now a submit interface with
 *       {@code submitCustomGeometry} and friends, and {@code RenderType} no longer exposes
 *       {@code mode()}/{@code format()}/{@code draw(MeshData)}.</li>
 *   <li>{@code PostChain#getTempTarget(String)} is gone as well (see
 *       {@link TransformationPostShaderManager}), so even a captured mask batch would have no
 *       destination.</li>
 * </ol>
 *
 * <p>What remains is the delegation contract: every call is forwarded unchanged to the delegate so
 * entity rendering keeps working. {@link #endMaskBatch()} is a no-op. Restoring the outline needs a
 * render-pipeline based approach rather than a collector wrapper; see 交接文档-v5-补遗.md §3.1.10.
 */
public final class TransformationMaskBufferSource {

    private SubmitNodeCollector delegate;

    public TransformationMaskBufferSource wrap(SubmitNodeCollector delegate) {
        this.delegate = delegate;
        return this;
    }

    /** The collector to submit geometry to, or {@code null} when no pass is active. */
    public SubmitNodeCollector getDelegate() {
        return this.delegate;
    }

    /**
     * Kept so callers keep compiling. The mask batch used to be flushed into the post-chain target
     * here; in 26.2 there is nothing to flush to.
     */
    public void endMaskBatch() {
        this.delegate = null;
    }

    // The former colour-packing / capture toggles. They no longer influence anything but are kept
    // because TransformationPostShaderManager still calls them around a render pass.
    public void setEntityColors(float primaryR, float primaryG, float primaryB,
                                float secondaryR, float secondaryG, float secondaryB) {
    }

    public void setMaskCaptureEnabled(boolean enabled) {
    }

    public void setIncludeOriginal(boolean includeOriginal) {
    }

    public void setForceCaptureAll(boolean forceCaptureAll) {
    }

    public void setMaskCaptureBlocked(boolean blocked) {
    }

    /** Unused, kept for source compatibility with the old call sites. */
    public RenderType remap(RenderType renderType) {
        return renderType;
    }
}
