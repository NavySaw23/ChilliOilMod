package net.chillioil.aesthetic.armor;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetheriteAssetTest {

    @BeforeAll
    static void init() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void testNetheriteAssetId() {
        ItemStack netherite = new ItemStack(Items.NETHERITE_CHESTPLATE);
        Equippable equippable = netherite.get(DataComponents.EQUIPPABLE);
        assertNotNull(equippable);
        assertTrue(equippable.assetId().isPresent());
        assertEquals(EquipmentAssets.NETHERITE, equippable.assetId().get());
    }
}
