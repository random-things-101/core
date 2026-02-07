package club.catmc.core.bukkit.dialogs;

import club.catmc.core.shared.grant.Grant;
import club.catmc.core.shared.rank.Rank;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dialog for listing a player's grants
 */
public class GrantListDialog {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Creates and returns a Grant List dialog
     *
     * @param playerName The player name whose grants are shown
     * @param grants List of grants to display
     * @param ranks Map of rank IDs to Rank objects for display
     * @param onRemove Callback for when a grant removal is requested
     * @param onBack Callback for when back button is pressed
     * @return The configured Dialog
     */
    public static Dialog create(String playerName, List<Grant> grants, java.util.Map<String, Rank> ranks,
                                Consumer<Grant> onRemove, Runnable onBack) {
        // Create action buttons for each grant
        List<ActionButton> grantButtons = new java.util.ArrayList<>();

        if (grants.isEmpty()) {
            grantButtons.add(ActionButton.create(
                    Component.text("No grants found", NamedTextColor.GRAY),
                    Component.text("This player has no ranks", NamedTextColor.DARK_GRAY),
                    200,
                    null
            ));
        } else {
            for (Grant grant : grants) {
                Rank rank = ranks.get(grant.getRankId());
                String rankName = rank != null ? rank.getDisplayName() : grant.getRankId();

                Component label = Component.text()
                        .append(Component.text("• ", NamedTextColor.WHITE))
                        .append(Component.text(rankName, NamedTextColor.GOLD))
                        .append(Component.newline())
                        .append(Component.text("  Granted: " + DATE_FORMATTER.format(grant.getGrantedAt()), NamedTextColor.DARK_GRAY))
                        .append(Component.newline())
                        .append(Component.text("  " + (grant.isPermanent() ? "Permanent" : "Expires: " + DATE_FORMATTER.format(grant.getExpiresAt())),
                                grant.isPermanent() ? NamedTextColor.GREEN : NamedTextColor.YELLOW))
                        .build();

                grantButtons.add(ActionButton.create(
                        label,
                        Component.text("Click to remove this grant", NamedTextColor.DARK_GRAY),
                        300,
                        DialogAction.customClick(
                                (view, audience) -> onRemove.accept(grant),
                                ClickCallback.Options.builder().uses(1).build()
                        )
                ));
            }
        }

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Grants for " + playerName, NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text(
                                        "This player has " + grants.size() + " grant(s).",
                                        NamedTextColor.GRAY
                                ))
                        ))
                        .build())
                .type(DialogType.multiAction(
                        grantButtons,
                        ActionButton.create(
                                Component.text("Back", NamedTextColor.RED),
                                Component.text("Return to grant menu", NamedTextColor.DARK_GRAY),
                                100,
                                onBack == null ? null : DialogAction.customClick(
                                        (view, audience) -> onBack.run(),
                                        ClickCallback.Options.builder().uses(1).build()
                                )
                        ),
                        1
                ))
        );
    }

    /**
     * Shows the Grant List dialog to a player
     *
     * @param player The player to show the dialog to
     * @param playerName The player name whose grants are shown
     * @param grants List of grants to display
     * @param ranks Map of rank IDs to Rank objects for display
     * @param onRemove Callback for when a grant removal is requested
     * @param onBack Callback for when back button is pressed
     */
    public static void show(Player player, String playerName, List<Grant> grants,
                           java.util.Map<String, Rank> ranks, Consumer<Grant> onRemove, Runnable onBack) {
        player.showDialog(create(playerName, grants, ranks, onRemove, onBack));
    }
}
