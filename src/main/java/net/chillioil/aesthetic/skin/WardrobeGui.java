package net.chillioil.aesthetic.skin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.chillioil.gui.AnvilPromptGui;
import net.chillioil.gui.ServerGuiMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WardrobeGui {
    private static final int PAGE_SIZE = 45; // Slots 0 to 44 for skins, row 6 (45-53) for controls

    public static void open(ServerPlayer player, int page) {
        UUID uuid = player.getUUID();
        List<Map.Entry<String, SkinData>> entries = WardrobeStorage.getEntries(uuid);
        int totalOutfits = entries.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalOutfits / PAGE_SIZE));
        int currentPage = Math.max(1, Math.min(page, totalPages));

        Component title = Component.literal("Wardrobe (Page " + currentPage + "/" + totalPages + ")");

        ServerGuiMenu.open(player, title, 6, (container, menu) -> {
            int startIndex = (currentPage - 1) * PAGE_SIZE;
            int endIndex = Math.min(startIndex + PAGE_SIZE, totalOutfits);

            // Populate skin slots
            for (int i = startIndex; i < endIndex; i++) {
                int slotIndex = i - startIndex;
                Map.Entry<String, SkinData> entry = entries.get(i);
                String outfitName = entry.getKey();
                SkinData skin = entry.getValue();

                ItemStack head = createPlayerHead(outfitName, skin);
                container.setItem(slotIndex, head);

                // Slot Click: Equip skin
                menu.setSlotCallback(slotIndex, (p, clickType) -> {
                    int remainingCooldown = PlayerSkinState.getRemainingWardrobeCooldown(p.getUUID());
                    if (remainingCooldown > 0) {
                        p.sendSystemMessage(Component.literal("Please wait " + remainingCooldown + " second(s) before changing skins again.")
                            .withStyle(ChatFormatting.RED));
                        return;
                    }

                    if (SkinApplier.applySkin(p, skin)) {
                        p.sendSystemMessage(Component.literal("Now wearing '")
                            .withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(outfitName).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                            .append(Component.literal("' from your wardrobe!").withStyle(ChatFormatting.GREEN)));
                        // Close wardrobe container on equip
                        p.closeContainer();
                    } else {
                        p.sendSystemMessage(Component.literal("Failed to wear wardrobe outfit.").withStyle(ChatFormatting.RED));
                    }
                });
            }

            // Fill bottom row with dark gray glass panes
            ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            filler.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
            for (int slot = 45; slot <= 53; slot++) {
                container.setItem(slot, filler.copy());
                menu.setSlotCallback(slot, (p, click) -> {});
            }

            // Slot 45: Previous Page Arrow
            if (currentPage > 1) {
                ItemStack prevArrow = new ItemStack(Items.ARROW);
                prevArrow.set(DataComponents.CUSTOM_NAME, Component.literal("◀ Previous Page (" + (currentPage - 1) + ")")
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
                container.setItem(45, prevArrow);
                menu.setSlotCallback(45, (p, click) -> open(p, currentPage - 1));
            }

            // Slot 48: [+] Add Current Skin
            ItemStack addBtn = new ItemStack(Items.NETHER_STAR);
            addBtn.set(DataComponents.CUSTOM_NAME, Component.literal("+ Add Current Skin")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            List<Component> addLore = List.of(
                Component.literal("Click to save your active skin").withStyle(ChatFormatting.GRAY),
                Component.literal("into your wardrobe.").withStyle(ChatFormatting.GRAY)
            );
            addBtn.set(DataComponents.LORE, new ItemLore(addLore));
            container.setItem(48, addBtn);
            menu.setSlotCallback(48, (p, click) -> {
                SkinData currentSkin = PlayerSkinState.getActiveSkin(p.getUUID());
                if (currentSkin == null) {
                    p.sendSystemMessage(Component.literal("No active skin detected to add.").withStyle(ChatFormatting.RED));
                    return;
                }
                if (WardrobeStorage.getEntries(p.getUUID()).size() >= SkinWardrobeConfig.getWardrobeLimit()) {
                    p.sendSystemMessage(Component.literal("Wardrobe is full! Max limit is " + SkinWardrobeConfig.getWardrobeLimit() + " skins.")
                        .withStyle(ChatFormatting.RED));
                    return;
                }

                // Open Anvil prompt for naming
                int nextIndex = 1;
                while (WardrobeStorage.getSkin(p.getUUID(), "newskin" + nextIndex).isPresent()) {
                    nextIndex++;
                }
                String defaultName = "newskin" + nextIndex;

                AnvilPromptGui.open(p, defaultName, (enteredName) -> {
                    String cleanName = enteredName.trim().toLowerCase();
                    if (WardrobeStorage.addSkin(p.getUUID(), cleanName, currentSkin)) {
                        p.sendSystemMessage(Component.literal("Saved '")
                            .withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(cleanName).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                            .append(Component.literal("' to your wardrobe!").withStyle(ChatFormatting.GREEN)));
                    } else {
                        p.sendSystemMessage(Component.literal("Failed to save skin. Wardrobe may be full.").withStyle(ChatFormatting.RED));
                    }
                    // Re-open wardrobe
                    open(p, currentPage);
                }, () -> {
                    // Cancelled -> reopen wardrobe
                    open(p, currentPage);
                });
            });

            // Slot 50: Info Paper (Bottom row 6th slot, symmetrical with [+] on 4th slot 48)
            ItemStack infoPaper = new ItemStack(Items.PAPER);
            infoPaper.set(DataComponents.CUSTOM_NAME, Component.literal("Wardrobe Info")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            List<Component> infoLore = List.of(
                Component.literal("Outfits: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(totalOutfits + " / " + SkinWardrobeConfig.getWardrobeLimit()).withStyle(ChatFormatting.WHITE)),
                Component.literal("Page: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(currentPage + " / " + totalPages).withStyle(ChatFormatting.WHITE)),
                Component.literal(""),
                Component.literal("• Click any skin head to equip").withStyle(ChatFormatting.DARK_GRAY),
                Component.literal("• Use /wardrobe remove <name> to delete").withStyle(ChatFormatting.DARK_GRAY)
            );
            infoPaper.set(DataComponents.LORE, new ItemLore(infoLore));
            container.setItem(50, infoPaper);

            // Slot 53: Next Page Arrow
            if (currentPage < totalPages) {
                ItemStack nextArrow = new ItemStack(Items.ARROW);
                nextArrow.set(DataComponents.CUSTOM_NAME, Component.literal("Next Page (" + (currentPage + 1) + ") ▶")
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
                container.setItem(53, nextArrow);
                menu.setSlotCallback(53, (p, click) -> open(p, currentPage + 1));
            }
        });
    }

    private static ItemStack createPlayerHead(String name, SkinData skin) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponents.CUSTOM_NAME, Component.literal(name)
            .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("Variant: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(skin.variant()).withStyle(ChatFormatting.AQUA)));
        lore.add(Component.literal(""));
        lore.add(Component.literal("▶ Click to Equip").withStyle(ChatFormatting.GREEN));
        lore.add(Component.literal("To delete: /wardrobe remove " + name).withStyle(ChatFormatting.DARK_GRAY));
        head.set(DataComponents.LORE, new ItemLore(lore));

        // Attach Mojang profile texture so the player head displays the skin face
        if (skin.value() != null && !skin.value().isBlank()) {
            try {
                com.google.common.collect.Multimap<String, Property> properties = com.google.common.collect.LinkedHashMultimap.create();
                properties.put("textures", new Property("textures", skin.value(), skin.signature()));
                GameProfile profile = new GameProfile(UUID.randomUUID(), name, new PropertyMap(properties));
                head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
            } catch (Exception ignored) {
            }
        }

        return head;
    }
}
