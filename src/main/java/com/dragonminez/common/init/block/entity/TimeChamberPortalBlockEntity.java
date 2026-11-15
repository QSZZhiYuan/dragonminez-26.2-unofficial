package com.dragonminez.common.init.block.entity;

import com.dragonminez.common.init.MainBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TimeChamberPortalBlockEntity extends BlockEntity {
	private BlockPos cachedTargetPos = null;

	public TimeChamberPortalBlockEntity(BlockPos pPos, BlockState pBlockState) {
		super(MainBlockEntities.TIME_CHAMBER_PORTAL.get(), pPos, pBlockState);
	}

	public boolean hasCachedTarget() {
		return cachedTargetPos != null;
	}

	public BlockPos getCachedTarget() {
		return cachedTargetPos;
	}

    public void setCachedTarget(BlockPos target) {
        this.cachedTargetPos = target;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput pTag) {
        super.loadAdditional(pTag);
        {
            // 26.2: NbtUtils#readBlockPos is gone - the BlockPos codec round-trips the same tag shape.`r`n            this.cachedTargetPos = pTag.read("TargetPos", net.minecraft.core.BlockPos.CODEC).orElse(null);
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput pTag) {
        super.saveAdditional(pTag);
        if (this.cachedTargetPos != null) {
            pTag.store("TargetPos", net.minecraft.core.BlockPos.CODEC, this.cachedTargetPos);
        }
    }
}
