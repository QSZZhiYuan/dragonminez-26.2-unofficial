package com.dragonminez.mixin.client;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Write access to {@link ClientInput}'s movement vector.
 *
 * <h2>26.2 context</h2>
 * 1.21 exposed {@code Input#forwardImpulse} / {@code leftImpulse} as public floats that mods could
 * scale in place. 26.2 replaced that with a {@code ClientInput} object whose movement vector is a
 * derived {@link Vec2} field:
 *
 * <pre>{@code
 * protected Vec2 moveVector;              // the field the player actually moves along
 * public Vec2 getMoveVector();            // read-only accessor
 * public boolean hasForwardImpulse();     // no equivalents for "left""back" any more
 * }</pre>
 *
 * <p>The field is {@code protected}, so an accessor mixin is the supported way to scale it. That is
 * what restores the attack-swing slowdown: DMZ scales the vector for the duration of a swing instead
 * of scaling per-axis impulses that no longer exist.
 */
@Mixin(ClientInput.class)
public interface ClientInputAccessor {

	@Accessor("moveVector")
	void dragonminez$setMoveVector(Vec2 moveVector);

	@Accessor("moveVector")
	Vec2 dragonminez$getMoveVector();
}
