package club.catmc.core.bukkit.dialogs;

import club.catmc.core.shared.rank.Rank;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dialog for granting a rank to a player
 */
public class GrantPlayerDialog {

    /**
     * Creates and returns a Grant Player dialog
     *
     * @param targetPlayerName The name of the player receiving the grant
     * @param ranks Collection of available ranks to choose from
     * @param callback The callback to handle the form submission
     * @return The configured Dialog
     */
    public static Dialog create(String targetPlayerName, Collection<Rank> ranks, DialogActionCallback callback) {
        // Create a list of available ranks for display
        String rankList = ranks.stream()
                .map(r -> r.getDisplayName() + " (" + r.getId() + ")")
                .collect(Collectors.joining(", "));

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Grant Rank to " + targetPlayerName, NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text(
                                        "Select a rank and duration for this grant.",
                                        NamedTextColor.GRAY
                                )),
                                DialogBody.plainMessage(Component.text(
                                        "Available ranks: " + rankList,
                                        NamedTextColor.DARK_GRAY
                                ))
                        ))
                        .inputs(List.of(
                                // Rank ID (text input with available ranks shown above)
                                DialogInput.text("rankId", Component.text("Rank ID", NamedTextColor.AQUA))
                                        .width(200)
                                        .labelVisible(true)
                                        .maxLength(64)
                                        .build(),

                                // Duration (number input for days)
                                DialogInput.numberRange(
                                        "duration",
                                        Component.text("Duration (days)", NamedTextColor.YELLOW),
                                        0f,
                                        365f
                                ).step(1f)
                                        .initial(0f)
                                        .labelFormat("%s: %s days (0 = permanent)")
                                        .width(200)
                                        .build(),

                                // Reason
                                DialogInput.text("reason", Component.text("Reason", NamedTextColor.GREEN))
                                        .width(200)
                                        .labelVisible(true)
                                        .maxLength(256)
                                        .build()
                        ))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.create(
                                Component.text("Grant Rank", NamedTextColor.GREEN),
                                Component.text("Grant the selected rank to this player.", NamedTextColor.GRAY),
                                100,
                                DialogAction.customClick(
                                        callback,
                                        ClickCallback.Options.builder().uses(1).build()
                                )
                        ),
                        ActionButton.create(
                                Component.text("Cancel", NamedTextColor.RED),
                                Component.text("Close without granting.", NamedTextColor.GRAY),
                                100,
                                null
                        )
                ))
        );
    }

    /**
     * Shows the Grant Player dialog to a player
     *
     * @param player The player to show the dialog to
     * @param targetPlayerName The name of the player receiving the grant
     * @param ranks Collection of available ranks to choose from
     * @param callback The callback to handle the form submission
     */
    public static void show(Player player, String targetPlayerName, Collection<Rank> ranks, DialogActionCallback callback) {
        player.showDialog(create(targetPlayerName, ranks, callback));
    }
}
