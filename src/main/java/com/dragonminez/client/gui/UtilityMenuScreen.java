package com.dragonminez.client.gui;

import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.client.gui.radial.FormPreview;
import com.dragonminez.client.gui.radial.IUtilityMenuSlotAdapter;
import com.dragonminez.client.gui.radial.ModelFormPreview;
import com.dragonminez.client.gui.radial.RadialLayoutStore;
import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.client.gui.radial.nodes.*;
import com.dragonminez.client.gui.utilitymenu.IUtilityMenuSlot;
import com.dragonminez.client.render.layer.DMZSkinLayer;
import com.dragonminez.client.render.shader.UtilityMenuBlur;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.client.util.ScrollbarState;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class UtilityMenuScreen extends ScaledScreen {
	private static final List<IUtilityMenuSlot> ADDON_SLOTS = new ArrayList<>();
	private static long utilityMenuReopenBlockedUntilMs = 0L;

	private static final long ANIMATION_DURATION = 140L;
	private static final int SLOTS = 8;

	private static final float R_INNER = 40f;
	private static final float R_OUTER = 86f;
	private static final float CHILD_BAND = 42f;
	private static final float SECTOR_GAP_DEG = 3f;

	private static final float ICON_BASE = 16f;
	private static final float ICON_CHILD = 14f;
	private static final float CHILD_ANGLE_HYSTERESIS_DEG = SECTOR_GAP_DEG;

	private static final int MODEL_SCALE = 32;
	private static final int MODEL_Y_OFFSET = 26;
	private static final float ANIM_SPEED = 14f;

	private static final float[] PANEL = {0.11f, 0.11f, 0.13f, 0.60f};
	private static final float[] PANEL_HOVER = {0.20f, 0.52f, 0.96f, 0.90f};
	private static final float[] PANEL_INACTIVE = {0.09f, 0.09f, 0.10f, 0.42f};
	private static final float[] CHILD_PANEL = {0.10f, 0.12f, 0.11f, 0.64f};
	private static final float[] CHILD_HOVER = {0.22f, 0.46f, 0.30f, 0.86f};

	private final long openTime;
	private boolean closing = false;
	private long closeStartTime = -1L;
	private long lastFrameNanos = 0L;
	private float frameDt = 0f;

	private StatsData statsData;
	private final List<RadialNode> baseNodes = new ArrayList<>();
	private final List<RadialNode> chain = new ArrayList<>();
	private int hoveredRadialLevel = 0;
	private FormPreview currentPreview = null;

	private static final int PANEL_WIDTH = 130;
	private static final int PANEL_ROW_H = 13;
	private static final int PANEL_TITLE_H = 15;
	private static final int PANEL_PAD = 5;
	private static final int PANEL_SCREEN_MARGIN = 4;
	private static final float PANEL_CLEAR_GAP = 12f;
	private static final int PANEL_MAX_ROWS = 8;

	private List<RadialNode> panelOptions = null;
	private Component panelTitle = null;
	private boolean panelScrollable = false;
	private int panelScroll = 0;
	private final ScrollbarState panelBar = new ScrollbarState();
	private MoreNode openMore = null;
	private float panelAngleDeg = 0f;
	private int panelLevel = 0;
	private Hover frozenHover = new Hover();
	private int dragIndex = -1;
	private boolean dragging = false;
	private double dragStartY = 0;
	private double dragCurrentY = 0;

	private static final long DOUBLE_CLICK_MS = 300L;
	private RadialNode lastClickNode = null;
	private long lastClickMs = 0L;

	public UtilityMenuScreen() {
		super(Component.literal("Menu").withStyle(Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(DMZ_FONT))));
		this.openTime = System.currentTimeMillis();
	}

	@Override
	protected void init() {
		super.init();
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> this.statsData = data);
		}
		buildBaseNodes();
		UtilityMenuBlur.start();
	}

	@Override
	public void removed() {
		super.removed();
		UtilityMenuBlur.stop();
	}

	private void buildBaseNodes() {
		baseNodes.clear();
		int[] addon = {0};
		baseNodes.add(new SuperFormNode());        // slot 0  top
		baseNodes.add(new MoreFormsNode());        // slot 1  upper-right
		baseNodes.add(nextAddonOrEmpty(addon));    // slot 2  right
		baseNodes.add(new MovementNode());         // slot 3  lower-right
		baseNodes.add(new DescendNode());          // slot 4  bottom
		baseNodes.add(new ActionsNode());          // slot 5  lower-left
		baseNodes.add(nextAddonOrEmpty(addon));    // slot 6  left
		baseNodes.add(new StackSkillNode());       // slot 7  upper-left
	}

	private RadialNode nextAddonOrEmpty(int[] addonIndex) {
		if (addonIndex[0] < ADDON_SLOTS.size()) return new IUtilityMenuSlotAdapter(ADDON_SLOTS.get(addonIndex[0]++));
		return new EmptyNode();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	protected float computeDynamicScale(float availableScale) {
		return availableScale * (2f / 3f) * ConfigManager.getUserConfig().getUtilityMenuScaleMultiplier();
	}

	@Override
	protected float getMinUiScale() {
		return 0.25f;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		if (statsData == null) return;

		updateUiScale();

		long nowNanos = System.nanoTime();
		frameDt = lastFrameNanos == 0L ? 0f : Math.min(0.1f, (nowNanos - lastFrameNanos) / 1.0e9f);
		lastFrameNanos = nowNanos;

		float openScale = computeOpenScale();

		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);

		beginUiScale(graphics);

		int w = getUiWidth();
		int h = getUiHeight();

		float cx = w / 2f;
		float cy = h / 2f;

		Hover hover = closing ? new Hover()
				: (panelOptions != null ? frozenHover : resolveHover(cx, cy, uiMouseX, uiMouseY, openScale));
		currentPreview = hover.deepest != null ? hover.deepest.preview(statsData) : null;

		drawBaseSectors(graphics, cx, cy, hover, openScale);
		for (RadialNode node : baseNodes) {
			if (isActiveSlot(node)) drawChildSectors(graphics, cx, cy, node, baseCenter(baseNodes.indexOf(node)), 1, hover, false, openScale);
		}

		int modelX = Math.round(cx);
		int modelY = Math.round(cy + MODEL_Y_OFFSET * openScale + 12 * openScale);
		ModelFormPreview.render(graphics, modelX, modelY,
				Math.max(1, Math.round(MODEL_SCALE * openScale)), (float) modelX, (float) modelY, currentPreview);

		drawBaseFaces(graphics, cx, cy, openScale);
		for (RadialNode node : baseNodes) {
			if (isActiveSlot(node)) drawChildSectors(graphics, cx, cy, node, baseCenter(baseNodes.indexOf(node)), 1, hover, true, openScale);
		}

		if (panelOptions != null) drawPanel(graphics, cx, cy, uiMouseX, uiMouseY);

		super.extractRenderState(graphics, (int) Math.round(uiMouseX), (int) Math.round(uiMouseY), partialTick);
		endUiScale(graphics);
	}

	private static float radiusForLevel(int level) {
		if (level <= 0) return (R_INNER + R_OUTER) / 2f;
		return R_OUTER + (level - 0.5f) * CHILD_BAND;
	}

	private int visiblePanelRows() {
		int total = panelOptions.size();
		return panelScrollable ? Math.min(total, PANEL_MAX_ROWS) : total;
	}

	private int[] panelBounds(float cx, float cy) {
		int rows = visiblePanelRows();
		int height = PANEL_TITLE_H + rows * PANEL_ROW_H + PANEL_PAD;

		double rad = Math.toRadians(panelAngleDeg);
		float dirX = (float) Math.cos(rad);
		float dirY = (float) Math.sin(rad);

		float clearRadius = radiusForLevel(panelLevel) + CHILD_BAND * 0.6f + PANEL_CLEAR_GAP;

		float panelCx = cx + dirX * clearRadius;
		float panelCy = cy + dirY * clearRadius;

		int px = Math.round(panelCx - PANEL_WIDTH / 2f);
		int py = Math.round(panelCy - height / 2f);

		int uiW = getUiWidth();
		int uiH = getUiHeight();

		int maxPx = uiW - PANEL_WIDTH - PANEL_SCREEN_MARGIN;
		int maxPy = uiH - height - PANEL_SCREEN_MARGIN;
		px = Mth.clamp(px, PANEL_SCREEN_MARGIN, Math.max(PANEL_SCREEN_MARGIN, maxPx));
		py = Mth.clamp(py, PANEL_SCREEN_MARGIN, Math.max(PANEL_SCREEN_MARGIN, maxPy));

		return new int[]{px, py, PANEL_WIDTH, height};
	}

	private int rowIndexAt(int rowsTop, double uiY) {
		return (int) Math.floor((uiY - rowsTop) / PANEL_ROW_H);
	}

	private void drawPanel(GuiGraphicsExtractor graphics, float cx, float cy, double mouseX, double mouseY) {
		List<RadialNode> opts = panelOptions;
		int visibleRows = visiblePanelRows();
		int maxScroll = Math.max(0, opts.size() - visibleRows);
		panelScroll = Mth.clamp(panelScroll, 0, maxScroll);
		int start = panelScrollable ? panelScroll : 0;

		int[] b = panelBounds(cx, cy);
		int px = b[0], py = b[1], pw = b[2], ph = b[3];

		graphics.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xFF000000);
		graphics.fill(px, py, px + pw, py + ph, 0xF0141420);
		graphics.fill(px, py, px + pw, py + PANEL_TITLE_H, 0x30FFFFFF);

		Style style = Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(DMZ_FONT));
		Component title = (panelTitle != null ? panelTitle : Component.translatable("gui.dragonminez.radial.options")).copy().withStyle(style);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, title, px + pw / 2, py + 4, 0xFFFFFFFF, 0xFF000000);

		int rowsTop = py + PANEL_TITLE_H;
		int textWidth = pw - 12 - (maxScroll > 0 ? 4 : 0);
		for (int r = 0; r < visibleRows; r++) {
			int i = start + r;
			RadialNode node = opts.get(i);
			int ry = rowsTop + r * PANEL_ROW_H;
			boolean isDragged = !panelScrollable && dragging && i == dragIndex;
			int drawY = isDragged ? (int) Math.round(dragCurrentY - PANEL_ROW_H / 2.0) : ry;

			boolean hovered = !dragging && mouseX >= px && mouseX <= px + pw && mouseY >= ry && mouseY < ry + PANEL_ROW_H;
			if (hovered) graphics.fill(px + 1, ry, px + pw - 1, ry + PANEL_ROW_H - 1, 0x55000000);
			if (isDragged) graphics.fill(px + 1, drawY, px + pw - 1, drawY + PANEL_ROW_H - 1, 0x33FFFFFF);
			if (node.active(statsData)) drawRowBorder(graphics, px + 1, drawY, px + pw - 1, drawY + PANEL_ROW_H - 1, 0xFF3BE05A);

			int color = ARGB.opaque(node.labelColor(statsData));
			String text = this.font.plainSubstrByWidth(node.label(statsData).getString(), textWidth);
			TextUtil.drawStringWithBorder(graphics, this.font, Component.literal(text).withStyle(style), px + 6, drawY + 3, color, 0xFF000000);
		}

		if (panelScrollable) {
			int trackX = px + pw - 3;
			int trackTop = rowsTop;
			int trackH = visibleRows * PANEL_ROW_H;
			panelBar.update(trackX, 2, trackTop, trackH, maxScroll);
			if (maxScroll > 0) {
				graphics.fill(trackX, trackTop, trackX + 2, trackTop + trackH, 0x40FFFFFF);
				int thumbH = Math.max(6, trackH * visibleRows / opts.size());
				int thumbY = trackTop + (trackH - thumbH) * panelScroll / maxScroll;
				graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, 0xC0FFFFFF);
			}
		} else {
			panelBar.clear();
		}

		if (!panelScrollable && dragging && dragIndex >= 0) {
			int target = Mth.clamp(rowIndexAt(rowsTop, mouseY), 0, opts.size());
			int lineY = rowsTop + target * PANEL_ROW_H;
			graphics.fill(px + 2, lineY - 1, px + pw - 2, lineY, 0xFFFFFFFF);
		}
	}

	private void drawRowBorder(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
		graphics.fill(x0, y0, x1, y0 + 1, color);
		graphics.fill(x0, y1 - 1, x1, y1, color);
		graphics.fill(x0, y0, x0 + 1, y1, color);
		graphics.fill(x1 - 1, y0, x1, y1, color);
	}

	private void reorderOption(int from, int to) {
		if (openMore == null) return;
		List<RadialNode> opts = openMore.options();
		if (from < 0 || from >= opts.size() || from == to) return;
		RadialNode moved = opts.remove(from);
		if (to > from) to--;
		to = Mth.clamp(to, 0, opts.size());
		opts.add(to, moved);

		List<String> keys = new ArrayList<>();
		boolean orderable = true;
		for (RadialNode node : opts) {
			String key = node.orderKey();
			if (key.isEmpty()) {
				orderable = false;
				break;
			}
			keys.add(key);
		}
		String categoryKey = openMore.categoryKey();
		if (orderable) {
			RadialLayoutStore.setOrder(categoryKey, keys);
			RadialLayoutStore.save();
		}
		buildBaseNodes();
		MoreNode refreshed = findMoreNode(categoryKey);
		if (refreshed != null) {
			openMore = refreshed;
			panelOptions = refreshed.options();
			refreshFrozenHoverFor(refreshed);
		}
	}

	private void refreshFrozenHoverFor(MoreNode target) {
		Hover rebuilt = new Hover();
		for (RadialNode base : baseNodes) {
			if (findPathTo(base, target, rebuilt.path)) {
				rebuilt.path.add(0, base);
				rebuilt.deepest = target;
				break;
			}
		}
		frozenHover = rebuilt;
	}

	private boolean findPathTo(RadialNode current, RadialNode target, List<RadialNode> outPath) {
		if (current == target) return true;
		for (RadialNode child : current.children(statsData)) {
			if (findPathTo(child, target, outPath)) {
				outPath.add(0, child);
				return true;
			}
		}
		return false;
	}

	private MoreNode findMoreNode(String categoryKey) {
		for (RadialNode base : baseNodes) {
			MoreNode found = findMoreNodeRec(base, categoryKey);
			if (found != null) return found;
		}
		return null;
	}

	private MoreNode findMoreNodeRec(RadialNode node, String categoryKey) {
		for (RadialNode child : node.children(statsData)) {
			if (child instanceof MoreNode more && more.categoryKey().equals(categoryKey)) return more;
			MoreNode deep = findMoreNodeRec(child, categoryKey);
			if (deep != null) return deep;
		}
		return null;
	}

	private float computeOpenScale() {
		long ms = System.currentTimeMillis();
		if (closing) {
			float p = Math.min(1.0f, (float) (ms - closeStartTime) / ANIMATION_DURATION);
			return Mth.clamp(easeOut(Math.max(0.0f, 1.0f - p)), 0.0f, 1.0f);
		}
		return Mth.clamp(easeOut(Mth.clamp((float) (ms - openTime) / ANIMATION_DURATION, 0.0f, 1.0f)), 0.0f, 1.0f);
	}

	private boolean isActiveSlot(RadialNode node) {
		return node.visible(statsData) && node.interactive(statsData);
	}

	private void drawBaseSectors(GuiGraphicsExtractor graphics, float cx, float cy, Hover hover, float openScale) {
		for (int i = 0; i < baseNodes.size(); i++) {
			RadialNode node = baseNodes.get(i);
			boolean active = isActiveSlot(node);
			float highlight = 0f;
			float hoverAmt = 0f;
			float[] color = PANEL_INACTIVE;
			if (active) {
				updateAnim(node, hover);
				if (node instanceof AbstractRadialNode a) {
					highlight = a.animHighlight;
					hoverAmt = a.animScale;
				}
				color = lerpColor(PANEL, PANEL_HOVER, Math.max(highlight * 0.5f, hoverAmt));
			}
			float center = baseCenter(i);
			float rIn = (R_INNER - 2f * hoverAmt) * openScale;
			float rOut = (R_OUTER + 4f * hoverAmt) * openScale;
			float half = 22.5f - SECTOR_GAP_DEG / 2f;
			fillSector(graphics, cx, cy, rIn, rOut, center - half, center + half, color);
		}
	}

	private void drawBaseFaces(GuiGraphicsExtractor graphics, float cx, float cy, float openScale) {
		float radius = (R_INNER + R_OUTER) / 2f * openScale;
		for (int i = 0; i < baseNodes.size(); i++) {
			RadialNode node = baseNodes.get(i);
			if (!isActiveSlot(node)) continue;
			float hoverAmt = node instanceof AbstractRadialNode a ? a.animScale : 0f;
			drawFace(graphics, cx, cy, node, baseCenter(i), radius, ICON_BASE, hoverAmt, 1.0f, (360f / SLOTS) / 2f - SECTOR_GAP_DEG / 2f);
		}
	}

	private void drawChildSectors(GuiGraphicsExtractor graphics, float cx, float cy, RadialNode parent, float parentCenter, int level, Hover hover, boolean facesPass, float openScale) {
		float expand = parent instanceof AbstractRadialNode a ? a.animExpand : 0f;
		if (expand < 0.01f) return;

		List<RadialNode> vis = visibleChildren(parent);
		int k = vis.size();
		if (k == 0) return;

		float t = easeOut(expand);
		float ringInner = (R_OUTER + (level - 1) * CHILD_BAND) * openScale;
		float ringOuter = ringInner + CHILD_BAND * openScale * t;
		float faceRadius = (ringInner + ringOuter) / 2f;
		float half = childDrawnHalfDeg(level);

		for (int j = 0; j < k; j++) {
			RadialNode child = vis.get(j);
			float center = parentCenter + (j - (k - 1) / 2.0f) * childArcDeg(level);
			if (!facesPass) {
				updateAnim(child, hover);
				float highlight = child instanceof AbstractRadialNode a ? a.animHighlight : 0f;
				float hoverAmt = child instanceof AbstractRadialNode a2 ? a2.animScale : 0f;
				float[] color = lerpColor(CHILD_PANEL, CHILD_HOVER, Math.max(highlight * 0.5f, hoverAmt));
				color = new float[]{color[0], color[1], color[2], color[3] * t};
				float grow = 4f * hoverAmt;
				fillSector(graphics, cx, cy, ringInner, ringOuter + grow, center - half, center + half, color);
			} else if (t >= 0.4f) {
				float hoverAmt = child instanceof AbstractRadialNode a ? a.animScale : 0f;
				drawFace(graphics, cx, cy, child, center, faceRadius, ICON_CHILD, hoverAmt, t, childDrawnHalfDeg(level));
			}
			drawChildSectors(graphics, cx, cy, child, center, level + 1, hover, facesPass, openScale);
		}
	}

	/**
	 * Draws a filled annular sector (radial-menu wedge).
	 *
	 * <h2>26.2 migration</h2>
	 * 1.21 emitted a triangle fan through the {@code Tesselator} singleton and pushed it with
	 * {@code BufferUploader}, with blend/cull set by hand on {@code RenderSystem}. All of that is
	 * deleted in 26.2, and {@code GuiGraphicsExtractor} only offers axis-aligned {@code fill} /
	 * {@code fillGradient} / {@code blit} - there is no triangle primitive at all.
	 *
	 * <p>So the fan is rasterised on the CPU instead: every triangle is scanline-filled with one
	 * {@code fill} call per pixel row. For a radial menu with a handful of wedges this costs a few
	 * hundred {@code fill}s per frame, which is well within budget, and the result is pixel-exact
	 * (unlike approximating each wedge with its bounding box).
	 */
	private void fillSector(GuiGraphicsExtractor graphics, float cx, float cy, float rIn, float rOut,
							float startDeg, float endDeg, float[] color) {
		if (rOut <= rIn || color[3] <= 0.01f) return;
		int argb = net.minecraft.util.ARGB.colorFromFloat(color[3], color[0], color[1], color[2]);

		int steps = Math.max(2, (int) Math.ceil(Math.abs(endDeg - startDeg) / 5.0));
		for (int s = 0; s < steps; s++) {
			double a0 = Math.toRadians(startDeg + (endDeg - startDeg) * s / steps);
			double a1 = Math.toRadians(startDeg + (endDeg - startDeg) * (s + 1) / steps);
			float cos0 = (float) Math.cos(a0), sin0 = (float) Math.sin(a0);
			float cos1 = (float) Math.cos(a1), sin1 = (float) Math.sin(a1);

			float ix0 = cx + cos0 * rIn, iy0 = cy + sin0 * rIn;
			float ox0 = cx + cos0 * rOut, oy0 = cy + sin0 * rOut;
			float ix1 = cx + cos1 * rIn, iy1 = cy + sin1 * rIn;
			float ox1 = cx + cos1 * rOut, oy1 = cy + sin1 * rOut;

			// Two triangles form the quad (ix0,ox0,ox1) + (ix0,ox1,ix1).
			fillTriangle(graphics, ix0, iy0, ox0, oy0, ox1, oy1, argb);
			fillTriangle(graphics, ix0, iy0, ox1, oy1, ix1, iy1, argb);
		}
	}

	/** Scanline-fills one triangle with per-row {@code fill} calls. */
	private static void fillTriangle(GuiGraphicsExtractor graphics,
									 float x0, float y0, float x1, float y1, float x2, float y2, int argb) {
		int minY = (int) Math.floor(Math.min(y0, Math.min(y1, y2)));
		int maxY = (int) Math.ceil(Math.max(y0, Math.max(y1, y2)));
		for (int y = minY; y < maxY; y++) {
			float py = y + 0.5f;
			float[] xs = new float[3];
			int n = 0;
			n = addEdgeX(xs, n, x0, y0, x1, y1, py);
			n = addEdgeX(xs, n, x1, y1, x2, y2, py);
			n = addEdgeX(xs, n, x2, y2, x0, y0, py);
			if (n < 2) continue;
			float lo = Math.min(xs[0], xs[1]);
			float hi = Math.max(xs[0], xs[1]);
			int ix0 = (int) Math.floor(lo);
			int ix1 = (int) Math.ceil(hi);
			if (ix1 > ix0) graphics.fill(ix0, y, ix1, y + 1, argb);
		}
	}

	/** Appends the x where the edge (ax,ay)-(bx,by) crosses scanline {@code py}, if it does. */
	private static int addEdgeX(float[] out, int n, float ax, float ay, float bx, float by, float py) {
		if ((ay <= py && by > py) || (by <= py && ay > py)) {
			if (n < out.length) {
				float t = (py - ay) / (by - ay);
				out[n++] = ax + t * (bx - ax);
			}
		}
		return n;
	}

	private void drawFace(GuiGraphicsExtractor graphics, float cx, float cy, RadialNode node, float angleDeg, float radius, float iconBase, float hoverAmt, float alpha, float sectorHalfDeg) {
		double rad = Math.toRadians(angleDeg);
		float x = (float) (cx + Math.cos(rad) * radius);
		float y = (float) (cy + Math.sin(rad) * radius);
		int maxWidth = Math.max(28, Math.round(2f * radius * (float) Math.sin(Math.toRadians(sectorHalfDeg))));
		String faceText = node.faceText(statsData);
		if (faceText != null) drawFaceText(graphics, faceText, x, y - 3f, iconBase, hoverAmt, node.labelColor(statsData), alpha);
		else drawIcon(graphics, node.icon(statsData), x, y - 3f, iconBase, hoverAmt, node.iconTint(statsData), alpha);
		drawLabel(graphics, node, x, y - 3f, iconBase, hoverAmt, alpha, maxWidth);
	}

	private void drawFaceText(GuiGraphicsExtractor graphics, String text, float x, float y, float iconBase, float hoverAmt, int color, float alpha) {
		if (alpha < 0.4f) return;
		float scale = 1.5f + 0.25f * hoverAmt;
		Style style = Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(DMZ_FONT));
		Component line = Component.literal(text).withStyle(style);
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);
		int opacity = Math.round(Mth.clamp(alpha, 0.0f, 1.0f) * 255.0f);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, line, 0, -this.font.lineHeight / 2, ARGB.color(opacity, color), ARGB.color(opacity, 0));
		graphics.pose().popMatrix();
	}

	private void drawIcon(GuiGraphicsExtractor graphics, Identifier icon, float x, float y, float iconBase, float hoverAmt, int tint, float alpha) {
		if (icon == null) return;
		float size = iconBase + 4f * hoverAmt;
		float half = size / 2f;
		Identifier safe = DMZSkinLayer.getSafeTexture(icon, AbstractRadialNode.PLACEHOLDER);

		float r = 1f, g = 1f, b = 1f;
		if (tint >= 0) {
			r = ((tint >> 16) & 0xFF) / 255f;
			g = ((tint >> 8) & 0xFF) / 255f;
			b = (tint & 0xFF) / 255f;
		}

		// 26.2: RenderSystem.setShaderColor is gone. The tint travels as the last argument of the
		// 13-argument blit overload, which is ARGB.
		int blitColor = net.minecraft.util.ARGB.colorFromFloat(alpha, r, g, b);
		graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, safe,
				Math.round(x - half), Math.round(y - half), 0.0F, 0.0F,
				Math.round(size), Math.round(size), 18, 18, 18, 18, blitColor);
	}

	private void drawLabel(GuiGraphicsExtractor graphics, RadialNode node, float x, float y, float iconBase, float hoverAmt, float alpha, int maxWidth) {
		if (alpha < 0.4f) return;
		Component label = node.label(statsData);
		if (label == null || label.getString().isEmpty()) return;

		int opacity = Math.round(Mth.clamp(alpha, 0.0f, 1.0f) * 255.0f);
		int color = ARGB.color(opacity, node.labelColor(statsData));
		Style style = Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(DMZ_FONT));
		List<String> lines = TextUtil.wrap(this.font, label.getString(), maxWidth, style);
		int lineHeight = this.font.lineHeight;
		int startY = Math.round(y + (iconBase + 4f * hoverAmt) / 2f + 2f);
		int centerX = Math.round(x);
		for (int i = 0; i < lines.size(); i++) {
			Component line = Component.literal(lines.get(i)).withStyle(style);
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, line, centerX, startY + i * lineHeight, color, ARGB.color(opacity, 0));
		}
	}

	private void updateAnim(RadialNode node, Hover hover) {
		if (!(node instanceof AbstractRadialNode a)) return;
		boolean hovered = node == hover.deepest;
		boolean inPath = hover.path.contains(node);
		boolean lit = hovered || inPath;
		a.animHighlight = approach(a.animHighlight, lit ? 1f : 0f);
		a.animScale = approach(a.animScale, hovered ? 1f : 0f);
		a.animExpand = approach(a.animExpand, (lit && node.expandable(statsData)) ? 1f : 0f);
	}

	private List<RadialNode> visibleChildren(RadialNode node) {
		List<RadialNode> out = new ArrayList<>();
		for (RadialNode child : node.children(statsData)) {
			if (child.visible(statsData)) out.add(child);
		}
		return out;
	}

	private Hover resolveHover(float cx, float cy, double mouseX, double mouseY, float openScale) {
		Hover result = new Hover();
		if (!Float.isFinite(openScale) || openScale <= 0.0f || baseNodes.isEmpty()) {
			chain.clear();
			hoveredRadialLevel = 0;
			return result;
		}
		double dx = mouseX - cx;
		double dy = mouseY - cy;
		double d = Math.sqrt(dx * dx + dy * dy);
		double ang = Math.toDegrees(Math.atan2(dy, dx));

		float inner = R_INNER * openScale;
		float outer = R_OUTER * openScale;
		float band = CHILD_BAND * openScale;

		if (d < inner) {
			chain.clear();
			hoveredRadialLevel = 0;
			return result;
		}

		int rawLevel = d < outer ? 0 : Math.max(0, (int) (1 + Math.floor((d - outer) / band)));
		float radialHysteresis = Math.min(8.0f * openScale, band * 0.2f);
		int previousLevel = hoveredRadialLevel;
		int level = rawLevel;
		if (rawLevel > previousLevel) {
			float enterBoundary = outer + previousLevel * band;
			if (d < enterBoundary + radialHysteresis) level = previousLevel;
		} else if (rawLevel < previousLevel) {
			float leaveBoundary = outer + (previousLevel - 1) * band;
			if (d > leaveBoundary - radialHysteresis) level = previousLevel;
		}
		hoveredRadialLevel = level;

		if (level == 0) {
			int slot = baseSlotAt(ang);
			RadialNode node = baseNodes.get(slot);
			chain.clear();
			if (!isActiveSlot(node)) {
				hoveredRadialLevel = 0;
				return result;
			}
			chain.add(node);
			result.deepest = node;
			result.path.add(node);
			result.deepestAngleDeg = baseCenter(slot);
			result.deepestLevel = 0;
			return result;
		}

		if (chain.isEmpty()) {
			int slot = baseSlotAt(ang);
			RadialNode node = baseNodes.get(slot);
			if (!isActiveSlot(node)) {
				hoveredRadialLevel = 0;
				return result;
			}
			chain.add(node);
		}
		while (chain.size() > level + 1) chain.remove(chain.size() - 1);
		if (chain.isEmpty()) {
			hoveredRadialLevel = 0;
			return result;
		}

		RadialNode base = chain.get(0);
		result.path.add(base);
		result.deepest = base;
		result.deepestAngleDeg = baseCenter(baseNodes.indexOf(base));
		result.deepestLevel = 0;

		float center = baseCenter(baseNodes.indexOf(base));
		RadialNode current = base;
		for (int l = 1; l <= level; l++) {
			if (!current.expandable(statsData)) break;
			List<RadialNode> vis = visibleChildren(current);
			int k = vis.size();
			if (k == 0) break;

			RadialNode previous = l < chain.size() ? chain.get(l) : null;
			int previousIndex = previous == null ? -1 : vis.indexOf(previous);
			int idx;
			if (l == level) {
				idx = pickChildByAngle(vis, center, ang, l, previousIndex);
				if (idx < 0) break;
				if (l < chain.size()) chain.set(l, vis.get(idx));
				else chain.add(vis.get(idx));
			} else {
				// Deeper rings can extend beyond the parent wedge; keep the chosen path
				// until the pointer returns to that depth, or the child tree changes.
				idx = previousIndex;
				if (idx < 0) {
					idx = pickChildByAngle(vis, center, ang, l, -1);
					if (idx < 0) break;
					if (l < chain.size()) chain.set(l, vis.get(idx));
					else chain.add(vis.get(idx));
				}
			}

			RadialNode child = vis.get(idx);
			center = center + (idx - (k - 1) / 2.0f) * childArcDeg(l);
			result.path.add(child);
			result.deepest = child;
			result.deepestAngleDeg = center;
			result.deepestLevel = l;
			current = child;
		}

		while (chain.size() > result.path.size()) chain.remove(chain.size() - 1);
		return result;
	}

	private int pickChildByAngle(List<RadialNode> children, float parentCenter, double ang, int level, int previousIndex) {
		int k = children.size();
		float arc = childArcDeg(level);
		if (previousIndex >= 0 && previousIndex < k) {
			float previousCenter = parentCenter + (previousIndex - (k - 1) / 2.0f) * arc;
			float previousDiff = (float) absAngleDiff(ang, previousCenter);
			if (previousDiff <= arc / 2f + CHILD_ANGLE_HYSTERESIS_DEG) return previousIndex;
		}
		float best = arc / 2f;
		int bestIdx = -1;
		for (int j = 0; j < k; j++) {
			float center = parentCenter + (j - (k - 1) / 2.0f) * arc;
			float diff = (float) absAngleDiff(ang, center);
			if (diff < best) {
				best = diff;
				bestIdx = j;
			}
		}
		return bestIdx;
	}

	private static float baseArcLength() {
		return (R_INNER + R_OUTER) / 2f * (float) Math.toRadians(360f / SLOTS);
	}

	private static float childArcDeg(int level) {
		float midRadius = R_OUTER + (level - 1) * CHILD_BAND + CHILD_BAND / 2f;
		float fullDeg = (float) Math.toDegrees(1.25f * baseArcLength() / midRadius);
		return Math.min(fullDeg, 360f / SLOTS);
	}

	private static float childDrawnHalfDeg(int level) {
		return childArcDeg(level) / 2f - SECTOR_GAP_DEG / 2f;
	}

	private static int baseSlotAt(double angleDeg) {
		return Math.floorMod(Math.round((float) ((angleDeg + 90.0) / 45.0)), SLOTS);
	}

	private static float baseCenter(int index) {
		return -90f + index * (360f / SLOTS);
	}

	private static double absAngleDiff(double a, double b) {
		double d = ((a - b) % 360.0 + 540.0) % 360.0 - 180.0;
		return Math.abs(d);
	}

	private float approach(float current, float target) {
		float step = Math.min(1.0f, frameDt * ANIM_SPEED);
		return current + (target - current) * step;
	}

	private static float easeOut(float t) {
		float inv = 1.0f - t;
		return 1.0f - inv * inv * inv;
	}

	private static float[] lerpColor(float[] a, float[] b, float t) {
		t = Math.max(0f, Math.min(1f, t));
		return new float[]{
				a[0] + (b[0] - a[0]) * t,
				a[1] + (b[1] - a[1]) * t,
				a[2] + (b[2] - a[2]) * t,
				a[3] + (b[3] - a[3]) * t
		};
	}

	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
		if (closing || statsData == null) return true;
		double ux = toUiX(mouseX), uy = toUiY(mouseY);
		float cx = getUiWidth() / 2f, cy = getUiHeight() / 2f;

		if (panelOptions != null) {
			int[] b = panelBounds(cx, cy);
			boolean inside = ux >= b[0] && ux <= b[0] + b[2] && uy >= b[1] && uy <= b[1] + b[3];
			if (!inside) {
				closePanel();
				return true;
			}
			int rowsTop = b[1] + PANEL_TITLE_H;
			if (panelScrollable) {
				if (panelBar.tryStartDrag(ux, uy)) {
					panelScroll = Math.round(panelBar.scrollFor(uy));
					return true;
				}
				int rel = rowIndexAt(rowsTop, uy);
				int i = panelScroll + rel;
				if (rel >= 0 && rel < visiblePanelRows() && i < panelOptions.size()) {
					selectNode(panelOptions.get(i));
				}
				return true;
			}
			int idx = rowIndexAt(rowsTop, uy);
			if (idx >= 0 && idx < panelOptions.size()) {
				dragIndex = idx;
				dragStartY = uy;
				dragCurrentY = uy;
				dragging = false;
			}
			return true;
		}

		Hover hover = resolveHover(cx, cy, ux, uy, computeOpenScale());
		RadialNode node = hover.deepest;
		if (node instanceof MoreNode more) {
			openPanel(more.options(), Component.translatable("gui.dragonminez.radial.options"), hover, false);
			openMore = more;
			return true;
		}
		if (node instanceof FlyNode fly && fly.interactive(statsData)) {
			fly.onSelect(statsData);
			return true;
		}
		if (node instanceof ReleaseNode release) {
			if (release.active(statsData)) {
				release.onSelect(statsData);
				return true;
			}
			openPanel(release.buildOptions(statsData), release.label(statsData), hover, true);
			return true;
		}
		if (node instanceof FlightSpeedNode flightSpeed) {
			if (flightSpeed.active(statsData)) {
				flightSpeed.onSelect(statsData);
				return true;
			}
			openPanel(flightSpeed.buildOptions(), flightSpeed.label(statsData), hover, true);
			return true;
		}
		if (node instanceof FormSelectNode form && form.interactive(statsData)) {
			selectNode(form);
			return true;
		}
		if (node != null && node.interactive(statsData) && !node.expandable(statsData)) {
			node.onSelect(statsData);
			return true;
		}
		return true;
	}

	private void openPanel(List<RadialNode> options, Component title, Hover hover, boolean scrollable) {
		panelOptions = options;
		panelTitle = title;
		panelScrollable = scrollable;
		panelScroll = 0;
		panelAngleDeg = hover.deepestAngleDeg;
		panelLevel = hover.deepestLevel;
		frozenHover = hover;
	}

	private void selectNode(RadialNode node) {
		if (node == null || !node.interactive(statsData)) return;
		long now = System.currentTimeMillis();
		boolean doubleClick = node == lastClickNode && (now - lastClickMs) <= DOUBLE_CLICK_MS;
		lastClickNode = node;
		lastClickMs = doubleClick ? 0L : now;
		if (doubleClick) node.onDoubleSelect(statsData);
		else node.onSelect(statsData);
	}

	private void closePanel() {
		panelOptions = null;
		panelTitle = null;
		panelScrollable = false;
		panelScroll = 0;
		openMore = null;
		dragIndex = -1;
		dragging = false;
		panelBar.stopDrag();
	}

	@Override
	public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
		if (panelBar.isDragging()) {
			panelScroll = Math.round(panelBar.scrollFor(toUiY(mouseY)));
			return true;
		}
		if (panelOptions != null && !panelScrollable && dragIndex >= 0) {
			double uy = toUiY(mouseY);
			if (Math.abs(uy - dragStartY) > 3) dragging = true;
			dragCurrentY = uy;
			return true;
		}
		return true;
	}

	@Override
	public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
		if (panelBar.isDragging()) {
			panelBar.stopDrag();
			return true;
		}
		if (panelOptions != null && !panelScrollable && dragIndex >= 0) {
			double uy = toUiY(mouseY);
			if (!dragging) {
				selectNode(panelOptions.get(dragIndex));
			} else {
				int[] b = panelBounds(getUiWidth() / 2f, getUiHeight() / 2f);
				int rowsTop = b[1] + PANEL_TITLE_H;
				int target = Mth.clamp(rowIndexAt(rowsTop, uy), 0, panelOptions.size());
				reorderOption(dragIndex, target);
			}
			dragIndex = -1;
			dragging = false;
			return true;
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (statsData != null && panelOptions == null) {
			Hover hover = resolveHover(getUiWidth() / 2f, getUiHeight() / 2f,
					toUiX(mouseX), toUiY(mouseY), computeOpenScale());
			if (hover.deepest instanceof FlightSpeedNode flightSpeed) {
				flightSpeed.adjustByWheel(statsData, scrollY);
				return true;
			}
		}
		if (panelOptions != null && panelScrollable) {
			int maxScroll = Math.max(0, panelOptions.size() - visiblePanelRows());
			panelScroll = Mth.clamp(panelScroll - (int) Math.signum(scrollY), 0, maxScroll);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		if (event.key() == KeyBinds.UTILITY_MENU.getKey().getValue()) return true;
		if (event.key() == 256) {
			if (panelOptions != null) {
				closePanel();
				return true;
			}
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void tick() {
		super.tick();
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> this.statsData = data);
		}
		if (closing && System.currentTimeMillis() - closeStartTime >= ANIMATION_DURATION) forceClose();
	}

	@Override
	public void onClose() {
		startClosingAnimation();
	}

	public void startClosingAnimation() {
		if (closing) return;
		closing = true;
		closeStartTime = System.currentTimeMillis();
	}

	private void forceClose() {
		if (this.minecraft != null) this.minecraft.gui.setScreen(null);
	}

	public static boolean isUtilityMenuReopenBlocked() {
		return System.currentTimeMillis() < utilityMenuReopenBlockedUntilMs;
	}

	public static void initMenuSlots() {
	}

	public static void addMenuSlot(IUtilityMenuSlot menuSlot) {
		ADDON_SLOTS.add(menuSlot);
	}

	private static final class Hover {
		private final List<RadialNode> path = new ArrayList<>();
		private RadialNode deepest = null;
		private float deepestAngleDeg = 0f;
		private int deepestLevel = 0;
	}
}
