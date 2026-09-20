package net.chillioil.aesthetic.skin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WardrobeCommand {
    private static final int ITEMS_PER_PAGE = 5;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(
            Commands.literal("wardrobe")
                // /wardrobe (defaults to page 1)
                .executes(ctx -> executeShowPage(ctx, 1))
                // /wardrobe <pageNumber>
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> executeShowPage(ctx, IntegerArgumentType.getInteger(ctx, "page")))
                )
                // /wardrobe wear <name>
                .then(Commands.literal("wear")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            if (p != null) {
                                List<String> names = WardrobeStorage.getEntries(p.getUUID())
                                    .stream()
                                    .map(Map.Entry::getKey)
                                    .toList();
                                return SharedSuggestionProvider.suggest(names, builder);
                            }
                            return builder.buildFuture();
                        })
                        .executes(WardrobeCommand::executeWear)
                    )
                )
                // /wardrobe add <name>
                .then(Commands.literal("add")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(WardrobeCommand::executeAdd)
                    )
                )
                // /wardrobe remove <name>
                .then(Commands.literal("remove")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            if (p != null) {
                                List<String> names = WardrobeStorage.getEntries(p.getUUID())
                                    .stream()
                                    .map(Map.Entry::getKey)
                                    .toList();
                                return SharedSuggestionProvider.suggest(names, builder);
                            }
                            return builder.buildFuture();
                        })
                        .executes(WardrobeCommand::executeRemove)
                    )
                )
        );
    }

    private static boolean isWardrobeEnabled(CommandSourceStack source) {
        if (!net.chillioil.management.ModuleConfig.isModuleEnabled("wardrobe")) {
            source.sendFailure(Component.literal("The 'wardrobe' module is currently disabled.").withStyle(ChatFormatting.RED));
            return false;
        }
        return true;
    }

    private static int executeShowPage(CommandContext<CommandSourceStack> context, int page) {
        CommandSourceStack source = context.getSource();
        if (!isWardrobeEnabled(source)) return 0;
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Only players can execute this command.").withStyle(ChatFormatting.RED));
            return 0;
        }

        WardrobeGui.open(player, page);
        return 1;
    }

    private static int executeWear(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!isWardrobeEnabled(source)) return 0;
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Only players can execute this command.").withStyle(ChatFormatting.RED));
            return 0;
        }

        int remainingCooldown = PlayerSkinState.getRemainingWardrobeCooldown(player.getUUID());
        if (remainingCooldown > 0) {
            source.sendFailure(Component.literal("Please wait " + remainingCooldown + " second(s) before changing skins again.")
                .withStyle(ChatFormatting.RED));
            return 0;
        }

        String skinName = StringArgumentType.getString(context, "name").toLowerCase();
        Optional<SkinData> optSkin = WardrobeStorage.getSkin(player.getUUID(), skinName);

        if (optSkin.isEmpty()) {
            source.sendFailure(Component.literal("No skin named '" + skinName + "' found in your wardrobe.").withStyle(ChatFormatting.RED));
            return 0;
        }

        SkinData skin = optSkin.get();
        if (SkinApplier.applySkin(player, skin)) {
            source.sendSuccess(() -> Component.literal("Now wearing '")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(skinName).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("' from your wardrobe!").withStyle(ChatFormatting.GREEN)), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to wear wardrobe skin.").withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int executeAdd(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!isWardrobeEnabled(source)) return 0;
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Only players can execute this command.").withStyle(ChatFormatting.RED));
            return 0;
        }

        String skinName = StringArgumentType.getString(context, "name").toLowerCase();
        SkinData currentSkin = PlayerSkinState.getActiveSkin(player.getUUID());

        if (currentSkin == null) {
            source.sendFailure(Component.literal("No active skin data available to save.").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (WardrobeStorage.addSkin(player.getUUID(), skinName, currentSkin)) {
            source.sendSuccess(() -> Component.literal("Added '")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(skinName).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("' (" + currentSkin.variant() + ") to your wardrobe!").withStyle(ChatFormatting.GREEN)), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Could not add skin. Your wardrobe has reached its limit (" + SkinWardrobeConfig.getWardrobeLimit() + " items).")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int executeRemove(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!isWardrobeEnabled(source)) return 0;
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Only players can execute this command.").withStyle(ChatFormatting.RED));
            return 0;
        }

        String skinName = StringArgumentType.getString(context, "name").toLowerCase();
        if (WardrobeStorage.removeSkin(player.getUUID(), skinName)) {
            source.sendSuccess(() -> Component.literal("Removed '")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(skinName).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("' from your wardrobe.").withStyle(ChatFormatting.GREEN)), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("No skin named '" + skinName + "' was found in your wardrobe.").withStyle(ChatFormatting.RED));
            return 0;
        }
    }
}
