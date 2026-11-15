package com.dragonminez.common.combat.logic.weapon;

import com.dragonminez.common.combat.util.PatternMatching;
import com.dragonminez.common.config.CombatConfig.CompatibilitySpecifier;
import com.dragonminez.common.config.ConfigManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;

public class WeaponAttributesFallback {
    public static void initialize() {
        var config = ConfigManager.getCombatConfig();
        for (var itemId : BuiltInRegistries.ITEM.keySet()) {
            var itemRef = BuiltInRegistries.ITEM.get(itemId);
            if (itemRef.isEmpty()) continue;
            var item = itemRef.get().value();
            if (PatternMatching.matches(itemId.toString(), config.getBlacklistItemIdRegex())) continue;

            List<CompatibilitySpecifier> specifiers = null;
            if (hasAttributeModifier(item, Attributes.ATTACK_DAMAGE)) specifiers = config.getFallbackCompatibility();
            if (specifiers == null) continue;

            for (var fallbackOption : specifiers) {
                if (WeaponRegistry.getAttributes(itemId) == null && PatternMatching.matches(itemId.toString(), fallbackOption.getItem_id_regex())) {
                    var container = WeaponRegistry.containers.get(Identifier.parse(fallbackOption.getWeapon_attributes()));
                    if (container != null) {
                        WeaponRegistry.resolveAndRegisterAttributes(itemId, container);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Reads the item's default attribute modifiers.
     *
     * <h2>26.2: why this is defensive</h2>
     * {@code getDefaultInstance()} builds an {@link net.minecraft.world.item.ItemStack}, and 26.2
     * binds item data components during a datapack load. This method runs from a
     * {@code SimplePreparableReloadListener} (the weapon-config reload), which can fire before that
     * binding is complete — {@code Holder.Reference#components()} then throws
     * {@code NullPointerException: Components not bound yet} and, because it happens inside
     * {@code CreateWorldScreen}, the game appears to hang on "preparing world" and then exits.
     *
     * <p>The specifier lookup only affects optional fallback resolution, so returning {@code false}
     * until components are available is the correct behaviour: the next reload picks it up.
     */
    private static boolean hasAttributeModifier(Item item, Holder<Attribute> searchedAttribute) {
        if (item == null) return false;

        final ItemStack stack;
        try {
            stack = item.getDefaultInstance();
        } catch (RuntimeException componentsNotReady) {
            return false;
        }
        // An empty stack means the item could not produce a default instance either.
        if (stack == null || stack.isEmpty()) return false;

        final boolean[] found = {false};
        try {
            stack.forEachModifier(EquipmentSlot.MAINHAND, (attr, mod) -> {
                if (attr.equals(searchedAttribute)) found[0] = true;
            });
        } catch (RuntimeException componentsNotReady) {
            return false;
        }
        return found[0];
    }
}
