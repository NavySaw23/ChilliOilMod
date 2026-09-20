package net.chillioil.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class ServerGuiMenu extends ChestMenu {
    private final Map<Integer, BiConsumer<ServerPlayer, ClickType>> slotCallbacks = new HashMap<>();

    public ServerGuiMenu(MenuType<ChestMenu> type, int containerId, Inventory playerInventory, Container container, int rows) {
        super(type, containerId, playerInventory, container, rows);
    }

    public void setSlotCallback(int slot, BiConsumer<ServerPlayer, ClickType> callback) {
        if (callback != null) {
            slotCallbacks.put(slot, callback);
        } else {
            slotCallbacks.remove(slot);
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Only handle interactions with the top (chest) container
        if (slotId >= 0 && slotId < this.getContainer().getContainerSize()) {
            // Always force cursor/carried item to EMPTY so the client never "holds" or "picks up" the virtual item
            this.setCarried(ItemStack.EMPTY);

            if (player instanceof ServerPlayer serverPlayer) {
                BiConsumer<ServerPlayer, ClickType> callback = slotCallbacks.get(slotId);
                if (callback != null) {
                    callback.accept(serverPlayer, clickType);
                }
            }

            // Immediately clear cursor and resync container data so client cursor never grabs items
            this.setCarried(ItemStack.EMPTY);
            this.sendAllDataToRemote();
            return;
        }

        // If clicking in player inventory while GUI is open, prevent shift-clicking or moving into the GUI
        if (clickType == ClickType.QUICK_MOVE || clickType == ClickType.SWAP) {
            this.setCarried(ItemStack.EMPTY);
            this.sendAllDataToRemote();
            return;
        }

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public static void open(ServerPlayer player, Component title, int rows, BiConsumer<SimpleContainer, ServerGuiMenu> populator) {
        MenuType<ChestMenu> menuType = switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };

        int size = rows * 9;
        SimpleContainer container = new SimpleContainer(size);

        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return title;
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player p) {
                ServerGuiMenu menu = new ServerGuiMenu(menuType, syncId, playerInventory, container, rows);
                populator.accept(container, menu);
                return menu;
            }
        });
    }
}
