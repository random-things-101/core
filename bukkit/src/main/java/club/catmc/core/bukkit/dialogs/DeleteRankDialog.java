package club.catmc.core.bukkit.dialogs;

import club.catmc.core.shared.rank.Rank;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Dialog for confirming rank deletion
 */
public class DeleteRankDialog {

    /**
     * Creates and returns a new Delete Rank confirmation dialog
     *
     * @param rank The rank to be deleted
     * @param callback The callback to handle the confirmation
     * @return The configured Dialog
     */
    public static Dialog create(Rank rank, DialogActionCallback callback) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Delete Rank", NamedTextColor.RED))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text(
                                        "Are you sure you want to delete the rank '" + rank.getDisplayName() + "'?",
                                        NamedTextColor.YELLOW
                                )),
                                DialogBody.plainMessage(Component.text(
                                        "ID: " + rank.getId(),
                                        NamedTextColor.GRAY
                                )),
                                DialogBody.plainMessage(Component.text(
                                        "This action cannot be undone.",
                                        NamedTextColor.RED
                                ))
                        ))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.create(
                                Component.text("Delete", NamedTextColor.RED),
                                Component.text("Permanently delete this rank.", NamedTextColor.GRAY),
                                100,
                                DialogAction.customClick(
                                        callback,
                                        ClickCallback.Options.builder().uses(1).build()
                                )
                        ),
                        ActionButton.create(
                                Component.text("Cancel", NamedTextColor.GREEN),
                                Component.text("Keep this rank.", NamedTextColor.GRAY),
                                100,
                                null
                        )
                ))
        );
    }

    /**
     * Shows the Delete Rank confirmation dialog to a player
     *
     * @param player The player to show the dialog to
     * @param rank The rank to be deleted
     * @param callback The callback to handle the confirmation
     */
    public static void show(Player player, Rank rank, DialogActionCallback callback) {
        player.showDialog(create(rank, callback));
    }
}
