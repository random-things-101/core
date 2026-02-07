package club.catmc.core.bukkit.dialogs;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Hub dialog for rank management - shown when running /rank
 */
public class RankHubDialog {

    /**
     * Creates and returns the Rank Hub dialog
     *
     * @param onCreate Callback for create rank action
     * @param onList Callback for list ranks action
     * @return The configured Dialog
     */
    public static Dialog create(Runnable onCreate, Runnable onList) {
        return Dialog.create(builder -> builder.empty()
                .base(io.papermc.paper.registry.data.dialog.DialogBase.builder(
                                Component.text("Rank Management", NamedTextColor.GOLD)
                        )
                        .body(List.of(
                                DialogBody.plainMessage(Component.text(
                                        "Select an action to manage ranks.",
                                        NamedTextColor.GRAY
                                ))
                        ))
                        .build())
                .type(DialogType.multiAction(
                        List.of(
                                ActionButton.create(
                                        Component.text("Create Rank", NamedTextColor.GREEN),
                                        Component.text("Create a new rank", NamedTextColor.DARK_GRAY),
                                        100,
                                        DialogAction.customClick(
                                                (view, audience) -> onCreate.run(),
                                                ClickCallback.Options.builder().uses(1).build()
                                        )
                                ),
                                ActionButton.create(
                                        Component.text("List Ranks", NamedTextColor.AQUA),
                                        Component.text("View and manage all ranks", NamedTextColor.DARK_GRAY),
                                        100,
                                        DialogAction.customClick(
                                                (view, audience) -> onList.run(),
                                                ClickCallback.Options.builder().uses(1).build()
                                        )
                                )
                        ),
                        ActionButton.create(
                                Component.text("Close", NamedTextColor.RED),
                                Component.text("Close this menu", NamedTextColor.DARK_GRAY),
                                100,
                                null
                        ),
                        1
                ))
        );
    }

    /**
     * Shows the Rank Hub dialog to a player
     *
     * @param player The player to show the dialog to
     * @param onCreate Callback for create rank action
     * @param onList Callback for list ranks action
     */
    public static void show(Player player, Runnable onCreate, Runnable onList) {
        player.showDialog(create(onCreate, onList));
    }
}
