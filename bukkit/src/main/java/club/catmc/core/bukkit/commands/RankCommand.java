package club.catmc.core.bukkit.commands;

import club.catmc.core.bukkit.BukkitPlugin;
import club.catmc.core.bukkit.dialogs.CreateRankDialog;
import club.catmc.core.bukkit.dialogs.DeleteRankDialog;
import club.catmc.core.bukkit.dialogs.EditRankDialog;
import club.catmc.core.bukkit.dialogs.RankHubDialog;
import club.catmc.core.bukkit.dialogs.RankListDialog;
import club.catmc.core.shared.rank.Rank;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import io.papermc.paper.dialog.DialogResponseView;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.List;

/**
 * Rank command with dialog-based UI for rank management
 */
@CommandAlias("rank")
@CommandPermission("core.command.rank")
@Description("Manage server ranks")
public class RankCommand extends BaseCommand {

    private final Plugin plugin;

    public RankCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Default command - shows the rank hub dialog
     *
     * @param player The player executing the command
     */
    @Default
    @Description("Manage ranks")
    public void onDefault(Player player) {
        RankHubDialog.show(
                player,
                () -> CreateRankDialog.show(player, this::handleRankCreate),
                () -> showRankList(player)
        );
    }

    /**
     * Shows a dialog to create a new rank
     *
     * @param player The player executing the command
     */
    @Subcommand("create")
    @Description("Create a new rank")
    public void onCreate(Player player) {
        CreateRankDialog.show(player, this::handleRankCreate);
    }

    /**
     * Lists all ranks in a dialog
     *
     * @param player The player executing the command
     */
    @Subcommand("list")
    @Description("List all ranks")
    public void onList(Player player) {
        showRankList(player);
    }

    /**
     * Shows the rank list dialog
     *
     * @param player The player to show the dialog to
     */
    private void showRankList(Player player) {
        BukkitPlugin bukkitPlugin = (BukkitPlugin) plugin;
        Collection<Rank> ranks = bukkitPlugin.getPlayerManager().getAllRanks();

        if (ranks.isEmpty()) {
            player.sendMessage(Component.text("No ranks found. Create one first!", NamedTextColor.YELLOW));
            return;
        }

        RankListDialog.show(
                player,
                ranks,
                rank -> EditRankDialog.show(player, rank, (view, audience) -> handleRankEdit(rank, view, audience)),
                () -> onDefault(player)
        );
    }

    /**
     * Edits a specific rank by ID
     *
     * @param player The player executing the command
     * @param id The rank ID to edit
     */
    @Subcommand("edit")
    @Syntax("<rank id>")
    @Description("Edit a rank")
    public void onEdit(Player player, String id) {
        BukkitPlugin bukkitPlugin = (BukkitPlugin) plugin;

        bukkitPlugin.getRankDao().findById(id).thenAccept(rankOpt -> {
            if (rankOpt.isEmpty()) {
                player.sendMessage(Component.text("Rank not found: " + id, NamedTextColor.RED));
                return;
            }

            Rank rank = rankOpt.get();
            EditRankDialog.show(player, rank, (view, audience) -> handleRankEdit(rank, view, audience));
        }).exceptionally(e -> {
            player.sendMessage(Component.text("Failed to load rank: " + e.getMessage(), NamedTextColor.RED));
            return null;
        });
    }

    /**
     * Deletes a rank
     *
     * @param player The player executing the command
     * @param id The rank ID to delete
     */
    @Subcommand("delete")
    @Syntax("<rank id>")
    @Description("Delete a rank")
    public void onDelete(Player player, String id) {
        BukkitPlugin bukkitPlugin = (BukkitPlugin) plugin;

        bukkitPlugin.getRankDao().findById(id).thenAccept(rankOpt -> {
            if (rankOpt.isEmpty()) {
                player.sendMessage(Component.text("Rank not found: " + id, NamedTextColor.RED));
                return;
            }

            Rank rank = rankOpt.get();
            DeleteRankDialog.show(player, rank, (view, audience) -> handleRankDelete(id, view, audience));
        }).exceptionally(e -> {
            player.sendMessage(Component.text("Failed to load rank: " + e.getMessage(), NamedTextColor.RED));
            return null;
        });
    }

    /**
     * Handles the rank creation from dialog input
     *
     * @param view The dialog response view containing input values
     * @param audience The audience (player)
     */
    private void handleRankCreate(DialogResponseView view, Audience audience) {
        if (!(audience instanceof Player player)) {
            return;
        }

        try {
            // Extract values from dialog inputs
            String id = view.getText("id");
            String name = view.getText("name");
            String displayName = view.getText("displayName");
            String prefix = view.getText("prefix");
            String suffix = view.getText("suffix");
            int priority = view.getFloat("priority").intValue();
            boolean defaultRank = view.getBoolean("defaultRank");

            // Validate required fields
            if (id == null || id.isBlank()) {
                player.sendMessage(Component.text("Rank ID cannot be empty!", NamedTextColor.RED));
                return;
            }
            if (name == null || name.isBlank()) {
                player.sendMessage(Component.text("Rank name cannot be empty!", NamedTextColor.RED));
                return;
            }

            // Normalize ID (lowercase, alphanumeric + underscore only)
            id = id.toLowerCase().replaceAll("[^a-z0-9_]", "_");

            // Use name as display name if not provided
            if (displayName == null || displayName.isBlank()) {
                displayName = name;
            }

            // Create and save rank via the plugin
            Rank rank = new Rank(
                    id,
                    name,
                    displayName,
                    prefix,
                    suffix,
                    priority,
                    defaultRank,
                    List.of()
            );

            // Get the RankDao from the plugin instance
            BukkitPlugin bukkitPlugin = (BukkitPlugin) plugin;
            bukkitPlugin.getRankDao().save(rank).thenRun(() -> {
                player.sendMessage(Component.text("Successfully created rank: ", NamedTextColor.GREEN)
                        .append(Component.text(rank.getDisplayName(), NamedTextColor.GOLD)));
                player.sendMessage(Component.text("  ID: ", NamedTextColor.GRAY)
                        .append(Component.text(rank.getId(), NamedTextColor.WHITE)));
                player.sendMessage(Component.text("  Priority: ", NamedTextColor.GRAY)
                        .append(Component.text(String.valueOf(rank.getPriority()), NamedTextColor.WHITE)));
                player.sendMessage(Component.text("  Default: ", NamedTextColor.GRAY)
                        .append(Component.text(String.valueOf(rank.isDefaultRank()), NamedTextColor.WHITE)));

                // Refresh rank cache
                bukkitPlugin.getPlayerManager().refreshRankCache();
            }).exceptionally(e -> {
                player.sendMessage(Component.text("Failed to create rank: " + e.getMessage(), NamedTextColor.RED));
                plugin.getLogger().severe("Failed to create rank: " + e.getMessage());
                e.printStackTrace();
                return null;
            });

        } catch (Exception e) {
            player.sendMessage(Component.text("Error processing rank creation: " + e.getMessage(), NamedTextColor.RED));
            plugin.getLogger().severe("Error processing rank creation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles the rank edit from dialog input
     *
     * @param originalRank The original rank being edited
     * @param view The dialog response view containing input values
     * @param audience The audience (player)
     */
    @SuppressWarnings("unused")
    private void handleRankEdit(Rank originalRank, DialogResponseView view, Audience audience) {
        if (!(audience instanceof Player player)) {
            return;
        }

        try {
            // Extract values from dialog inputs
            String id = view.getText("id"); // This should match original rank's ID
            String name = view.getText("name");
            String displayName = view.getText("displayName");
            String prefix = view.getText("prefix");
            String suffix = view.getText("suffix");
            int priority = view.getFloat("priority").intValue();
            boolean defaultRank = view.getBoolean("defaultRank");

            // Validate required fields
            if (name == null || name.isBlank()) {
                player.sendMessage(Component.text("Rank name cannot be empty!", NamedTextColor.RED));
                return;
            }

            // Use name as display name if not provided
            if (displayName == null || displayName.isBlank()) {
                displayName = name;
            }

            // Update the rank with new values
            originalRank.setName(name);
            originalRank.setDisplayName(displayName);
            originalRank.setPrefix(prefix);
            originalRank.setSuffix(suffix);
            originalRank.setPriority(priority);
            originalRank.setDefaultRank(defaultRank);

            // Save the updated rank
            BukkitPlugin bukkitPlugin = (BukkitPlugin) plugin;
            bukkitPlugin.getRankDao().save(originalRank).thenRun(() -> {
                player.sendMessage(Component.text("Successfully updated rank: ", NamedTextColor.GREEN)
                        .append(Component.text(originalRank.getDisplayName(), NamedTextColor.GOLD)));

                // Refresh rank cache
                bukkitPlugin.getPlayerManager().refreshRankCache();
            }).exceptionally(e -> {
                player.sendMessage(Component.text("Failed to update rank: " + e.getMessage(), NamedTextColor.RED));
                plugin.getLogger().severe("Failed to update rank: " + e.getMessage());
                e.printStackTrace();
                return null;
            });

        } catch (Exception e) {
            player.sendMessage(Component.text("Error processing rank update: " + e.getMessage(), NamedTextColor.RED));
            plugin.getLogger().severe("Error processing rank update: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles the rank deletion
     *
     * @param id The rank ID to delete
     * @param view The dialog response view
     * @param audience The audience (player)
     */
    private void handleRankDelete(String id, DialogResponseView view, Audience audience) {
        if (!(audience instanceof Player player)) {
            return;
        }

        BukkitPlugin bukkitPlugin = (BukkitPlugin) plugin;

        bukkitPlugin.getRankDao().deleteById(id).thenRun(() -> {
            player.sendMessage(Component.text("Successfully deleted rank: " + id, NamedTextColor.GREEN));
            bukkitPlugin.getPlayerManager().refreshRankCache();
        }).exceptionally(e -> {
            player.sendMessage(Component.text("Failed to delete rank: " + e.getMessage(), NamedTextColor.RED));
            return null;
        });
    }
}
