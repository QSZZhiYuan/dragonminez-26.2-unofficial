package com.dragonminez.common.init.item.weapons;

import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BlasterCannonItem extends Item {
	public BlasterCannonItem( ) {
		this(new net.minecraft.world.item.Item.Properties().stacksTo(1).durability(200));
	}

	/** 26.2: the registry id must be on the properties before super() runs. */
	public BlasterCannonItem(net.minecraft.world.item.Item.Properties properties) {
		super(properties);
	}

    @Override
    public InteractionResult use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);

        if (!pLevel.isClientSide()) {

            KiBlastEntity kiBlast = new KiBlastEntity(pLevel, pPlayer);
            kiBlast.setupKiBlast(pPlayer, 10.0f,  1.0f, 0xFF5E7C, 0x940404, 1.0f, 5);
//            kiBlast.shootFromRotation(pPlayer, pPlayer.getXRot(), pPlayer.getYRot(), 0.0F, kiBlast.getKiSpeed(), 0.5F);

            pLevel.addFreshEntity(kiBlast);
            itemstack.hurtAndBreak(1, pPlayer, pHand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }


        pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 2.0F);

        pPlayer.getCooldowns().addCooldown(new ItemStack(this), 60);

        return pLevel.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }
}


