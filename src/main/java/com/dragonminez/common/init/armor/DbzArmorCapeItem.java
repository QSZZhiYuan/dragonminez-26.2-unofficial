package com.dragonminez.common.init.armor;

import com.dragonminez.Reference;
import net.minecraft.core.Holder;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

/**
 * Geo-rendered DBZ cape armor piece.
 *
 * <p>26.2 note: vanilla {@code ArmorItem} was removed together with the
 * {@code getArmorTexture} hook. Armor behaviour now lives in the
 * {@code EQUIPPABLE} data component installed by
 * {@code Item.Properties.humanoidArmor}.
 */
public class DbzArmorCapeItem extends Item implements GeoItem, DbzArmorTextured {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String modId;
    private final String itemId;

    public DbzArmorCapeItem(ArmorMaterial pMaterial, ArmorType pType, Properties pProperties, String itemId) {
        this(pMaterial, pType, pProperties, Reference.MOD_ID, itemId);
    }

    public DbzArmorCapeItem(ArmorMaterial pMaterial, ArmorType pType, Properties pProperties, String modId, String itemId) {
        super(pProperties.humanoidArmor(pMaterial, pType));
        this.modId = modId;
        this.itemId = itemId;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("controller", 5, event -> {
            if (event.isMoving()) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            }
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @java.lang.SuppressWarnings("all")
    public String getModId() {
        return this.modId;
    }

    @java.lang.SuppressWarnings("all")
    public String getItemId() {
        return this.itemId;
    }
}
