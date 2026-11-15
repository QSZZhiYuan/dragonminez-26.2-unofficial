package com.dragonminez.common.init.item.entities;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.PunchMachineEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class PunchMachineItem extends Item {
	public PunchMachineItem( ) {
		this(new net.minecraft.world.item.Item.Properties().stacksTo(1));
	}

	/** 26.2: the registry id must be on the properties before super() runs. */
	public PunchMachineItem(net.minecraft.world.item.Item.Properties properties) {
		super(properties);
	}

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Player player = pContext.getPlayer();
        Level level = pContext.getLevel();
        BlockPos pos = pContext.getClickedPos();
        Direction direction = pContext.getClickedFace();

        // Posición centrada sobre el bloque
        BlockPos spawnPos = pos.above();
        double x = spawnPos.getX() + 0.5;
        double y = spawnPos.getY();
        double z = spawnPos.getZ() + 0.5;

        if (player != null && level != null) {
            PunchMachineEntity machine = new PunchMachineEntity(MainEntities.PUNCH_MACHINE.get(), level);

            machine.setPos(x, y, z);
            float rot = player.getYRot() + 180.0F;

            machine.setYRot(rot);
            machine.setYHeadRot(rot);
            machine.setYBodyRot(rot);
            machine.yRotO = rot;
            level.addFreshEntity(machine);

            if (!player.isCreative()) {
                pContext.getItemInHand().shrink(1);
            }

            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        }

        return super.useOn(pContext);
    }
}

