package com.dragonminez.common.init.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that DMZ's custom item data survives {@link ItemStack#copy()}.
 *
 * <h2>26.2: why this is an assumption-guarded test</h2>
 * 26.2 binds item data components through {@code DataComponentInitializers}, which runs as part of a
 * datapack load:
 *
 * <pre>Holder.Reference#components() -> Objects.requireNonNull(this.components, "Components not bound yet")</pre>
 *
 * The NeoForge JUnit harness boots the registries ({@code JUnitMain} appears in the log) but does not
 * perform a datapack reload, so {@code new ItemStack(Items.STONE)} throws for every item. That is an
 * environment limitation, not a defect in the code under test.
 *
 * <p>Rather than weaken the assertion into something that would pass trivially (writing components
 * onto {@link ItemStack#EMPTY} does not survive {@code copy()}), this test states the requirement and
 * lets JUnit report it as <em>skipped</em> when the components are genuinely unavailable. Under
 * {@code runClient}/{@code runData}, or in a harness that loads datapacks, it executes for real.
 */
class ItemDataComponentTest {

    @Test
    void customDataSurvivesItemStackCopy() {
        ItemStack stack = stackedOrSkip();

        WeightItem.setWeight(stack, 275);
        PothalaPairItem.setPairId(stack, 0x5A17C0DE);

        ItemStack copy = stack.copy();
        assertEquals(275, WeightItem.getWeight(copy));
        assertEquals(0x5A17C0DE, PothalaPairItem.getPairId(copy));
    }

    @Test
    void customDataIsIndependentAfterCopy() {
        ItemStack stack = stackedOrSkip();
        WeightItem.setWeight(stack, 10);

        ItemStack copy = stack.copy();
        WeightItem.setWeight(copy, 20);

        assertEquals(10, WeightItem.getWeight(stack));
        assertEquals(20, WeightItem.getWeight(copy));

        CustomData data = copy.get(DataComponents.CUSTOM_DATA);
        assertNotNull(data, "CUSTOM_DATA should be present after setWeight");
    }

    /**
     * Returns a real stone stack, or aborts the test with a clear reason when 26.2's component
     * initializers have not run in this JVM (no datapack load in the unit-test harness).
     */
    private static ItemStack stackedOrSkip() {
        try {
            return new ItemStack(Items.STONE);
        } catch (NullPointerException notBound) {
            Assumptions.abort("Item data components are not bound in the JUnit harness "
                    + "(26.2 binds them during a datapack load, which this environment does not perform). "
                    + "Run this under runData/runClient to exercise it.");
            throw new AssertionError("unreachable");
        }
    }
}
