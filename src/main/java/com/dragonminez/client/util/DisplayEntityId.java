package com.dragonminez.client.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helpers for the throwaway entities DMZ renders in menus (quest-tree enemy previews, and anything
 * else that needs a live {@link Entity} purely for drawing).
 *
 * <h2>26.2: display entities must be given an id</h2>
 * Since 26.2 {@code Entity#getId()} throws when the id is still 0:
 *
 * <pre>{@code
 * public int getId() {
 *     if (this.id == 0) throw new IllegalStateException("Tried to access entity ID before ID assignment");
 *     return this.id;
 * }
 * }</pre>
 *
 * A real entity gets its id when it is added to a level; a hand-built one never does. Merely rendering
 * such an entity happens to work, but the moment anything resolves an {@code ItemStackRenderState} —
 * for example {@code ItemModelResolver#updateForLiving}, which is reached as soon as the preview holds
 * or wears any item — the renderer reads {@code getId()} and the screen crashes with
 * {@code IllegalStateException: Tried to access entity ID before ID assignment}.
 *
 * <p>So every display entity must be stamped with a non-zero id. The ids come from a dedicated
 * negative range that cannot collide with real entities (those start at 1 and increase), and they are
 * only ever used for rendering.
 */
public final class DisplayEntityId {

    /** Negative, so it can never collide with a server-assigned entity id. */
    private static final AtomicInteger NEXT_ID = new AtomicInteger(-2);

    private DisplayEntityId() {}

    /**
     * Assigns a unique, non-zero id to a display-only entity.
     *
     * @param entity the entity to stamp; may be null
     * @param <T>    entity type
     * @return the same entity, for chaining
     */
    public static <T extends Entity> T assign(T entity) {
        if (entity != null && entity.getId() == 0) {
            entity.setId(NEXT_ID.getAndDecrement());
        }
        return entity;
    }

    /**
     * Safe id lookup for render code that may run before an entity has been added to a level.
     * Returns 0 rather than throwing.
     */
    public static int peek(Entity entity) {
        if (entity == null) return 0;
        try {
            return entity.getId();
        } catch (IllegalStateException notAssigned) {
            return 0;
        }
    }

    /** Wraps an id into the display range, for use as a render-state key. */
    public static int renderKey(Entity entity) {
        return Mth.floor(peek(entity));
    }
}
