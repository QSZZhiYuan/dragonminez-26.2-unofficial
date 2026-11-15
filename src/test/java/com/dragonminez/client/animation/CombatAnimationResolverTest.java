package com.dragonminez.client.animation;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CombatAnimationResolverTest {
    private static final Identifier COMBAT_FILE = Identifier.parse(
            "dragonminez:geckolib/animations/entity/races/combat.animation.json");

    @Test
    void realCombatFileResolvesPunchesKicksAndMainArmMirroring() {
        CombatAnimationResolver.reload(resources(new Resource(null, () -> {
            var stream = getClass().getResourceAsStream("/assets/dragonminez/geckolib/animations/entity/races/combat.animation.json");
            assertNotNull(stream, "The actual combat asset must be on the runtime classpath");
            return stream;
        })));

        assertEquals("combat.one_handed_punch_right", CombatAnimationResolver.resolveAttack("combat.one_handed_punch_right", false));
        assertEquals("combat.one_handed_punch_left", CombatAnimationResolver.resolveAttack("dragonminez:combat.one_handed_punch_right", true));
        assertEquals("combat.lowkick_right", CombatAnimationResolver.resolveAttack("lowkick_right", false));
        assertEquals("combat.one_handed_uppercut_right", CombatAnimationResolver.resolveAttack("one_handed_uppercut_right", false));
        assertEquals("", CombatAnimationResolver.resolveAttack("missing_attack", false));
    }

    @Test
    void resourceReloadReplacesAvailableAnimationNames() {
        CombatAnimationResolver.reload(jsonResources("{\"animations\":{\"combat.custom_pose\":{}}}"));
        assertEquals("combat.custom_pose", CombatAnimationResolver.resolvePose("custom_pose"));

        CombatAnimationResolver.reload(jsonResources("{\"animations\":{\"combat.replacement_pose\":{}}}"));
        assertEquals("", CombatAnimationResolver.resolvePose("custom_pose"));
        assertEquals("combat.replacement_pose", CombatAnimationResolver.resolvePose("replacement_pose"));
    }

    private static ResourceManager jsonResources(String json) {
        return resources(new Resource(null, () -> new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))));
    }

    private static ResourceManager resources(Resource combat) {
        return (ResourceManager) Proxy.newProxyInstance(ResourceManager.class.getClassLoader(),
                new Class<?>[]{ResourceManager.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getResource")) {
                        assertEquals(COMBAT_FILE, args[0], "ResourceManager needs the file path, not the GeckoLib cache key");
                        return Optional.of(combat);
                    }
                    throw new AssertionError("Unexpected resource operation: " + method);
                });
    }
}
