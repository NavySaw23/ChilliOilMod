package net.chillioil.aesthetic.armor;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ArmorCommand {
    private static final EquipmentSlot[] EQUIPPED_SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET,
        EquipmentSlot.OFFHAND
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(
            Commands.literal("armor")
                // /armor list (OP only)
                .then(Commands.literal("list")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .executes(ArmorCommand::executeList)
                )
                // /armor add <item> (OP only)
                .then(Commands.literal("add")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.argument("item", IdentifierArgument.id())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(), builder))
                        .executes(ArmorCommand::executeAdd)
                    )
                )
                // /armor remove <item> (OP only)
                .then(Commands.literal("remove")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.argument("item", IdentifierArgument.id())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(ArmorConfig.getItems(), builder))
                        .executes(ArmorCommand::executeRemove)
                    )
                )
                // /armor hide & /armor hide all
                .then(Commands.literal("hide")
                    .then(Commands.literal("all")
                        .executes(ArmorCommand::executeHideAll)
                    )
                    .executes(ArmorCommand::executeHideMainHand)
                )
                // /armor unhide & /armor unhide all
                .then(Commands.literal("unhide")
                    .then(Commands.literal("all")
                        .executes(ArmorCommand::executeUnhideAll)
                    )
                    .executes(ArmorCommand::executeUnhideMainHand)
                )
                // /armor code <0|1>x4 (e.g. 0101)
                .then(Commands.literal("code")
                    .then(Commands.argument("code", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(ArmorCommand::executeCode)
                    )
                )
        );
    }

    private static boolean isArmorEnabled(CommandSourceStack source) {
        if (!net.chillioil.management.ModuleConfig.isModuleEnabled("armor")) {
            source.sendFailure(Component.literal("The 'armor' module is currently disabled.").withStyle(ChatFormatting.RED));
            return false;
        }
        return true;
    }

    private static int executeList(CommandContext<CommandSourceStack> context) {
        List<Identifier> items = ArmorConfig.getItems();
        CommandSourceStack source = context.getSource();

        if (items.isEmpty()) {
            source.sendSuccess(() -> Component.literal("armor.json is currently empty.").withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("=== armor.json Items (" + items.size() + ") ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            sb.append(items.get(i).toString());
            if (i < items.size() - 1) {
                sb.append(", ");
            }
        }
        String listText = sb.toString();
        source.sendSuccess(() -> Component.literal(listText).withStyle(ChatFormatting.AQUA), false);
        return items.size();
    }

    private static int executeAdd(CommandContext<CommandSourceStack> context) {
        Identifier id = IdentifierArgument.getId(context, "item");
        CommandSourceStack source = context.getSource();

        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            source.sendFailure(Component.literal("Item '" + id + "' does not exist in the Minecraft registry.").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (ArmorConfig.add(id)) {
            source.sendSuccess(() -> Component.literal("Added '").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(id.toString()).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("' to armor.json.").withStyle(ChatFormatting.GREEN)), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Item '" + id + "' is already present in armor.json.").withStyle(ChatFormatting.YELLOW));
            return 0;
        }
    }

    private static int executeRemove(CommandContext<CommandSourceStack> context) {
        Identifier id = IdentifierArgument.getId(context, "item");
        CommandSourceStack source = context.getSource();

        if (ArmorConfig.remove(id)) {
            source.sendSuccess(() -> Component.literal("Removed '").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(id.toString()).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("' from armor.json.").withStyle(ChatFormatting.GREEN)), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Item '" + id + "' was not found in armor.json.").withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int executeHideMainHand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!isArmorEnabled(source)) return 0;
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("This command can only be executed by a player.").withStyle(ChatFormatting.RED));
            return 0;
        }

        ItemStack handItem = player.getMainHandItem();
        if (handItem.isEmpty()) {
            source.sendFailure(Component.literal("You must be holding an item in your main hand to hide it.").withStyle(ChatFormatting.RED));
            return 0;
        }

        Identifier id = BuiltInRegistries.ITEM.getKey(handItem.getItem());
        if (!ArmorConfig.contains(id)) {
            source.sendFailure(Component.literal("hiding this item isnt allowed/possible").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!ArmorTagHelper.canBeHidden(handItem)) {
            source.sendFailure(Component.literal("this item cannot be hidden").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (ArmorTagHelper.isInvisible(handItem)) {
            source.sendFailure(Component.literal("The held item is already invisible.").withStyle(ChatFormatting.YELLOW));
            return 0;
        }

        ArmorTagHelper.setInvisible(handItem, true);
        ArmorTagHelper.resendEquipment(player);

        source.sendSuccess(() -> Component.literal("Model for '").withStyle(ChatFormatting.GREEN)
            .append(Component.literal(handItem.getHoverName().getString()).withStyle(ChatFormatting.AQUA))
            .append(Component.literal("' is now invisible.").withStyle(ChatFormatting.GREEN)), false);
        return 1;
    }

    private static int executeUnhideMainHand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!isArmorEnabled(source)) return 0;
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("This command can only be executed by a player.").withStyle(ChatFormatting.RED));
            return 0;
        }

        ItemStack handItem = player.getMainHandItem();
        if (handItem.isEmpty()) {
            source.sendFailure(Component.literal("You must be holding an item in your main hand to unhide it.").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!ArmorTagHelper.isInvisible(handItem)) {
            source.sendFailure(Component.literal("The held item is not currently invisible.").withStyle(ChatFormatting.YELLOW));
            return 0;
        }

        ArmorTagHelper.setInvisible(handItem, false);
        ArmorTagHelper.resendEquipment(player);

        source.sendSuccess(() -> Component.literal("Model for '").withStyle(ChatFormatting.GREEN)
            .append(Component.literal(handItem.getHoverName().getString()).withStyle(ChatFormatting.AQUA))
            .append(Component.literal("' is now visible.").withStyle(ChatFormatting.GREEN)), false);
        return 1;
    }

    private static int executeHideAll(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!isArmorEnabled(source)) return 0;
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("This command can only be executed by a player.").withStyle(ChatFormatting.RED));
            return 0;
        }

        int count = 0;
        for (EquipmentSlot slot : EQUIPPED_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (ArmorConfig.contains(id) && ArmorTagHelper.canBeHidden(stack)) {
                    if (!ArmorTagHelper.isInvisible(stack)) {
                        ArmorTagHelper.setInvisible(stack, true);
                        count++;
                    }
                }
            }
        }

        if (count > 0) {
            ArmorTagHelper.resendEquipment(player);
            final int hiddenCount = count;
            source.sendSuccess(() -> Component.literal("Hidden models for " + hiddenCount + " equipped item(s).").withStyle(ChatFormatting.GREEN), false);
            return count;
        } else {
            source.sendFailure(Component.literal("No unhidden equipped items matching armor.json were found.").withStyle(ChatFormatting.YELLOW));
            return 0;
        }
    }

    private static int executeUnhideAll(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!isArmorEnabled(source)) return 0;

        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("This command can only be executed by a player.").withStyle(ChatFormatting.RED));
            return 0;
        }

        int count = 0;
        for (EquipmentSlot slot : EQUIPPED_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && ArmorTagHelper.isInvisible(stack)) {
                ArmorTagHelper.setInvisible(stack, false);
                count++;
            }
        }

        if (count > 0) {
            ArmorTagHelper.resendEquipment(player);
            final int unhiddenCount = count;
            source.sendSuccess(() -> Component.literal("Unhidden models for " + unhiddenCount + " equipped item(s).").withStyle(ChatFormatting.GREEN), false);
            return count;
        } else {
            source.sendFailure(Component.literal("No hidden equipped items were found.").withStyle(ChatFormatting.YELLOW));
            return 0;
        }
    }

    private static final EquipmentSlot[] ARMOR_PIECES_TOP_TO_BOTTOM = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    };

    private static final String[] ARMOR_PIECE_NAMES = {
        "Helmet",
        "Chestplate",
        "Leggings",
        "Boots"
    };

    private static int executeCode(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!isArmorEnabled(source)) return 0;

        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("This command can only be executed by a player.").withStyle(ChatFormatting.RED));
            return 0;
        }

        String code = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "code").trim();
        if (code.length() != 4 || !code.matches("[01]{4}")) {
            source.sendFailure(Component.literal("Invalid code! Please provide a 4-digit binary code (e.g. /armor code 0101).")
                .withStyle(ChatFormatting.RED)
                .append(Component.literal("\n0 = Hidden, 1 = Visible (Slots: Helmet, Chestplate, Leggings, Boots)")
                    .withStyle(ChatFormatting.GRAY)));
            return 0;
        }

        int changedCount = 0;
        StringBuilder statusReport = new StringBuilder();

        for (int i = 0; i < 4; i++) {
            char bit = code.charAt(i);
            EquipmentSlot slot = ARMOR_PIECES_TOP_TO_BOTTOM[i];
            String slotName = ARMOR_PIECE_NAMES[i];
            ItemStack stack = player.getItemBySlot(slot);

            if (stack.isEmpty()) {
                continue;
            }

            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!ArmorConfig.contains(id) || !ArmorTagHelper.canBeHidden(stack)) {
                continue;
            }

            boolean shouldBeInvisible = (bit == '0');
            boolean isCurrentlyInvisible = ArmorTagHelper.isInvisible(stack);

            if (shouldBeInvisible != isCurrentlyInvisible) {
                ArmorTagHelper.setInvisible(stack, shouldBeInvisible);
                changedCount++;
            }

            if (statusReport.length() > 0) {
                statusReport.append(", ");
            }
            statusReport.append(slotName).append(": ").append(shouldBeInvisible ? "Hidden" : "Visible");
        }

        ArmorTagHelper.resendEquipment(player);

        if (statusReport.length() == 0) {
            source.sendFailure(Component.literal("No compatible armor pieces are currently equipped in armor slots.")
                .withStyle(ChatFormatting.YELLOW));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Applied armor code [")
            .withStyle(ChatFormatting.GREEN)
            .append(Component.literal(code).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
            .append(Component.literal("] -> " + statusReport).withStyle(ChatFormatting.GREEN)), false);

        return 1;
    }
}
