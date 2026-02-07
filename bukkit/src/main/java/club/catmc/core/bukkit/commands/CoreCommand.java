package club.catmc.core.bukkit.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * Core command for the Bukkit plugin
 */
@CommandAlias("core")
@CommandPermission("core.command")
@Description("Core plugin commands")
public class CoreCommand extends BaseCommand {

    /**
     * Default command handler
     *
     * @param player The player executing the command
     */
    @Default
    @Description("Shows core plugin help")
    public void onDefault(Player player) {
        player.sendMessage(Component.text("Hello from Core Bukkit Plugin!", NamedTextColor.GREEN));
        player.sendMessage(Component.text("Usage: /core <reload|version>", NamedTextColor.YELLOW));
    }

    /**
     * Reload command handler
     *
     * @param player The player executing the command
     */
    @Subcommand("reload")
    @Syntax("")
    @Description("Reloads the plugin configuration")
    public void onReload(Player player) {
        player.sendMessage(Component.text("Configuration reloaded!", NamedTextColor.GREEN));
    }

    /**
     * Version command handler
     *
     * @param player The player executing the command
     */
    @Subcommand("version")
    @Description("Shows plugin version information")
    public void onVersion(Player player) {
        player.sendMessage(Component.text("Core Bukkit Plugin v1.0.0", NamedTextColor.AQUA));
        player.sendMessage(Component.text("Powered by ACF", NamedTextColor.GRAY));
    }
}
