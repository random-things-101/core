package club.catmc.core.bukkit.commands;

import club.catmc.core.bukkit.BukkitPlugin;
import club.catmc.core.bukkit.dialogs.DeleteRankDialog;
import club.catmc.core.bukkit.dialogs.GrantListDialog;
import club.catmc.core.bukkit.dialogs.GrantPlayerDialog;
import club.catmc.core.shared.grant.Grant;
import club.catmc.core.shared.rank.Rank;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import io.papermc.paper.dialog.DialogResponseView;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Grant command with dialog-based UI for granting ranks to players
 */
@CommandAlias("grant")
@CommandPermission("core.command.grant")
@Description("Grant ranks to players")
public class GrantCommand extends BaseCommand {

    private final Plugin plugin;

    public GrantCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Default command - shows usage/help
     *
     * @param player The player executing the command
     */
    @Default
    @Description("Manage grants")
    public void onDefault(Player player) {
        player.sendMessage(Component.text("Usage: /grant <player> | /grant list <player>", NamedTextColor.YELLOW));
    }

    /**
     * Grant a rank to a player
     *
     * @param player The player executing the command
     * @param targetPlayer The player to receive the grant
     */
    @Default
    @Syntax("<player>")
    @CommandCompletion("players")
    @Description("Grant a rank to a player")
    public void onGrant(Player player, OfflinePlayer targetPlayer) {
        if (targetPlayer == null) {
            player.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return;
        }

        String targetName = targetPlayer.getName() != null ? targetPlayer.getName() : targetPlayer.getUniqueId().toString();
        BukkitPlugin bukkitPlugin = (BukkitPlugin) plugin;

        // Get all ranks for the dialog
        Collection<Rank> ranks = bukkitPlugin.getPlayerManager().getAllRanks();

        if (ranks.isEmpty()) {
            player.sendMessage(Component.text("No ranks found. Create one first!", NamedTextColor.RED));
            return;
        }

        // Show grant dialog
        GrantPlayerDialog.show(
                player,
                targetName,
                ranks,
                (view, audience) -> handleGrant(targetPlayer, view, audience)
        );
    }

    /**
     * Lists all grants for a player
     *
     * @param player The player executing the command
     * @param targetPlayer The player whose grants to list
     */
    @Subcommand("list")
    @Syntax("<player>")
    @CommandCompletion("players")
    @Description("List all grants for a player")
    public void onList(Player player, OfflinePlayer targetPlayer) {
        if (targetPlayer == null) {
            player.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return;
        }

        BukkitPlugin bukkitPlugin = (BukkitPlugin) plugin;

        // Get player's UUID (OfflinePlayer may not have loaded profile yet)
        UUID targetUuid = targetPlayer.getUniqueId();

        // Fetch grants
        bukkitPlugin.getGrantDao().findActiveByPlayer(targetUuid).thenAccept(grants -> {
            // Build rank map for display
            Map<String, Rank> rankMap = new HashMap<>();
            for (Rank rank : bukkitPlugin.getPlayerManager().getAllRanks()) {
                rankMap.put(rank.getId(), rank);
            }

            String targetName = targetPlayer.getName() != null ? targetPlayer.getName() : targetUuid.toString();

            // Show grant list dialog
            GrantListDialog.show(
                    player,
                    targetName,
                    grants,
                    rankMap,
                    grant -> showRemoveGrantDialog(player, targetName, grant, rankMap),
                    () -> onDefault(player)
            );
        }).exceptionally(e -> {
            player.sendMessage(Component.text("Failed to load grants: " + e.getMessage(), NamedTextColor.RED));
            return null;
        });
    }

    /**
     * Handle grant creation from dialog
     */
    private void handleGrant(OfflinePlayer targetPlayer, DialogResponseView view, Audience audience) {
        if (!(audience instanceof Player player)) {
            return;
        }

        try {
            String rankId = view.getText("rankId");
            int durationDays = view.getFloat("duration").intValue();
            String reason = view.getText("reason");

            // Validate rank ID
            if (rankId == null || rankId.isBlank()) {
                player.sendMessage(Component.text("Rank ID cannot be empty!", NamedTextColor.RED));
                return;
            }

            // Get rank from cache to validate
            BukkitPlugin bukkitPlugin = (BukkitPlugin) plugin;
            Rank rank = bukkitPlugin.getPlayerManager().getRank(rankId);
            if (rank == null) {
                player.sendMessage(Component.text("Rank not found: " + rankId, NamedTextColor.RED));
                return;
            }

            // Create grant
            LocalDateTime expiresAt = durationDays > 0
                    ? LocalDateTime.now().plusDays(durationDays)
                    : null;

            Grant grant = new Grant(
                    targetPlayer.getUniqueId(),
                    rankId,
                    player.getUniqueId(),
                    player.getName(),
                    reason != null ? reason : "Granted by " + player.getName(),
                    expiresAt
            );

            bukkitPlugin.getGrantDao().save(grant).thenRun(() -> {
                player.sendMessage(Component.text("Successfully granted rank: ", NamedTextColor.GREEN)
                        .append(Component.text(rank.getDisplayName(), NamedTextColor.GOLD))
                        .append(Component.text(" to ", NamedTextColor.GRAY))
                        .append(Component.text(targetPlayer.getName(), NamedTextColor.WHITE)));

                if (durationDays > 0) {
                    player.sendMessage(Component.text("  Expires in: " + durationDays + " days", NamedTextColor.YELLOW));
                } else {
                    player.sendMessage(Component.text("  Permanent grant", NamedTextColor.GREEN));
                }

                // If target is online, reload their grants
                Player onlineTarget = Bukkit.getPlayer(targetPlayer.getUniqueId());
                if (onlineTarget != null && onlineTarget.isOnline()) {
                    bukkitPlugin.getPlayerManager().reloadPlayerGrants(targetPlayer.getUniqueId());
                }

            }).exceptionally(e -> {
                player.sendMessage(Component.text("Failed to create grant: " + e.getMessage(), NamedTextColor.RED));
                plugin.getLogger().severe("Failed to create grant: " + e.getMessage());
                e.printStackTrace();
                return null;
            });

        } catch (Exception e) {
            player.sendMessage(Component.text("Error processing grant: " + e.getMessage(), NamedTextColor.RED));
            plugin.getLogger().severe("Error processing grant: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Shows confirmation dialog for removing a grant
     */
    private void showRemoveGrantDialog(Player player, String targetPlayerName, Grant grant, Map<String, Rank> rankMap) {
        Rank rank = rankMap.get(grant.getRankId());
        String rankName = rank != null ? rank.getDisplayName() : grant.getRankId();

        // Use a simple confirmation for removing grant
        DeleteRankDialog.show(
                player,
                new club.catmc.core.shared.rank.Rank(
                        grant.getRankId(),
                        rankName,
                        rankName,
                        null,
                        null,
                        rank != null ? rank.getPriority() : 0,
                        false,
                        List.of()
                ),
                (view, audience) -> handleRemoveGrant(grant, targetPlayerName, view, audience)
        );
    }

    /**
     * Handle grant removal
     */
    private void handleRemoveGrant(Grant grant, String targetPlayerName, DialogResponseView view, Audience audience) {
        if (!(audience instanceof Player player)) {
            return;
        }

        BukkitPlugin bukkitPlugin = (BukkitPlugin) plugin;

        bukkitPlugin.getGrantDao().updateActiveStatus(grant.getId(), false).thenRun(() -> {
            player.sendMessage(Component.text("Successfully removed grant from: ", NamedTextColor.GREEN)
                    .append(Component.text(targetPlayerName, NamedTextColor.WHITE)));

            // If target is online, reload their grants
            Player onlineTarget = Bukkit.getPlayer(grant.getPlayerUuid());
            if (onlineTarget != null && onlineTarget.isOnline()) {
                bukkitPlugin.getPlayerManager().reloadPlayerGrants(grant.getPlayerUuid());
            }

        }).exceptionally(e -> {
            player.sendMessage(Component.text("Failed to remove grant: " + e.getMessage(), NamedTextColor.RED));
            return null;
        });
    }
}
