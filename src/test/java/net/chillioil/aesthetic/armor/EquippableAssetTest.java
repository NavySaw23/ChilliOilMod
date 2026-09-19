package net.chillioil.aesthetic.armor;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EquippableAssetTest {

    @BeforeAll
    static void init() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void testEquippableComponents() {
        ItemStack chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
        Equippable chestEquippable = chestplate.get(DataComponents.EQUIPPABLE);
        assertNotNull(chestEquippable, "Diamond chestplate should have EQUIPPABLE component");
        assertTrue(chestEquippable.assetId().isPresent(), "Asset ID should be present");
        assertEquals(EquipmentAssets.DIAMOND, chestEquippable.assetId().get());

        ItemStack wolfArmor = new ItemStack(Items.WOLF_ARMOR);
        Equippable wolfEquippable = wolfArmor.get(DataComponents.EQUIPPABLE);
        assertNotNull(wolfEquippable, "Wolf armor should have EQUIPPABLE component");
        assertTrue(wolfEquippable.assetId().isPresent(), "Wolf armor asset ID should be present");
        assertEquals(EquipmentAssets.ARMADILLO_SCUTE, wolfEquippable.assetId().get());

        ItemStack horseArmor = new ItemStack(Items.IRON_HORSE_ARMOR);
        Equippable horseEquippable = horseArmor.get(DataComponents.EQUIPPABLE);
        assertNotNull(horseEquippable, "Horse armor should have EQUIPPABLE component");
        assertTrue(horseEquippable.assetId().isPresent(), "Horse armor asset ID should be present");
        assertEquals(EquipmentAssets.IRON, horseEquippable.assetId().get());

        ItemStack saddle = new ItemStack(Items.SADDLE);
        Equippable saddleEquippable = saddle.get(DataComponents.EQUIPPABLE);
        assertNotNull(saddleEquippable, "Saddle should have EQUIPPABLE component");
        assertTrue(saddleEquippable.assetId().isPresent(), "Saddle asset ID should be present");
        assertEquals(EquipmentAssets.SADDLE, saddleEquippable.assetId().get());

        ItemStack shield = new ItemStack(Items.SHIELD);
        Equippable shieldEquippable = shield.get(DataComponents.EQUIPPABLE);
        assertNotNull(shieldEquippable, "Shield should have EQUIPPABLE component");
        System.out.println("Shield assetId: " + shieldEquippable.assetId());
    }

    @Test
    void testSwapWithAirAsset() {
        ItemStack chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
        Equippable original = chestplate.get(DataComponents.EQUIPPABLE);
        assertNotNull(original);

        ResourceKey<EquipmentAsset> airAsset = EquipmentAssets.createId("air");

        Equippable hidden = new Equippable(
            original.slot(),
            original.equipSound(),
            Optional.of(airAsset),
            original.cameraOverlay(),
            original.allowedEntities(),
            original.dispensable(),
            original.swappable(),
            original.damageOnHurt(),
            original.equipOnInteract(),
            original.canBeSheared(),
            original.shearingSound()
        );

        chestplate.set(DataComponents.EQUIPPABLE, hidden);

        Equippable updated = chestplate.get(DataComponents.EQUIPPABLE);
        assertNotNull(updated);
        assertEquals(airAsset, updated.assetId().orElse(null));
    }
}
