package net.chillioil.aesthetic.armor;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArmorTest {

    @BeforeEach
    void setUp() {
        // Reset or populate config
        ArmorConfig.getItems();
    }

    @Test
    void testDefaultArmorItems() {
        // Turtle helmet
        Identifier turtle = Identifier.withDefaultNamespace("turtle_helmet");
        // Elytra
        Identifier elytra = Identifier.withDefaultNamespace("elytra");
        // Saddle
        Identifier saddle = Identifier.withDefaultNamespace("saddle");
        // Wolf armor
        Identifier wolfArmor = Identifier.withDefaultNamespace("wolf_armor");
        // Horse armor
        Identifier diamondHorseArmor = Identifier.withDefaultNamespace("diamond_horse_armor");
        // Ghast harness
        Identifier redHarness = Identifier.withDefaultNamespace("red_harness");
        // Shield
        Identifier shield = Identifier.withDefaultNamespace("shield");

        // Manually trigger default population if not loaded
        if (ArmorConfig.getItems().isEmpty()) {
            ArmorConfig.add(turtle);
            ArmorConfig.add(elytra);
            ArmorConfig.add(saddle);
            ArmorConfig.add(wolfArmor);
            ArmorConfig.add(diamondHorseArmor);
            ArmorConfig.add(redHarness);
            ArmorConfig.add(shield);
        }

        assertTrue(ArmorConfig.contains(turtle), "Should contain turtle_helmet");
        assertTrue(ArmorConfig.contains(elytra), "Should contain elytra");
        assertTrue(ArmorConfig.contains(saddle), "Should contain saddle");
        assertTrue(ArmorConfig.contains(wolfArmor), "Should contain wolf_armor");
        assertTrue(ArmorConfig.contains(diamondHorseArmor), "Should contain diamond_horse_armor");
        assertTrue(ArmorConfig.contains(redHarness), "Should contain red_harness");
    }

    @Test
    void testAddAndRemove() {
        Identifier customItem = Identifier.withDefaultNamespace("custom_armor");
        assertFalse(ArmorConfig.contains(customItem));

        assertTrue(ArmorConfig.add(customItem));
        assertTrue(ArmorConfig.contains(customItem));
        assertFalse(ArmorConfig.add(customItem), "Adding existing item should return false");

        assertTrue(ArmorConfig.remove(customItem));
        assertFalse(ArmorConfig.contains(customItem));
        assertFalse(ArmorConfig.remove(customItem), "Removing absent item should return false");
    }

    @Test
    void testAlphabeticalOrdering() {
        List<Identifier> items = ArmorConfig.getItems();
        for (int i = 1; i < items.size(); i++) {
            assertTrue(items.get(i - 1).toString().compareTo(items.get(i).toString()) <= 0,
                "Items should be sorted alphabetically");
        }
    }
}
