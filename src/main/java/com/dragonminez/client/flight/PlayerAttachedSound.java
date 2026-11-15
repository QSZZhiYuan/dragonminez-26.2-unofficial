package com.dragonminez.client.flight;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/** Keeps the local player's sounds at the listener; other players remain positional. */
public final class PlayerAttachedSound extends EntityBoundSoundInstance {
    public PlayerAttachedSound(SoundEvent event, Player player, float volume, float pitch) {
        super(event, SoundSource.PLAYERS, volume, pitch, player, player.getRandom().nextLong());
        relative = player == Minecraft.getInstance().player;
        if (relative) {
            attenuation = Attenuation.NONE;
            x = y = z = 0;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (relative) x = y = z = 0;
    }
}
