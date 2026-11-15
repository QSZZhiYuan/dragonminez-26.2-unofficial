package com.dragonminez.client.render.util;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;

/**
 * The single std140 UBO block the DMZ effect shaders read their per-draw parameters from.
 *
 * <h2>Why this replaces {@code ShaderInstance#safeGetUniform}</h2>
 * 1.21 let call sites poke individual uniforms on a live {@code ShaderInstance}:
 * <pre>{@code
 * shader.safeGetUniform("colorCore").set(r, g, b);
 * shader.safeGetUniform("time").set(t);
 * }</pre>
 * 26.2 deletes {@code ShaderInstance} entirely. Uniforms are now declared on the pipeline's
 * {@code BindGroupLayout} as a std140 uniform buffer, filled on the CPU with {@link Std140Builder},
 * and handed to the render pass by name ({@code RenderPass#setUniform(name, GpuBufferSlice)}).
 *
 * <p>Every DMZ shader therefore reads one block named {@code DmzMaterial}. The field order below
 * is the contract — it must match the {@code layout(std140) uniform DmzMaterial} declaration in
 * {@code assets/dragonminez/shaders/core/*.fsh|vsh} exactly.
 *
 * <h2>Layout</h2>
 * <pre>
 * float time;          // animation clock (seconds)
 * float alphaMult;     // global alpha
 * float speed;         // spritesheet frame cursor
 * float pad0;
 * vec3  colorCore;
 * float pad1;
 * vec3  colorBorder;
 * float pad2;
 * vec3  colorOutline;
 * float pad3;
 * vec4  color1;
 * vec4  color2;
 * vec4  color3;
 * vec4  color4;
 * </pre>
 *
 * <p>Instances are mutable builders: set the fields a given effect needs, then
 * {@link #write(Matrix4f)} to obtain a slice for this frame.
 */
public final class DmzMaterial {

    /** Byte size of the block: 4 floats + 3 (vec3+float) + 4 vec4 = 4*4 + 3*16 + 4*16 = 128. */
    public static final int SIZE = 128;

    /** Uniform block name as declared in GLSL and on the pipeline bind group layout. */
    public static final String BLOCK_NAME = "DmzMaterial";

    public float time;
    public float alphaMult = 1.0f;
    public float speed;

    public float coreR, coreG, coreB;
    public float borderR, borderG, borderB;
    public float outlineR, outlineG, outlineB;

    public float c1r, c1g, c1b, c1a = 1.0f;
    public float c2r, c2g, c2b, c2a = 1.0f;
    public float c3r, c3g, c3b, c3a = 1.0f;
    public float c4r, c4g, c4b, c4a = 1.0f;

    public DmzMaterial setTime(float seconds) {
        this.time = seconds;
        return this;
    }

    public DmzMaterial setAlpha(float alpha) {
        this.alphaMult = alpha;
        return this;
    }

    public DmzMaterial setSpeed(float frameCursor) {
        this.speed = frameCursor;
        return this;
    }

    public DmzMaterial setCore(float r, float g, float b) {
        this.coreR = r;
        this.coreG = g;
        this.coreB = b;
        return this;
    }

    public DmzMaterial setBorder(float r, float g, float b) {
        this.borderR = r;
        this.borderG = g;
        this.borderB = b;
        return this;
    }

    public DmzMaterial setOutline(float r, float g, float b) {
        this.outlineR = r;
        this.outlineG = g;
        this.outlineB = b;
        return this;
    }

    public DmzMaterial setCore(Vector3f v) { return setCore(v.x, v.y, v.z); }
    public DmzMaterial setBorder(Vector3f v) { return setBorder(v.x, v.y, v.z); }
    public DmzMaterial setOutline(Vector3f v) { return setOutline(v.x, v.y, v.z); }

    public DmzMaterial setColorBand(int index, float r, float g, float b, float a) {
        switch (index) {
            case 0 -> { c1r = r; c1g = g; c1b = b; c1a = a; }
            case 1 -> { c2r = r; c2g = g; c2b = b; c2a = a; }
            case 2 -> { c3r = r; c3g = g; c3b = b; c3a = a; }
            default -> { c4r = r; c4g = g; c4b = b; c4a = a; }
        }
        return this;
    }

    public DmzMaterial setColor1(Vector4f v) { return setColorBand(0, v.x, v.y, v.z, v.w); }
    public DmzMaterial setColor2(Vector4f v) { return setColorBand(1, v.x, v.y, v.z, v.w); }
    public DmzMaterial setColor3(Vector4f v) { return setColorBand(2, v.x, v.y, v.z, v.w); }
    public DmzMaterial setColor4(Vector4f v) { return setColorBand(3, v.x, v.y, v.z, v.w); }

    /**
     * Serializes this block into a freshly allocated slice suitable for
     * {@code RenderPass#setUniform(DmzMaterial.BLOCK_NAME, slice)}.
     *
     * <p>The returned slice is a copy: it stays valid after this instance is mutated again, so
     * several effects can be queued in one frame.
     */
    public GpuBufferSlice write() {
        ByteBuffer buffer = serialize();
        GpuBuffer gpu = com.mojang.blaze3d.systems.RenderSystem.getDevice()
                .createBuffer(() -> "dmz_material", com.mojang.blaze3d.buffers.GpuBuffer.USAGE_UNIFORM, buffer);
        return new GpuBufferSlice(gpu, 0L, SIZE);
    }

    /** Serializes the block into a direct ByteBuffer laid out exactly as the GLSL expects. */
    public ByteBuffer serialize() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(SIZE);
        Std140Builder.intoBuffer(buffer)
                .putFloat(time)
                .putFloat(alphaMult)
                .putFloat(speed)
                .putFloat(0.0f)
                .putVec3(coreR, coreG, coreB)
                .putFloat(0.0f)
                .putVec3(borderR, borderG, borderB)
                .putFloat(0.0f)
                .putVec3(outlineR, outlineG, outlineB)
                .putFloat(0.0f)
                .putVec4(c1r, c1g, c1b, c1a)
                .putVec4(c2r, c2g, c2b, c2a)
                .putVec4(c3r, c3g, c3b, c3a)
                .putVec4(c4r, c4g, c4b, c4a);
        buffer.flip();
        return buffer;
    }

    /** Convenience overload kept for call sites that still pass a matrix; it is ignored. */
    public GpuBufferSlice write(Matrix4f ignored) {
        return write();
    }

    }
