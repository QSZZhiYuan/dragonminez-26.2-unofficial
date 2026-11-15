package com.dragonminez.client.render.shader;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Fans every vertex call out to two {@link VertexConsumer}s.
 *
 * <h2>26.2 migration</h2>
 * Minecraft used to ship {@code com.mojang.blaze3d.vertex.VertexMultiConsumer} with a
 * {@code create(a, b)} factory; it was deleted in 26.2 and nothing replaced it (the whole
 * {@code blaze3d.vertex} package lost its helper consumers, keeping only the core builder types).
 *
 * <p>DMZ needs it for the transformation-mask pass: each vertex has to be written both to the
 * on-screen buffer and to the off-screen mask buffer.
 *
 * <p>Only the eight <b>abstract</b> members of {@link VertexConsumer} are implemented here. In 26.2
 * everything else ({@code setLight}, {@code setOverlay}, and every pose-taking
 * {@code addVertex}/{@code setNormal} overload) is a {@code default} method that funnels into those
 * eight, so implementing the primitives is sufficient and a chained
 * {@code addVertex(...).setUv(...)} keeps working.
 */
public final class MultiVertexConsumer implements VertexConsumer {

	private final VertexConsumer first;
	private final VertexConsumer second;

	private MultiVertexConsumer(VertexConsumer first, VertexConsumer second) {
		this.first = first;
		this.second = second;
	}

	/** Returns a consumer that writes every vertex to both {@code first} and {@code second}. */
	public static VertexConsumer create(VertexConsumer first, VertexConsumer second) {
		return new MultiVertexConsumer(first, second);
	}

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		this.second.addVertex(x, y, z);
		return this.first.addVertex(x, y, z);
	}

	@Override
	public VertexConsumer setColor(int r, int g, int b, int a) {
		this.second.setColor(r, g, b, a);
		return this.first.setColor(r, g, b, a);
	}

	@Override
	public VertexConsumer setColor(int color) {
		this.second.setColor(color);
		return this.first.setColor(color);
	}

	@Override
	public VertexConsumer setUv(float u, float v) {
		this.second.setUv(u, v);
		return this.first.setUv(u, v);
	}

	@Override
	public VertexConsumer setUv1(int u, int v) {
		this.second.setUv1(u, v);
		return this.first.setUv1(u, v);
	}

	@Override
	public VertexConsumer setUv2(int u, int v) {
		this.second.setUv2(u, v);
		return this.first.setUv2(u, v);
	}

	@Override
	public VertexConsumer setNormal(float x, float y, float z) {
		this.second.setNormal(x, y, z);
		return this.first.setNormal(x, y, z);
	}

	@Override
	public VertexConsumer setLineWidth(float width) {
		this.second.setLineWidth(width);
		return this.first.setLineWidth(width);
	}
}
