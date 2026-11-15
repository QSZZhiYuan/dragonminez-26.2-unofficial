package com.dragonminez.client.render;

import com.dragonminez.common.stats.StatsData;
import net.minecraft.client.player.AbstractClientPlayer;
import com.geckolib.constant.dataticket.DataTicket;

/**
 * Per-frame data handed from {@link DMZPlayerRenderer#addRenderData} to the render layers.
 *
 * <p><b>Why this exists</b> — Minecraft 26.2 + GeckoLib 5 changed the layer contract:
 * {@code GeoRenderLayer#preRender(RenderPassInfo, SubmitNodeCollector)} no longer receives the
 * animatable object. {@code GeoRenderer#addRenderData(T animatable, O, R renderState, float)} is
 * the only hook that still gets the animatable, so the player-derived data every DMZ layer needs
 * is captured there and stashed on the render state via {@link #TICKET}.
 *
 * <p>Layers then do:
 * <pre>{@code
 * DMZPlayerRenderData data = info.renderState().getGeckolibData(DMZPlayerRenderData.TICKET);
 * if (data == null) return;
 * StatsData stats = data.stats();
 * }</pre>
 *
 * <p>Keep this immutable — render states are extracted once per frame and read many times.
 */
public record DMZPlayerRenderData(
        StatsData stats,
        AbstractClientPlayer player,
        boolean spectator,
        boolean hudPortrait,
        int entityId,
        long gameTime
) {
    /** Ticket used to store/retrieve this record on a {@code GeoRenderState}. */
    public static final DataTicket<DMZPlayerRenderData> TICKET =
            DataTicket.create("dmz_player_render_data", DMZPlayerRenderData.class);

    public static DMZPlayerRenderData of(AbstractClientPlayer player, StatsData stats,
                                        boolean hudPortrait, int entityId, long gameTime) {
        return new DMZPlayerRenderData(stats, player, player.isSpectator(), hudPortrait, entityId, gameTime);
    }
}
