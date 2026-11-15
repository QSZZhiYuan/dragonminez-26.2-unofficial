package com.dragonminez.client.init.blocks.model;

import com.dragonminez.Reference;
import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonBallSetAssetDefinition;
import com.dragonminez.common.init.block.entity.DragonBallBlockEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

/**
 * Asset selection for the dragon-ball blocks.
 *
 * <h2>GeckoLib 5 migration</h2>
 * {@code getModelResource} / {@code getTextureResource} now receive a {@link GeoRenderState} instead of
 * the block entity, so everything that depended on the entity instance has to be resolved while the
 * entity is still reachable. {@link #addAdditionalStateData} is that hook - it runs during render-state
 * extraction with the live block entity, so the resolved asset definition and the star/namekian flags
 * are stashed there and read back at model/texture time.
 *
 * <p>{@link #getAnimationResource(Object)} still receives the entity, so it is unchanged.
 */
public class DragonBallBlockModel extends GeoModel<DragonBallBlockEntity> {

    private static final DataTicket<DragonBallSetAssetDefinition> DMZ_BALL_ASSETS =
            DataTicket.create("dmz_ball_assets", DragonBallSetAssetDefinition.class);
    private static final DataTicket<Boolean> DMZ_BALL_NAMEKIAN =
            DataTicket.create("dmz_ball_namekian", Boolean.class);
    private static final DataTicket<Integer> DMZ_BALL_STARS =
            DataTicket.create("dmz_ball_stars", Integer.class);

    @Override
    public void addAdditionalStateData(DragonBallBlockEntity animatable, Object unused, GeoRenderState renderState) {
        var set = DragonBallDefinitions.getBallSet(animatable.getBallSetId());
        renderState.addGeckolibData(DMZ_BALL_ASSETS, set != null ? set.resolveAssetDefinition() : null);
        renderState.addGeckolibData(DMZ_BALL_NAMEKIAN, animatable.isNamekian());
        renderState.addGeckolibData(DMZ_BALL_STARS,
                animatable.getBallType() != null ? animatable.getBallType().getStars() : 1);
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        DragonBallSetAssetDefinition assets = renderState.getGeckolibData(DMZ_BALL_ASSETS);
        if (assets != null && assets.getGeoModelPath().isPresent()) {
            return dmz$assetId(assets.getGeoModelPath().get());
        }
        boolean namekian = Boolean.TRUE.equals(renderState.getGeckolibData(DMZ_BALL_NAMEKIAN));
        String modelName = namekian ? "dballnamek" : "dball";
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "block/" + modelName );
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        DragonBallSetAssetDefinition assets = renderState.getGeckolibData(DMZ_BALL_ASSETS);
        Integer stars = renderState.getGeckolibData(DMZ_BALL_STARS);
        int starCount = stars != null ? stars : 1;

        if (assets != null && assets.getGeoTexturePathForStar(starCount).isPresent()) {
            return dmz$assetId(assets.getGeoTexturePathForStar(starCount).get());
        }
        boolean namekian = Boolean.TRUE.equals(renderState.getGeckolibData(DMZ_BALL_NAMEKIAN));
        String prefix = namekian ? "dballnamekblock" : "dballblock";
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID,
                "textures/block/custom/" + prefix + starCount + ".png");
    }

    @Override
    public Identifier getAnimationResource(DragonBallBlockEntity blockEntity) {
        DragonBallSetAssetDefinition assets = DragonBallDefinitions.getBallSet(blockEntity.getBallSetId()) != null
            ? DragonBallDefinitions.getBallSet(blockEntity.getBallSetId()).resolveAssetDefinition()
            : null;
        if (assets != null && assets.getAnimationPath().isPresent()) {
            return dmz$assetId(assets.getAnimationPath().get());
        }
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "block/dball");
    }

	/**
	 * Resolves a configured asset path, defaulting the namespace to this mod.
	 *
	 * <p>The {@code geo_model} / animation / texture paths in a ball-set definition are written
	 * without a namespace ({@code "block/dball"}), and {@link Identifier#parse} would then hand them
	 * the vanilla {@code minecraft} namespace. GeckoLib consequently reported
	 * {@code Unable to find model: minecraft:block/dball} for an asset that was packaged correctly at
	 * {@code assets/dragonminez/geckolib/models/block/dball.geo.json}.
	 */
	private static Identifier dmz$assetId(String configured) {
		if (configured == null || configured.isBlank()) {
			return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "block/dball");
		}
		return configured.indexOf(':') >= 0
				? Identifier.parse(configured)
				: Identifier.fromNamespaceAndPath(Reference.MOD_ID, configured);
	}
}
