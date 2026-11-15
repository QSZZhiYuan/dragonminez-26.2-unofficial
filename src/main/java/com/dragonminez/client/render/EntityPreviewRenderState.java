package com.dragonminez.client.render;

import com.dragonminez.common.stats.character.Character;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3x2fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Independent appearance and orientation for each carousel/grid preview in a frame. */
public record EntityPreviewRenderState(
        LivingEntity entity, EntityRenderState renderState, Character appearance, boolean hudPortrait, boolean formPreview,
        Vector3fc translation, Quaternionfc rotation, Quaternionfc cameraOrientation,
        int x0, int y0, int x1, int y1, float scale,
        ScreenRectangle scissorArea, ScreenRectangle bounds) implements PictureInPictureRenderState {

    public static EntityPreviewRenderState create(GuiGraphicsExtractor graphics, LivingEntity entity,
            EntityRenderState state, Character appearance, boolean hudPortrait, int x, int y, float scale,
            Vector3f translation, Quaternionf rotation, Quaternionf cameraOrientation) {
        Layout layout = layout(graphics.pose(), x, y, scale, state.boundingBoxWidth, state.boundingBoxHeight);
        Vector3f offset = new Vector3f(translation);
        offset.y -= layout.bottomPadding() / layout.scale();
        ScreenRectangle scissor = graphics.peekScissorStack();
        return new EntityPreviewRenderState(entity, state, appearance, hudPortrait,
                com.dragonminez.client.render.layer.DMZSkinLayer.PREVIEW_MODE, offset,
                rotation == null ? new Quaternionf().rotateZ((float) Math.PI) : new Quaternionf(rotation),
                cameraOrientation == null ? null : new Quaternionf(cameraOrientation),
                layout.x0(), layout.y0(), layout.x1(), layout.y1(), layout.scale(), scissor,
                PictureInPictureRenderState.getBounds(layout.x0(), layout.y0(), layout.x1(), layout.y1(), scissor));
    }

    // GuiGraphicsExtractor.entity ignores the current 2D pose. Bake it into bounds and texture size
    // so scaled menus keep the same position, hit areas, and native pixel density.
    static Layout layout(Matrix3x2fc pose, float x, float y, float scale, float width, float height) {
        Vector2f anchor = pose.transformPosition(x, y, new Vector2f());
        float uiScale = (float) Math.sqrt(pose.m10() * pose.m10() + pose.m11() * pose.m11());
        float renderedScale = scale * uiScale;
        int padding = Math.max(2, Math.round(4.0F * uiScale));
        int halfWidth = (int) Math.ceil(Math.max(1.6F, width * 1.6F) * renderedScale / 2.0F) + padding;
        int modelHeight = (int) Math.ceil(Math.max(3.0F, height + 0.8F) * renderedScale);
        int baseX = Math.round(anchor.x);
        int baseY = Math.round(anchor.y);
        return new Layout(baseX - halfWidth, baseY - modelHeight - padding,
                baseX + halfWidth, baseY + padding, renderedScale, padding);
    }

    record Layout(int x0, int y0, int x1, int y1, float scale, int bottomPadding) {}
}
