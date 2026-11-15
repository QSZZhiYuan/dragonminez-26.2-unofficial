package com.dragonminez.client.render;

import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Render state for animated skill projectiles, which are not living entities. */
public class GeoKiRenderState extends EntityRenderState implements GeoRenderState {
    public float lockedYaw;
    public float lockedPitch;
}
