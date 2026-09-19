package net.chillioil.aesthetic.armor;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArmorTagHelperTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void testSetAndCheckInvisiblePlayerArmor() {
        ItemStack chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
        assertFalse(ArmorTagHelper.isInvisible(chestplate), "New item should not be invisible");

        // Make invisible
        boolean changed = ArmorTagHelper.setInvisible(chestplate, true);
        assertTrue(changed);
        assertTrue(ArmorTagHelper.isInvisible(chestplate), "Item should now be marked invisible");

        // Verify assetId is air
        Equippable hiddenEquippable = chestplate.get(DataComponents.EQUIPPABLE);
        assertNotNull(hiddenEquippable);
        assertEquals(ArmorTagHelper.AIR_ASSET, hiddenEquippable.assetId().orElse(null));

        // Check lore is added
        ItemLore lore = chestplate.get(DataComponents.LORE);
        assertNotNull(lore, "Lore should be present");
        boolean containsInvisible = lore.lines().stream()
            .map(Component::getString)
            .anyMatch(s -> s.contains("Invisible"));
        assertTrue(containsInvisible, "Lore should contain 'Invisible'");

        // Remove invisibility
        boolean reverted = ArmorTagHelper.setInvisible(chestplate, false);
        assertTrue(reverted);
        assertFalse(ArmorTagHelper.isInvisible(chestplate), "Item should no longer be invisible");

        // Verify original assetId is restored
        Equippable restoredEquippable = chestplate.get(DataComponents.EQUIPPABLE);
        assertNotNull(restoredEquippable);
        assertEquals(EquipmentAssets.DIAMOND, restoredEquippable.assetId().orElse(null));

        // Check lore is removed
        ItemLore emptyLore = chestplate.get(DataComponents.LORE);
        if (emptyLore != null) {
            boolean stillContains = emptyLore.lines().stream()
                .map(Component::getString)
                .anyMatch(s -> s.contains("Invisible"));
            assertFalse(stillContains, "Lore should no longer contain 'Invisible'");
        }
    }

    @Test
    void testWolfArmorInvisibility() {
        ItemStack wolfArmor = new ItemStack(Items.WOLF_ARMOR);
        assertFalse(ArmorTagHelper.isInvisible(wolfArmor));

        assertTrue(ArmorTagHelper.setInvisible(wolfArmor, true));
        assertTrue(ArmorTagHelper.isInvisible(wolfArmor));

        Equippable hidden = wolfArmor.get(DataComponents.EQUIPPABLE);
        assertNotNull(hidden);
        assertEquals(ArmorTagHelper.AIR_ASSET, hidden.assetId().orElse(null));

        assertTrue(ArmorTagHelper.setInvisible(wolfArmor, false));
        assertFalse(ArmorTagHelper.isInvisible(wolfArmor));

        Equippable restored = wolfArmor.get(DataComponents.EQUIPPABLE);
        assertNotNull(restored);
        assertEquals(EquipmentAssets.ARMADILLO_SCUTE, restored.assetId().orElse(null));
    }

    @Test
    void testHorseArmorInvisibility() {
        ItemStack horseArmor = new ItemStack(Items.IRON_HORSE_ARMOR);
        assertFalse(ArmorTagHelper.isInvisible(horseArmor));

        assertTrue(ArmorTagHelper.setInvisible(horseArmor, true));
        assertTrue(ArmorTagHelper.isInvisible(horseArmor));

        Equippable hidden = horseArmor.get(DataComponents.EQUIPPABLE);
        assertNotNull(hidden);
        assertEquals(ArmorTagHelper.AIR_ASSET, hidden.assetId().orElse(null));

        assertTrue(ArmorTagHelper.setInvisible(horseArmor, false));
        assertFalse(ArmorTagHelper.isInvisible(horseArmor));

        Equippable restored = horseArmor.get(DataComponents.EQUIPPABLE);
        assertNotNull(restored);
        assertEquals(EquipmentAssets.IRON, restored.assetId().orElse(null));
    }

    @Test
    void testSaddleInvisibility() {
        ItemStack saddle = new ItemStack(Items.SADDLE);
        assertFalse(ArmorTagHelper.isInvisible(saddle));

        assertTrue(ArmorTagHelper.setInvisible(saddle, true));
        assertTrue(ArmorTagHelper.isInvisible(saddle));

        Equippable hidden = saddle.get(DataComponents.EQUIPPABLE);
        assertNotNull(hidden);
        assertEquals(ArmorTagHelper.AIR_ASSET, hidden.assetId().orElse(null));

        assertTrue(ArmorTagHelper.setInvisible(saddle, false));
        assertFalse(ArmorTagHelper.isInvisible(saddle));

        Equippable restored = saddle.get(DataComponents.EQUIPPABLE);
        assertNotNull(restored);
        assertEquals(EquipmentAssets.SADDLE, restored.assetId().orElse(null));
    }

    @Test
    void testNonHideableItems() {
        ItemStack shield = new ItemStack(Items.SHIELD);
        assertFalse(ArmorTagHelper.canBeHidden(shield), "Shields should not be hideable");
        assertFalse(ArmorTagHelper.setInvisible(shield, true), "setInvisible should return false for shield");

        ItemStack torch = new ItemStack(Items.TORCH);
        assertFalse(ArmorTagHelper.canBeHidden(torch), "Torches should not be hideable");
        assertFalse(ArmorTagHelper.setInvisible(torch, true), "setInvisible should return false for torch");

        ItemStack apple = new ItemStack(Items.APPLE);
        assertFalse(ArmorTagHelper.canBeHidden(apple), "Food items should not be hideable");
        assertFalse(ArmorTagHelper.setInvisible(apple, true), "setInvisible should return false for apple");

        ItemStack chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
        assertTrue(ArmorTagHelper.canBeHidden(chestplate), "Diamond chestplate should be hideable");
    }
}
