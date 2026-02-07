package club.catmc.core.bukkit.dialogs;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dialog for listing ranks with action buttons
 */
public class RankListDialog {

    /**
     * Creates and returns a Rank List dialog
     *
     * @param ranks Collection of ranks to display
     * @param onEdit Callback for when a rank is selected for editing
     * @param onBack Callback for when back button is pressed
     * @return The configured Dialog
     */
    public static Dialog create(Collection<Rank> ranks, Consumer<Rank> onEdit, Runnable onBack) {
        // Create action buttons for each rank
        List<ActionButton> rankButtons = new ArrayList<>();
        for (Rank rank : ranks) {
            rankButtons.add(ActionButton.create(
                    Component.text("• " + rank.getDisplayName(), NamedTextColor.WHITE),
                    Component.text("ID: " + rank.getId() + " | Priority: " + rank.getPriority(), NamedTextColor.DARK_GRAY),
                    200,
                    DialogAction.customClick(
                            (view, audience) -> onEdit.accept(rank),
                            ClickCallback.Options.builder().uses(1).build()
                    )
            ));
        }

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Ranks (" + ranks.size() + ")", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text(
                                        "Select a rank to edit or manage.",
                                        NamedTextColor.GRAY
                                ))
                        ))
                        .build())
                .type(DialogType.multiAction(
                        rankButtons,
                        ActionButton.create(
                                Component.text("Back", NamedTextColor.RED),
                                Component.text("Return to rank menu", NamedTextColor.DARK_GRAY),
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
     * Shows the Rank List dialog to a player
     *
     * @param player The player to show the dialog to
     * @param ranks Collection of ranks to display
     * @param onEdit Callback for when a rank is selected for editing
     * @param onBack Callback for when back button is pressed
     */
    public static void show(Player player, Collection<Rank> ranks, Consumer<Rank> onEdit, Runnable onBack) {
        player.showDialog(create(ranks, onEdit, onBack));
    }
}
