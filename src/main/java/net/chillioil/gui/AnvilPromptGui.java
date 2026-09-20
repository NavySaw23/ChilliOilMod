package net.chillioil.gui;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

public class AnvilPromptGui extends AnvilMenu {
    private final Consumer<String> onComplete;
    private final Runnable onCancel;
    private boolean completed = false;

    public AnvilPromptGui(int syncId, Inventory playerInventory, Consumer<String> onComplete, Runnable onCancel) {
        super(syncId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), playerInventory.player.blockPosition()));
        this.onComplete = onComplete;
        this.onCancel = onCancel;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Slot 2 is the output result slot in AnvilMenu
        if (slotId == 2) {
            ItemStack outputStack = this.getSlot(2).getItem();
            String enteredName = "";
            if (!outputStack.isEmpty()) {
                enteredName = outputStack.getHoverName().getString().trim();
            }

            if (!enteredName.isEmpty() && player instanceof ServerPlayer serverPlayer) {
                completed = true;
                // Clear the virtual slots so vanilla anvil drop logic doesn't drop ghost items or crash container
                this.getSlot(0).set(ItemStack.EMPTY);
                this.getSlot(2).set(ItemStack.EMPTY);
                serverPlayer.closeContainer();
                final String finalName = enteredName;
                serverPlayer.level().getServer().execute(() -> {
                    if (onComplete != null) {
                        onComplete.accept(finalName);
                    }
                });
                return;
            }
            this.sendAllDataToRemote();
            return;
        }

        // Prevent taking the input item out of slot 0 or slot 1
        if (slotId == 0 || slotId == 1) {
            this.sendAllDataToRemote();
            return;
        }

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public void removed(Player player) {
        // Clear slots to prevent dropping virtual name tags
        this.getSlot(0).set(ItemStack.EMPTY);
        this.getSlot(2).set(ItemStack.EMPTY);
        super.removed(player);
        if (!completed && onCancel != null && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.level().getServer().execute(onCancel);
        }
    }

    public static void open(ServerPlayer player, String initialName, Consumer<String> onComplete, Runnable onCancel) {
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Name Your Outfit");
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player p) {
                AnvilPromptGui menu = new AnvilPromptGui(syncId, playerInventory, onComplete, onCancel);
                ItemStack paper = new ItemStack(Items.NAME_TAG);
                String name = initialName != null ? initialName : "outfit";
                paper.set(DataComponents.CUSTOM_NAME, Component.literal(name));
                menu.getSlot(0).set(paper);
                menu.setItemName(name);
                return menu;
            }
        });
    }
}
