package net.chillioil.management;

import net.chillioil.aesthetic.armor.ArmorConfig;
import net.chillioil.aesthetic.skin.SkinWardrobeConfig;
import net.chillioil.gui.ServerGuiMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;
import java.util.Map;

public class ChilliConfigGui {
    public static void open(ServerPlayer player) {
        Component title = Component.literal("ChilliOil Configuration");

        ServerGuiMenu.open(player, title, 3, (container, menu) -> {
            // Fill background with dark gray glass panes
            ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            filler.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
            for (int i = 0; i < 27; i++) {
                container.setItem(i, filler.copy());
                menu.setSlotCallback(i, (p, click) -> {});
            }

            // Slot 11: /armor Module
            boolean armorOn = ModuleConfig.isModuleEnabled("armor");
            ItemStack armorItem = new ItemStack(Items.NETHERITE_CHESTPLATE);
            armorItem.set(DataComponents.CUSTOM_NAME, Component.literal("Module: /armor")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            armorItem.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("Status: ").withStyle(ChatFormatting.GRAY)
                    .append(armorOn ? Component.literal("ENABLED").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                                    : Component.literal("DISABLED").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)),
                Component.literal("Aesthetic equipment & armor model hiding").withStyle(ChatFormatting.DARK_GRAY),
                Component.literal(""),
                Component.literal("▶ Click to Toggle").withStyle(ChatFormatting.YELLOW)
            )));
            container.setItem(11, armorItem);
            menu.setSlotCallback(11, (p, click) -> {
                ModuleConfig.setModuleEnabled("armor", !armorOn);
                open(p); // Re-render GUI with new state
            });

            // Slot 20: Status Indicator for /armor (Lime or Red glass pane)
            ItemStack armorIndicator = new ItemStack(armorOn ? Items.LIME_STAINED_GLASS_PANE : Items.RED_STAINED_GLASS_PANE);
            armorIndicator.set(DataComponents.CUSTOM_NAME, Component.literal(armorOn ? "ON" : "OFF")
                .withStyle(armorOn ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD));
            container.setItem(20, armorIndicator);
            menu.setSlotCallback(20, (p, click) -> {
                ModuleConfig.setModuleEnabled("armor", !armorOn);
                open(p);
            });

            // Slot 13: /skin Module
            boolean skinOn = ModuleConfig.isModuleEnabled("skin");
            ItemStack skinItem = new ItemStack(Items.PLAYER_HEAD);
            skinItem.set(DataComponents.CUSTOM_NAME, Component.literal("Module: /skin")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            skinItem.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("Status: ").withStyle(ChatFormatting.GRAY)
                    .append(skinOn ? Component.literal("ENABLED").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                                   : Component.literal("DISABLED").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)),
                Component.literal("Dynamic player skins & restyling").withStyle(ChatFormatting.DARK_GRAY),
                Component.literal(""),
                Component.literal("▶ Click to Toggle").withStyle(ChatFormatting.YELLOW)
            )));
            container.setItem(13, skinItem);
            menu.setSlotCallback(13, (p, click) -> {
                ModuleConfig.setModuleEnabled("skin", !skinOn);
                open(p);
            });

            // Slot 22: Status Indicator for /skin
            ItemStack skinIndicator = new ItemStack(skinOn ? Items.LIME_STAINED_GLASS_PANE : Items.RED_STAINED_GLASS_PANE);
            skinIndicator.set(DataComponents.CUSTOM_NAME, Component.literal(skinOn ? "ON" : "OFF")
                .withStyle(skinOn ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD));
            container.setItem(22, skinIndicator);
            menu.setSlotCallback(22, (p, click) -> {
                ModuleConfig.setModuleEnabled("skin", !skinOn);
                open(p);
            });

            // Slot 15: /wardrobe Module
            boolean wardrobeOn = ModuleConfig.isModuleEnabled("wardrobe");
            ItemStack wardrobeItem = new ItemStack(Items.ENDER_CHEST);
            wardrobeItem.set(DataComponents.CUSTOM_NAME, Component.literal("Module: /wardrobe")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            wardrobeItem.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("Status: ").withStyle(ChatFormatting.GRAY)
                    .append(wardrobeOn ? Component.literal("ENABLED").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                                       : Component.literal("DISABLED").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)),
                Component.literal("Player outfit wardrobe & GUI").withStyle(ChatFormatting.DARK_GRAY),
                Component.literal(""),
                Component.literal("▶ Click to Toggle").withStyle(ChatFormatting.YELLOW)
            )));
            container.setItem(15, wardrobeItem);
            menu.setSlotCallback(15, (p, click) -> {
                ModuleConfig.setModuleEnabled("wardrobe", !wardrobeOn);
                open(p);
            });

            // Slot 24: Status Indicator for /wardrobe
            ItemStack wardrobeIndicator = new ItemStack(wardrobeOn ? Items.LIME_STAINED_GLASS_PANE : Items.RED_STAINED_GLASS_PANE);
            wardrobeIndicator.set(DataComponents.CUSTOM_NAME, Component.literal(wardrobeOn ? "ON" : "OFF")
                .withStyle(wardrobeOn ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD));
            container.setItem(24, wardrobeIndicator);
            menu.setSlotCallback(24, (p, click) -> {
                ModuleConfig.setModuleEnabled("wardrobe", !wardrobeOn);
                open(p);
            });

            // Slot 0: Reload Configs Button
            ItemStack reloadItem = new ItemStack(Items.COMPASS);
            reloadItem.set(DataComponents.CUSTOM_NAME, Component.literal("Reload Configs")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            reloadItem.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("Reloads armor.json, skin-wardrobe.json,").withStyle(ChatFormatting.GRAY),
                Component.literal("and modules.json from disk.").withStyle(ChatFormatting.GRAY),
                Component.literal(""),
                Component.literal("▶ Click to Reload").withStyle(ChatFormatting.GREEN)
            )));
            container.setItem(0, reloadItem);
            menu.setSlotCallback(0, (p, click) -> {
                ArmorConfig.load();
                SkinWardrobeConfig.load();
                ModuleConfig.load();
                p.sendSystemMessage(Component.literal("ChilliOil configurations reloaded from disk.").withStyle(ChatFormatting.GREEN));
                open(p);
            });

            // Slot 8: Reset to Defaults Button
            ItemStack resetItem = new ItemStack(Items.REDSTONE_TORCH);
            resetItem.set(DataComponents.CUSTOM_NAME, Component.literal("Reset All Modules")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            resetItem.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("Restores all modules to ENABLED").withStyle(ChatFormatting.GRAY),
                Component.literal("and saves modules.json.").withStyle(ChatFormatting.GRAY),
                Component.literal(""),
                Component.literal("▶ Click to Reset").withStyle(ChatFormatting.RED)
            )));
            container.setItem(8, resetItem);
            menu.setSlotCallback(8, (p, click) -> {
                ModuleConfig.resetDefaults();
                p.sendSystemMessage(Component.literal("Reset all module toggle states to ENABLED.").withStyle(ChatFormatting.GREEN));
                open(p);
            });
        });
    }
}
