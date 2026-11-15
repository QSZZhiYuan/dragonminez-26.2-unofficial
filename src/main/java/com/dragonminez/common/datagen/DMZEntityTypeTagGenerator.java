package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class DMZEntityTypeTagGenerator extends EntityTypeTagsProvider {
	public DMZEntityTypeTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
		super(output, lookupProvider, Reference.MOD_ID);
	}

	@Override
	protected void addTags(HolderLookup.@NotNull Provider provider) {
		this.tag(MainTags.EntityTypes.FRIEZA_SOLDIERS)
				.add(MainEntities.SAGA_FRIEZA_SOLDIER.getKey())
				.add(MainEntities.SAGA_FRIEZA_SOLDIER2.getKey())
				.add(MainEntities.SAGA_FRIEZA_SOLDIER3.getKey());

		this.tag(MainTags.EntityTypes.SAIBAMEN)
				.add(MainEntities.SAGA_SAIBAMAN.getKey())
				.add(MainEntities.SAGA_SAIBAMAN2.getKey())
				.add(MainEntities.SAGA_SAIBAMAN3.getKey())
				.add(MainEntities.SAGA_SAIBAMAN4.getKey())
				.add(MainEntities.SAGA_SAIBAMAN5.getKey())
				.add(MainEntities.SAGA_SAIBAMAN6.getKey());

		this.tag(MainTags.EntityTypes.RED_RIBBON_ROBOTS)
				.add(MainEntities.RED_RIBBON_ROBOT1.getKey())
				.add(MainEntities.RED_RIBBON_ROBOT2.getKey())
				.add(MainEntities.RED_RIBBON_ROBOT3.getKey());
	}
}

