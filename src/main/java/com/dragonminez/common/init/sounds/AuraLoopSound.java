package com.dragonminez.common.init.sounds;

import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public class AuraLoopSound extends AbstractTickableSoundInstance {
    private final Player player;

    public AuraLoopSound(Player player) {
        super(MainSounds.KI_CHARGE_LOOP.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());

        this.player = player;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.5F;
        this.pitch = 1.0F;
        this.relative = player == net.minecraft.client.Minecraft.getInstance().player;
        if (this.relative) this.attenuation = SoundInstance.Attenuation.NONE;

        updatePosition();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (this.player.isRemoved()) {
            this.stop();
            return;
        }

        var stats = StatsProvider.get(StatsCapability.INSTANCE, this.player).orElse(null);

        if (stats == null || !(stats.getStatus().isAuraActive() || stats.getStatus().isPermanentAura())) {
            this.stop();
            return;
        }

        updatePosition();

        // this.pitch = 1.0f + (stats.getCharacter().getPowerLevel() / 10000f);
    }

    private void updatePosition() {
        this.x = relative ? 0 : player.getX();
        this.y = relative ? 0 : player.getY();
        this.z = relative ? 0 : player.getZ();
    }
}
