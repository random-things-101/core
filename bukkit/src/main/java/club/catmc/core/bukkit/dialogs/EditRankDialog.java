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

import java.util.List;

/**
 * Dialog for editing an existing rank
 */
public class EditRankDialog {

    /**
     * Creates and returns an Edit Rank dialog
     *
     * @param rank The rank to edit
     * @param callback The callback to handle the form submission
     * @return The configured Dialog
     */
    public static Dialog create(Rank rank, DialogActionCallback callback) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Edit Rank: " + rank.getName(), NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text(
                                        "Update the rank details below.",
                                        NamedTextColor.GRAY
                                ))
                        ))
                        .inputs(List.of(
                                // Rank ID (read-only - show but don't allow editing)
                                DialogInput.text("id", Component.text("Rank ID (cannot be changed)", NamedTextColor.DARK_GRAY))
                                        .width(200)
                                        .labelVisible(true)
                                        .maxLength(64)
                                        .initial(rank.getId())
                                        .build(),

                                // Rank Name
                                DialogInput.text("name", Component.text("Name", NamedTextColor.AQUA))
                                        .width(200)
                                        .labelVisible(true)
                                        .maxLength(100)
                                        .initial(rank.getName())
                                        .build(),

                                // Display Name
                                DialogInput.text("displayName", Component.text("Display Name", NamedTextColor.AQUA))
                                        .width(200)
                                        .labelVisible(true)
                                        .maxLength(100)
                                        .initial(rank.getDisplayName())
                                        .build(),

                                // Prefix
                                DialogInput.text("prefix", Component.text("Prefix", NamedTextColor.YELLOW))
                                        .width(200)
                                        .labelVisible(true)
                                        .maxLength(100)
                                        .initial(rank.getPrefix() != null ? rank.getPrefix() : "")
                                        .build(),

                                // Suffix
                                DialogInput.text("suffix", Component.text("Suffix", NamedTextColor.YELLOW))
                                        .width(200)
                                        .labelVisible(true)
                                        .maxLength(100)
                                        .initial(rank.getSuffix() != null ? rank.getSuffix() : "")
                                        .build(),

                                // Priority
                                DialogInput.numberRange("priority", Component.text("Priority", NamedTextColor.GREEN),
                                        0f, 1000f)
                                        .width(200)
                                        .labelFormat("Priority: %s")
                                        .initial((float) rank.getPriority())
                                        .step(1f)
                                        .build(),

                                // Is Default Rank
                                DialogInput.bool("defaultRank", Component.text("Default Rank", NamedTextColor.LIGHT_PURPLE))
                                        .initial(rank.isDefaultRank())
                                        .build()
                        ))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.create(
                                Component.text("Save Changes", NamedTextColor.GREEN),
                                Component.text("Update the rank with the specified values.", NamedTextColor.GRAY),
                                100,
                                DialogAction.customClick(
                                        callback,
                                        ClickCallback.Options.builder().uses(1).build()
                                )
                        ),
                        ActionButton.create(
                                Component.text("Cancel", NamedTextColor.RED),
                                Component.text("Close without saving changes.", NamedTextColor.GRAY),
                                100,
                                null
                        )
                ))
        );
    }

    /**
     * Shows the Edit Rank dialog to a player
     *
     * @param player The player to show the dialog to
     * @param rank The rank to edit
     * @param callback The callback to handle the form submission
     */
    public static void show(Player player, Rank rank, DialogActionCallback callback) {
        player.showDialog(create(rank, callback));
    }
}
