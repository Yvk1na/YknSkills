package dev.aurelium.auraskills.bukkit.trait;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManaRegenTraitTest {

    @Test
    void calculatesRegenFromMaxManaAndTrait() {
        assertEquals(15.1, ManaRegenTrait.calculateRegen(0.1, 750.0, 2.0), 0.000001);
    }

    @Test
    void preventsNegativeRegen() {
        assertEquals(0.0, ManaRegenTrait.calculateRegen(-10.0, 100.0, -2.0), 0.000001);
    }

}
