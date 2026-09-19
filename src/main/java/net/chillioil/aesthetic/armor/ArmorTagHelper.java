package net.chillioil.aesthetic.armor;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArmorTagHelper {
    public static final String INVISIBLE_TAG_KEY = "invisible";
    public static final String ORIGINAL_ASSET_ID_KEY = "EQUIPPABLE_ASSET_ID";
    public static final Component INVISIBLE_LORE_COMPONENT = Component.literal("Invisible")
        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);

    public static final ResourceKey<EquipmentAsset> AIR_ASSET = EquipmentAssets.createId("air");

    public static boolean isInvisible(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.getBooleanOr(INVISIBLE_TAG_KEY, false)) {
                return true;
            }
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null && equippable.assetId().isPresent()) {
            return equippable.assetId().get().equals(AIR_ASSET);
        }
        return false;
    }

    public static boolean canBeHidden(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) {
            return false;
        }
        if (equippable.assetId().isPresent()) {
            return true;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.copyTag().contains(ORIGINAL_ASSET_ID_KEY);
    }

    public static boolean setInvisible(ItemStack stack, boolean invisible) {
        if (stack == null || stack.isEmpty() || !canBeHidden(stack)) {
            return false;
        }

        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) {
            return false;
        }

        if (invisible) {
            // Save original asset ID if present and not already air
            Optional<ResourceKey<EquipmentAsset>> currentAsset = equippable.assetId();
            if (currentAsset.isPresent() && !currentAsset.get().equals(AIR_ASSET)) {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                    tag.putString(ORIGINAL_ASSET_ID_KEY, currentAsset.get().identifier().toString());
                    tag.putBoolean(INVISIBLE_TAG_KEY, true);
                });
            } else {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                    tag.putBoolean(INVISIBLE_TAG_KEY, true);
                });
            }

            // Set assetId to air
            Equippable hiddenEquippable = new Equippable(
                equippable.slot(),
                equippable.equipSound(),
                Optional.of(AIR_ASSET),
                equippable.cameraOverlay(),
                equippable.allowedEntities(),
                equippable.dispensable(),
                equippable.swappable(),
                equippable.damageOnHurt(),
                equippable.equipOnInteract(),
                equippable.canBeSheared(),
                equippable.shearingSound()
            );
            stack.set(DataComponents.EQUIPPABLE, hiddenEquippable);

            // Add "Invisible" to lore if not already present
            ItemLore existingLore = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
            List<Component> lines = new ArrayList<>(existingLore.lines());
            boolean alreadyPresent = false;
            for (Component line : lines) {
                if (line.getString().contains("Invisible")) {
                    alreadyPresent = true;
                    break;
                }
            }
            if (!alreadyPresent) {
                lines.add(INVISIBLE_LORE_COMPONENT);
                stack.set(DataComponents.LORE, new ItemLore(lines));
            }
            return true;
        } else {
            // Restore original asset ID
            Optional<ResourceKey<EquipmentAsset>> restoredAsset = Optional.empty();
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                if (tag.contains(ORIGINAL_ASSET_ID_KEY)) {
                    String idStr = tag.getStringOr(ORIGINAL_ASSET_ID_KEY, "");
                    if (!idStr.isEmpty()) {
                        Identifier id = Identifier.tryParse(idStr);
                        if (id != null) {
                            restoredAsset = Optional.of(ResourceKey.create(EquipmentAssets.ROOT_ID, id));
                        }
                    }
                }
            }

            // If not found in custom data, fallback to item's default equippable assetId
            if (restoredAsset.isEmpty()) {
                Equippable defaultEquippable = stack.getItem().components().get(DataComponents.EQUIPPABLE);
                if (defaultEquippable != null && defaultEquippable.assetId().isPresent()) {
                    restoredAsset = defaultEquippable.assetId();
                }
            }

            // Set restored Equippable
            Equippable restoredEquippable = new Equippable(
                equippable.slot(),
                equippable.equipSound(),
                restoredAsset,
                equippable.cameraOverlay(),
                equippable.allowedEntities(),
                equippable.dispensable(),
                equippable.swappable(),
                equippable.damageOnHurt(),
                equippable.equipOnInteract(),
                equippable.canBeSheared(),
                equippable.shearingSound()
            );
            stack.set(DataComponents.EQUIPPABLE, restoredEquippable);

            // Clean CustomData
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.remove(ORIGINAL_ASSET_ID_KEY);
                tag.remove(INVISIBLE_TAG_KEY);
            });

            // Remove "Invisible" from Lore
            ItemLore existingLore = stack.get(DataComponents.LORE);
            if (existingLore != null) {
                List<Component> lines = new ArrayList<>(existingLore.lines());
                lines.removeIf(line -> line.getString().contains("Invisible"));
                if (lines.isEmpty()) {
                    stack.remove(DataComponents.LORE);
                } else {
                    stack.set(DataComponents.LORE, new ItemLore(lines));
                }
            }
            return true;
        }
    }

    public static void resendEquipment(ServerPlayer player) {
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();

        if (player.level() instanceof ServerLevel serverLevel) {
            List<Pair<EquipmentSlot, ItemStack>> slots = new ArrayList<>();
            for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                slots.add(Pair.of(slot, player.getItemBySlot(slot)));
            }
            ClientboundSetEquipmentPacket packet = new ClientboundSetEquipmentPacket(player.getId(), slots);
            serverLevel.getChunkSource().sendToTrackingPlayers(player, packet);

            if (player.getVehicle() instanceof LivingEntity livingVehicle) {
                List<Pair<EquipmentSlot, ItemStack>> vehicleSlots = new ArrayList<>();
                for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                    vehicleSlots.add(Pair.of(slot, livingVehicle.getItemBySlot(slot)));
                }
                ClientboundSetEquipmentPacket vehiclePacket = new ClientboundSetEquipmentPacket(livingVehicle.getId(), vehicleSlots);
                serverLevel.getChunkSource().sendToTrackingPlayers(livingVehicle, vehiclePacket);
            }
        }
    }
}
