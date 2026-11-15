package com.dragonminez.client.render.util;

import net.minecraft.util.ARGB;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pre-baked helix (spiral) tube meshes for the ki laser / makankosappo beams.
 *
 * <h2>26.2 migration</h2>
 * This used to build a {@code com.mojang.blaze3d.vertex.VertexBuffer} once through
 * {@code Tesselator} and let callers {@code bind()}/{@code drawWithShader()}/{@code unbind()} it.
 * All three of those are gone in 26.2 (the immediate-mode {@code Tesselator} singleton, the
 * {@code VertexBuffer} handle, and the shader-taking draw call).
 *
 * <p>The mesh is now described as plain vertex data — the same {@link KiMeshFactory.Vertex} the other
 * ki meshes use — and replayed into whatever {@link com.mojang.blaze3d.vertex.VertexConsumer} the
 * frame's {@code SubmitNodeCollector} supplies. The LRU cache still exists, it just caches vertex
 * lists instead of GPU handles (and therefore no longer needs to {@code close()} evicted entries).
 *
 * <p>Vertex colour carries the same "local position" encoding the old code wrote: local X/Y are
 * remapped to 0..1 and local Z (a repeating ring index) is the blue channel. The shader read it via
 * {@code localPosMode}; with no per-draw uniforms available the tint is applied on top of that
 * encoding by {@link #emit(List, com.mojang.blaze3d.vertex.PoseStack.Pose, com.mojang.blaze3d.vertex.VertexConsumer, int)}.
 */
public final class KiSpiralMesh {
	private static final int MAX_RINGS = 3000;
	private static final int RING_DENSITY = 24;
	private static final int RADIAL = 8;
	private static final int LOCAL_Z_CYCLE = 6;
	private static final int CACHE_CAPACITY = 8;

	private record Key(int type, int lengthBucket, int sizeBucket) {}

	private static final LinkedHashMap<Key, List<KiMeshFactory.Vertex>> CACHE =
			new LinkedHashMap<>(16, 0.75f, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry<Key, List<KiMeshFactory.Vertex>> eldest) {
					return size() > CACHE_CAPACITY;
				}
			};

	private KiSpiralMesh() {
	}

	/**
	 * Returns the cached helix mesh for the given parameters.
	 *
	 * @param type            cache discriminator (the renderer passes 1 or 2)
	 * @param size            beam thickness, bucketed into the cache key
	 * @param length          beam length, bucketed into the cache key
	 * @param radius          helix radius
	 * @param tubeRadius      tube radius of the first helix
	 * @param tubeRadius2     tube radius of the second helix (only used when {@code doubleHelix})
	 * @param twistsPerBlock  helix twists per block of length
	 * @param doubleHelix     whether to weave a second, phase-shifted tube
	 */
	public static List<KiMeshFactory.Vertex> get(int type, float size, float length,
												 float radius, float tubeRadius, float tubeRadius2,
												 float twistsPerBlock, boolean doubleHelix) {
		Key key = new Key(type, Math.round(length * 4.0f), Math.round(size * 8.0f));
		return CACHE.computeIfAbsent(key,
				k -> build(length, radius, tubeRadius, tubeRadius2, twistsPerBlock, doubleHelix));
	}

	private static List<KiMeshFactory.Vertex> build(float length, float radius, float tubeRadius,
													float tubeRadius2, float twistsPerBlock, boolean doubleHelix) {
		List<KiMeshFactory.Vertex> out = new ArrayList<>();

		int rings = Math.max(2, Math.min(MAX_RINGS, (int) (length * RING_DENSITY)));
		float k = (float) (2.0 * Math.PI * twistsPerBlock);

		appendTube(out, length, radius, tubeRadius, k, rings, 0.0F);
		if (doubleHelix) {
			appendTube(out, length, radius, tubeRadius2, k, rings, (float) Math.PI);
		}
		return List.copyOf(out);
	}

	private static void appendTube(List<KiMeshFactory.Vertex> out, float length, float radius,
								   float tubeRadius, float k, int rings, float phase) {
		float step = length / rings;

		Vector3f center0 = new Vector3f();
		Vector3f center1 = new Vector3f();
		Vector3f tangent = new Vector3f();
		Vector3f side = new Vector3f();
		Vector3f up = new Vector3f();
		Vector3f ref = new Vector3f();

		Vector3f p00 = new Vector3f();
		Vector3f p01 = new Vector3f();
		Vector3f p10 = new Vector3f();
		Vector3f p11 = new Vector3f();
		Vector3f n00 = new Vector3f();
		Vector3f n01 = new Vector3f();
		Vector3f n10 = new Vector3f();
		Vector3f n11 = new Vector3f();

		for (int i = 0; i < rings; i++) {
			float z0 = i * step;
			float z1 = (i + 1) * step;

			helixCenter(center0, radius, k, z0, phase);
			helixCenter(center1, radius, k, z1, phase);

			helixFrame(z0, radius, k, phase, tangent, side, up, ref);
			float lz0 = (float) (i % LOCAL_Z_CYCLE) / LOCAL_Z_CYCLE;

			Vector3f sideA = new Vector3f(side);
			Vector3f upA = new Vector3f(up);

			helixFrame(z1, radius, k, phase, tangent, side, up, ref);
			float lz1 = (float) ((i + 1) % LOCAL_Z_CYCLE) / LOCAL_Z_CYCLE;

			for (int j = 0; j < RADIAL; j++) {
				float a0 = (float) (2.0 * Math.PI * j / RADIAL);
				float a1 = (float) (2.0 * Math.PI * (j + 1) / RADIAL);

				float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
				float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);

				ringPoint(p00, n00, center0, sideA, upA, c0, s0, tubeRadius);
				ringPoint(p01, n01, center0, sideA, upA, c1, s1, tubeRadius);
				ringPoint(p11, n11, center1, side, up, c1, s1, tubeRadius);
				ringPoint(p10, n10, center1, side, up, c0, s0, tubeRadius);

				addVertex(out, p00, n00, c0, s0, lz0, 0.0F);
				addVertex(out, p01, n01, c1, s1, lz0, 1.0F);
				addVertex(out, p11, n11, c1, s1, lz1, 1.0F);
				addVertex(out, p10, n10, c0, s0, lz1, 0.0F);
			}
		}
	}

	private static void helixCenter(Vector3f out, float radius, float k, float z, float phase) {
		float a = k * z + phase;
		out.set(-radius * (float) Math.sin(a), radius * (float) Math.cos(a), z);
	}

	private static void helixFrame(float z, float radius, float k, float phase,
								   Vector3f tangent, Vector3f side, Vector3f up, Vector3f ref) {
		float a = k * z + phase;
		tangent.set(-radius * k * (float) Math.cos(a), -radius * k * (float) Math.sin(a), 1.0F).normalize();

		if (Math.abs(tangent.z) < 0.95F) {
			ref.set(0.0F, 0.0F, 1.0F);
		} else {
			ref.set(1.0F, 0.0F, 0.0F);
		}
		tangent.cross(ref, side).normalize();
		side.cross(tangent, up).normalize();
	}

	private static void ringPoint(Vector3f outPos, Vector3f outNormal, Vector3f center,
								  Vector3f side, Vector3f up, float cos, float sin, float tubeRadius) {
		outNormal.set(
				side.x * cos + up.x * sin,
				side.y * cos + up.y * sin,
				side.z * cos + up.z * sin);
		outPos.set(
				center.x + outNormal.x * tubeRadius,
				center.y + outNormal.y * tubeRadius,
				center.z + outNormal.z * tubeRadius);
	}

	private static void addVertex(List<KiMeshFactory.Vertex> out, Vector3f pos, Vector3f normal,
								  float localX, float localY, float localZ, float u) {
		int color = ARGB.color(255, encode(localX * 0.5F + 0.5F), encode(localY * 0.5F + 0.5F), encode(localZ));
		out.add(new KiMeshFactory.Vertex(
				pos.x(), pos.y, pos.z,
				u, localZ,
				normal.x, normal.y, normal.z,
				color));
	}

	private static int encode(float value) {
		return Math.round(Math.max(0.0F, Math.min(1.0F, value)) * 255.0F);
	}
}
