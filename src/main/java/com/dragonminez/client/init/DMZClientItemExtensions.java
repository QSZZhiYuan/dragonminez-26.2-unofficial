package com.dragonminez.client.init;

import com.dragonminez.Reference;
import com.dragonminez.client.util.ArmorTextureResolver;
import com.dragonminez.common.init.MainFluids;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.armor.DbzArmorCapeItem;
import com.dragonminez.common.init.armor.DbzArmorItem;
import com.dragonminez.common.init.armor.DbzArmorTextured;
import com.dragonminez.common.init.armor.client.model.ArmorBaseModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * NeoForge 1.21 replaced {@code Item#initializeClient} with
 * {@link RegisterClientExtensionsEvent}. Register custom humanoid armor models
 * for all DBZ armor pieces so armor stands (Goku house displays) and vanilla
 * armor layers use {@link ArmorBaseModel}.
 */
@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class DMZClientItemExtensions {
	private DMZClientItemExtensions() {}

    @SubscribeEvent
    public static void onRegisterFluidModels(net.neoforged.neoforge.client.event.RegisterFluidModelsEvent event) {
        event.register(((com.dragonminez.common.init.fluid.SimpleFluid) MainFluids.HEALING_FLUID_TYPE.get()).createClientModel(),
                MainFluids.SOURCE_HEALING, MainFluids.FLOWING_HEALING);
        event.register(((com.dragonminez.common.init.fluid.SimpleFluid) MainFluids.NAMEK_FLUID_TYPE.get()).createClientModel(),
                MainFluids.SOURCE_NAMEK, MainFluids.FLOWING_NAMEK);
    }

	private static final IClientItemExtensions DBZ_ARMOR_EXTENSIONS = new IClientItemExtensions() {
		private ArmorBaseModel model;

		@Override
		@NotNull
		// 26.2: signature became (ItemStack, EquipmentClientInfo.LayerType, Model).
		public net.minecraft.client.model.Model getHumanoidArmorModel(ItemStack itemStack,
				net.minecraft.client.resources.model.EquipmentClientInfo.LayerType layerType,
				net.minecraft.client.model.Model original) {
			if (model == null) {
				model = new ArmorBaseModel(Minecraft.getInstance().getEntityModels().bakeLayer(ArmorBaseModel.LAYER_LOCATION));
			}
			return model;
		}

		@Override
		public Identifier getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type,
				EquipmentClientInfo.Layer layer, Identifier defaultTexture) {
			if (!(stack.getItem() instanceof DbzArmorTextured armor)) return defaultTexture;
			EquipmentSlot slot = type == EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS
					? EquipmentSlot.LEGS : EquipmentSlot.CHEST;
			return ArmorTextureResolver.resolve(Reference.MOD_ID, armor.getItemId(), slot, stack);
		}
	};

	@SubscribeEvent
	public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
		List<Item> armorItems = new ArrayList<>();
		for (DeferredHolder<Item, ? extends Item> holder : MainItems.ITEM_REGISTER.getEntries()) {
			Item item = holder.get();
			if (item instanceof DbzArmorItem || item instanceof DbzArmorCapeItem) {
				armorItems.add(item);
			}
		}

		if (!armorItems.isEmpty()) {
			event.registerItem(DBZ_ARMOR_EXTENSIONS, armorItems.toArray(Item[]::new));
		}

		// 26.2: FluidType#initializeClient was removed, so the DMZ fluids register their fog
		// behaviour here instead.
		registerFluidExtensions(event, MainFluids.HEALING_FLUID_TYPE);
		registerFluidExtensions(event, MainFluids.NAMEK_FLUID_TYPE);
	}

	private static void registerFluidExtensions(RegisterClientExtensionsEvent event,
												DeferredHolder<net.neoforged.neoforge.fluids.FluidType, net.neoforged.neoforge.fluids.FluidType> holder) {
		if (holder.get() instanceof com.dragonminez.common.init.fluid.SimpleFluid fluid) {
			fluid.registerClientExtensions(event);
		}
	}
}

