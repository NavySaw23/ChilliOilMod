package net.chillioil.management;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.chillioil.aesthetic.armor.ArmorConfig;
import net.chillioil.aesthetic.skin.SkinWardrobeConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Map;

public class ChilliConfigCommand {
    private static final List<String> MODULE_NAMES = List.of("armor", "skin", "wardrobe");
    private static final List<String> RESETTABLE_MODULES = List.of("armor", "skin", "wardrobe", "modules");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(
            Commands.literal("chilliconfig")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                // /chilliconfig (opens GUI)
                .executes(ChilliConfigCommand::executeGui)
                // /chilliconfig reload
                .then(Commands.literal("reload")
                    .executes(ChilliConfigCommand::executeReload)
                )
                // /chilliconfig list
                .then(Commands.literal("list")
                    .executes(ChilliConfigCommand::executeList)
                )
                // /chilliconfig module <modulename> <true/false>
                .then(Commands.literal("module")
                    .then(Commands.argument("modulename", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MODULE_NAMES, builder))
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                            .executes(ChilliConfigCommand::executeToggleModule)
                        )
                    )
                )
                // /chilliconfig reset <modulename>
                .then(Commands.literal("reset")
                    .then(Commands.argument("modulename", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RESETTABLE_MODULES, builder))
                        .executes(ChilliConfigCommand::executeResetModule)
                    )
                )
        );
    }

    private static int executeGui(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        net.minecraft.server.level.ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Only players can open the GUI. Use subcommands (/chilliconfig list, reload, module, reset) from console.")
                .withStyle(ChatFormatting.RED));
            return 0;
        }

        ChilliConfigGui.open(player);
        return 1;
    }

    private static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ArmorConfig.load();
        SkinWardrobeConfig.load();
        ModuleConfig.load();

        source.sendSuccess(() -> Component.literal("ChilliOil configurations (armor, skin-wardrobe, modules) reloaded from disk.")
            .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int executeList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Map<String, ModuleConfig.ModuleEntry> modules = ModuleConfig.getModules();

        MutableComponent header = Component.literal("\n=== ChilliOil Modules (" + modules.size() + ") ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        source.sendSuccess(() -> header, false);

        for (Map.Entry<String, ModuleConfig.ModuleEntry> entry : modules.entrySet()) {
            String name = entry.getKey();
            ModuleConfig.ModuleEntry mod = entry.getValue();

            MutableComponent line = Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY);
            line.append(Component.literal("/" + name).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
            line.append(Component.literal(" "));

            if (mod.enabled) {
                line.append(Component.literal("[ON]").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            } else {
                line.append(Component.literal("[OFF]").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            }

            line.append(Component.literal(" - " + mod.description).withStyle(ChatFormatting.GRAY));
            source.sendSuccess(() -> line, false);
        }

        return modules.size();
    }

    private static int executeToggleModule(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String moduleName = StringArgumentType.getString(context, "modulename").toLowerCase();
        boolean enabled = BoolArgumentType.getBool(context, "enabled");

        if (!MODULE_NAMES.contains(moduleName)) {
            source.sendFailure(Component.literal("Unknown module '" + moduleName + "'. Available modules: " + String.join(", ", MODULE_NAMES))
                .withStyle(ChatFormatting.RED));
            return 0;
        }

        ModuleConfig.setModuleEnabled(moduleName, enabled);

        source.sendSuccess(() -> Component.literal("Module '")
            .withStyle(ChatFormatting.GREEN)
            .append(Component.literal(moduleName).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
            .append(Component.literal("' is now "))
            .append(enabled 
                ? Component.literal("ENABLED").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                : Component.literal("DISABLED").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
            .append(Component.literal(".")), true);

        return 1;
    }

    private static int executeResetModule(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String moduleName = StringArgumentType.getString(context, "modulename").toLowerCase();

        switch (moduleName) {
            case "armor" -> {
                ArmorConfig.resetDefaults();
                source.sendSuccess(() -> Component.literal("Reset 'armor' module configuration (armor.json) to default settings.")
                    .withStyle(ChatFormatting.GREEN), true);
                return 1;
            }
            case "skin", "wardrobe" -> {
                SkinWardrobeConfig.resetDefaults();
                source.sendSuccess(() -> Component.literal("Reset 'skin' and 'wardrobe' configuration (skin-wardrobe.json) to default settings.")
                    .withStyle(ChatFormatting.GREEN), true);
                return 1;
            }
            case "modules" -> {
                ModuleConfig.resetDefaults();
                source.sendSuccess(() -> Component.literal("Reset module toggle states (modules.json) to all enabled.")
                    .withStyle(ChatFormatting.GREEN), true);
                return 1;
            }
            default -> {
                source.sendFailure(Component.literal("Unknown module '" + moduleName + "'. Valid modules to reset: " + String.join(", ", RESETTABLE_MODULES))
                    .withStyle(ChatFormatting.RED));
                return 0;
            }
        }
    }
}
