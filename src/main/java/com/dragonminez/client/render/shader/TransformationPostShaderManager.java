package com.dragonminez.client.render.shader;

import com.dragonminez.client.util.ColorUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.player.Player;
import javax.annotation.Nullable;

/** Compatibility facade; outlines use dedicated targets and never select a GameRenderer post effect. */
public final class TransformationPostShaderManager {
    private static boolean shaderpackMainPass;
    private static final TransformationMaskBufferSource LEGACY_COLLECTOR = new TransformationMaskBufferSource();
    private TransformationPostShaderManager() {}
    public static void tick() {}
    public static void reset() { TransformationOutlineRenderer.reset(); }
    public static void setShaderpackMainPass(boolean active) { shaderpackMainPass = active; }
    public static boolean isShaderpackMainPass() { return shaderpackMainPass; }
    public static TransformationMaskBufferSource getMaskBufferSource() { return LEGACY_COLLECTOR; }
    public static void flushMaskAndApplyUniforms(float partialTick, PoseStack pose) {}
    public static void processShaderpackOutline(float partialTick) {}
    @Nullable public static MaskData getEntityMaskData(Player player) {
        var config = TransformationOutlineRenderer.config(player);
        if (config == null) return null;
        var a = ColorUtils.hexToRgb(config.getPrimaryColor());
        var b = ColorUtils.hexToRgb(config.getSecondaryColor());
        return new MaskData(a[0], a[1], a[2], b[0], b[1], b[2]);
    }
    public record MaskData(float primaryR, float primaryG, float primaryB, float secondaryR, float secondaryG, float secondaryB) {}
}
