package club.catmc.core.bungee.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Core command for the BungeeCord plugin
 */
@CommandAlias("bcore|bc")
@CommandPermission("core.command")
@Description("Core plugin commands")
public class CoreCommand extends BaseCommand {

    /**
     * Default command handler
     *
     * @param sender The command sender
     */
    @Default
    @Description("Shows core plugin help")
    public void onDefault(CommandSender sender) {
        sender.sendMessage(new TextComponent("§aHello from Core Bungee Plugin!"));
        sender.sendMessage(new TextComponent("§eUsage: /bcore <reload|version|info>"));

        if (sender instanceof ProxiedPlayer player) {
            sender.sendMessage(new TextComponent("§7You are connected to: " + player.getServer().getInfo().getName()));
        }
    }

    /**
     * Reload command handler
     *
     * @param sender The command sender
     */
    @Subcommand("reload")
    @Syntax("")
    @Description("Reloads the plugin configuration")
    public void onReload(CommandSender sender) {
        sender.sendMessage(new TextComponent("§aConfiguration reloaded!"));
    }

    /**
     * Version command handler
     *
     * @param sender The command sender
     */
    @Subcommand("version")
    @Description("Shows plugin version information")
    public void onVersion(CommandSender sender) {
        sender.sendMessage(new TextComponent("§bCore Bungee Plugin v1.0.0"));
        sender.sendMessage(new TextComponent("§7Powered by ACF"));
    }

    /**
     * Info command handler
     *
     * @param sender The command sender
     */
    @Subcommand("info")
    @Description("Shows server information")
    public void onInfo(CommandSender sender) {
        sender.sendMessage(new TextComponent("§6=== Core Plugin Info ==="));
        sender.sendMessage(new TextComponent("§7This is a shared Bukkit/Bungee plugin"));
        sender.sendMessage(new TextComponent("§7Author: CatMC"));
    }
}
