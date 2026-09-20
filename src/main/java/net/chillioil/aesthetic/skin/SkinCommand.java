package net.chillioil.aesthetic.skin;

import com.mojang.brigadier.CommandDispatcher;
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

public class SkinCommand {
    private static final List<String> VARIANTS = List.of(SkinData.VARIANT_CLASSIC, SkinData.VARIANT_SLIM);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(
            Commands.literal("skin")
                // /skin user <type> <username>
                .then(Commands.literal("user")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(VARIANTS, builder))
                        .then(Commands.argument("username", StringArgumentType.word())
                            .executes(SkinCommand::executeUser)
                        )
                    )
                )
                // /skin web <type> <url>
                .then(Commands.literal("web")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(VARIANTS, builder))
                        .then(Commands.argument("url", StringArgumentType.string())
                            .executes(SkinCommand::executeWeb)
                        )
                    )
                )
                // /skin info
                .then(Commands.literal("info")
                    .executes(SkinCommand::executeInfo)
                )
                // /skin style <type>
                .then(Commands.literal("style")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(VARIANTS, builder))
                        .executes(SkinCommand::executeStyle)
                    )
                )
                // /skin clear
                .then(Commands.literal("clear")
                    .executes(SkinCommand::executeClear)
                )
        );
    }

    private static boolean isSkinEnabled(CommandSourceStack source) {
        if (!net.chillioil.management.ModuleConfig.isModuleEnabled("skin")) {
            source.sendFailure(Component.literal("The 'skin' module is currently disabled.").withStyle(ChatFormatting.RED));
            return false;
        }
        return true;
    }

    private static boolean checkCooldown(ServerPlayer player, CommandSourceStack source) {
        int remaining = PlayerSkinState.getRemainingCooldown(player.getUUID());
        if (remaining > 0) {
            source.sendFailure(Component.literal("Please wait " + remaining + " second(s) before changing skins again.")
                .withStyle(ChatFormatting.RED));
            return false;
        }
        return true;
    }

    private static boolean isValidVariant(String variant) {
        return SkinData.VARIANT_CLASSIC.equalsIgnoreCase(variant) || SkinData.VARIANT_SLIM.equalsIgnoreCase(variant);
    }

    private static int executeUser(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!isSkinEnabled(source)) return 0;
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Only players can execute this command.").withStyle(ChatFormatting.RED));
            return 0;
        }

        String type = StringArgumentType.getString(context, "type").toLowerCase();
        if (!isValidVariant(type)) {
            source.sendFailure(Component.literal("Invalid type '" + type + "'. Please choose 'classic' or 'slim'.").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!checkCooldown(player, source)) {
            return 0;
        }

        String targetUser = StringArgumentType.getString(context, "username");
        source.sendSuccess(() -> Component.literal("Fetching skin for Mojang user '" + targetUser + "'...").withStyle(ChatFormatting.YELLOW), false);

        MojangSkinFetcher.fetchSkin(targetUser, type).thenAccept(optSkin -> {
            player.level().getServer().execute(() -> {
                if (optSkin.isPresent()) {
                    SkinData skin = optSkin.get();
                    if (SkinApplier.applySkin(player, skin)) {
                        source.sendSuccess(() -> Component.literal("Successfully applied skin of '")
                            .withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(targetUser).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal("' (" + type + ")!").withStyle(ChatFormatting.GREEN)), false);
                    } else {
                        source.sendFailure(Component.literal("Failed to apply skin to player.").withStyle(ChatFormatting.RED));
                    }
                } else {
                    source.sendFailure(Component.literal("Could not find or fetch skin for Mojang user '" + targetUser + "'.").withStyle(ChatFormatting.RED));
                }
            });
        });

        return 1;
    }

    private static int executeWeb(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!isSkinEnabled(source)) return 0;
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Only players can execute this command.").withStyle(ChatFormatting.RED));
            return 0;
        }

        String type = StringArgumentType.getString(context, "type").toLowerCase();
        if (!isValidVariant(type)) {
            source.sendFailure(Component.literal("Invalid type '" + type + "'. Please choose 'classic' or 'slim'.").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!checkCooldown(player, source)) {
            return 0;
        }

        String rawUrl = StringArgumentType.getString(context, "url");
        source.sendSuccess(() -> Component.literal("Validating image at URL...").withStyle(ChatFormatting.YELLOW), false);

        MineSkinFetcher.validateSkinUrl(rawUrl).thenAccept(validation -> {
            player.level().getServer().execute(() -> {
                if (!validation.valid()) {
                    source.sendFailure(Component.literal("Validation failed: " + validation.message()).withStyle(ChatFormatting.RED));
                    return;
                }

                source.sendSuccess(() -> Component.literal("Generating Mojang signature via MineSkin... Please wait.").withStyle(ChatFormatting.YELLOW), false);

                MineSkinFetcher.fetchSkinFromUrl(rawUrl, type).thenAccept(optSkin -> {
                    player.level().getServer().execute(() -> {
                        if (optSkin.isPresent()) {
                            SkinData skin = optSkin.get();
                            if (SkinApplier.applySkin(player, skin)) {
                                source.sendSuccess(() -> Component.literal("Successfully applied web skin (" + type + ")!").withStyle(ChatFormatting.GREEN), false);
                            } else {
                                source.sendFailure(Component.literal("Failed to apply web skin to player.").withStyle(ChatFormatting.RED));
                            }
                        } else {
                            source.sendFailure(Component.literal("Failed to generate signed skin from URL via MineSkin. The server may be busy or rate-limited.").withStyle(ChatFormatting.RED));
                        }
                    });
                });
            });
        });

        return 1;
    }

    private static int executeInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!isSkinEnabled(source)) return 0;
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Only players can execute this command.").withStyle(ChatFormatting.RED));
            return 0;
        }

        SkinData activeSkin = PlayerSkinState.getActiveSkin(player.getUUID());
        if (activeSkin == null) {
            source.sendSuccess(() -> Component.literal("You are currently using your default Mojang account skin.").withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }

        String copyCommand;
        if ("MOJANG_USER".equalsIgnoreCase(activeSkin.sourceType())) {
            copyCommand = "/skin user " + activeSkin.variant() + " " + activeSkin.source();
        } else if ("WEB_URL".equalsIgnoreCase(activeSkin.sourceType())) {
            copyCommand = "/skin web " + activeSkin.variant() + " \"" + activeSkin.source() + "\"";
        } else {
            copyCommand = "/skin clear";
        }

        MutableComponent header = Component.literal("=== Current Skin Information ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        MutableComponent details = Component.literal("\nType: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(activeSkin.variant()).withStyle(ChatFormatting.AQUA))
            .append(Component.literal("\nSource: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(activeSkin.source()).withStyle(ChatFormatting.WHITE));

        MutableComponent clickToCopy = Component.literal("\n[Click here to copy skin command]")
            .withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.CopyToClipboard(copyCommand))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy: " + copyCommand)))
            );

        source.sendSuccess(() -> header.append(details).append(clickToCopy), false);
        return 1;
    }

    private static int executeStyle(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!isSkinEnabled(source)) return 0;
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Only players can execute this command.").withStyle(ChatFormatting.RED));
            return 0;
        }

        String newVariant = StringArgumentType.getString(context, "type").toLowerCase();
        if (!isValidVariant(newVariant)) {
            source.sendFailure(Component.literal("Invalid type '" + newVariant + "'. Please choose 'classic' or 'slim'.").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!checkCooldown(player, source)) {
            return 0;
        }

        SkinData currentSkin = PlayerSkinState.getActiveSkin(player.getUUID());
        if (currentSkin == null) {
            source.sendFailure(Component.literal("Could not detect active skin data to restyle.").withStyle(ChatFormatting.RED));
            return 0;
        }

        SkinData updatedSkin = new SkinData(
            newVariant,
            currentSkin.source(),
            currentSkin.sourceType(),
            currentSkin.value(),
            currentSkin.signature()
        );

        if (SkinApplier.applySkin(player, updatedSkin)) {
            source.sendSuccess(() -> Component.literal("Switched skin model style to '")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(newVariant).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("'!").withStyle(ChatFormatting.GREEN)), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to change skin style.").withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int executeClear(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!isSkinEnabled(source)) return 0;
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Only players can execute this command.").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!checkCooldown(player, source)) {
            return 0;
        }

        SkinData defaultSkin = PlayerSkinState.getDefaultSkin(player.getUUID());
        if (defaultSkin != null) {
            SkinApplier.applySkin(player, defaultSkin);
            source.sendSuccess(() -> Component.literal("Reset skin back to your account default.").withStyle(ChatFormatting.GREEN), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Original default skin was not found.").withStyle(ChatFormatting.RED));
            return 0;
        }
    }
}
