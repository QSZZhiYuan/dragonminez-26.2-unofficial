package com.dragonminez.common.init.item.weapons;

import com.dragonminez.common.init.entities.ki.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MerusLaserItem extends Item {
	public MerusLaserItem( ) {
		this(new net.minecraft.world.item.Item.Properties().stacksTo(1).durability(250));
	}

	/** 26.2: the registry id must be on the properties before super() runs. */
	public MerusLaserItem(net.minecraft.world.item.Item.Properties properties) {
		super(properties);
	}
    @Override
    public InteractionResult use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);

        if (!pLevel.isClientSide()) {

            KiBlastEntity kiBlast = new KiBlastEntity(pLevel, pPlayer);

            kiBlast.setupKiBlast(pPlayer, 10.0f,  2.5f, 0x00FFFF, 0x78FFFF, 1.0f, 5);

            //kiBlast.shootFromRotation(pPlayer, pPlayer.getXRot(), pPlayer.getYRot(), 0.0F, 1.5f, 0.5F);
            itemstack.hurtAndBreak(1, pPlayer, pHand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }


        pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 2.0F);

        pPlayer.getCooldowns().addCooldown(new ItemStack(this), 30);

        return pLevel.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

}


