package club.catmc.core.bukkit.dialogs;

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
 * Dialog for creating a new rank
 */
public class CreateRankDialog {

    /**
     * Creates and returns a new Create Rank dialog
     *
     * @param callback The callback to handle the form submission
     * @return The configured Dialog
     */
    public static Dialog create(DialogActionCallback callback) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Create New Rank", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text(
                                        "Fill in the details below to create a new rank.",
                                        NamedTextColor.GRAY
                                ))
                        ))
                        .inputs(List.of(
                                // Rank ID (unique identifier)
                                DialogInput.text("id", Component.text("Rank ID", NamedTextColor.AQUA))
                                        .width(200)
                                        .labelVisible(true)
                                        .maxLength(64)
                                        .build(),

                                // Rank Name
                                DialogInput.text("name", Component.text("Name", NamedTextColor.AQUA))
                                        .width(200)
                                        .labelVisible(true)
                                        .maxLength(100)
                                        .build(),

                                // Display Name
                                DialogInput.text("displayName", Component.text("Display Name", NamedTextColor.AQUA))
                                        .width(200)
                                        .labelVisible(true)
                                        .maxLength(100)
                                        .build(),

                                // Prefix
                                DialogInput.text("prefix", Component.text("Prefix", NamedTextColor.YELLOW))
                                        .width(200)
                                        .labelVisible(true)
                                        .maxLength(100)
                                        .build(),

                                // Suffix
                                DialogInput.text("suffix", Component.text("Suffix", NamedTextColor.YELLOW))
                                        .width(200)
                                        .labelVisible(true)
                                        .maxLength(100)
                                        .build(),

                                // Priority
                                DialogInput.numberRange("priority", Component.text("Priority", NamedTextColor.GREEN), 0f, 1000f)
                                        .width(200)
                                        .labelFormat("Priority: %s")
                                        .initial(0f)
                                        .step(1f)
                                        .build(),

                                // Is Default Rank
                                DialogInput.bool("defaultRank", Component.text("Default Rank", NamedTextColor.LIGHT_PURPLE))
                                        .initial(false)
                                        .build()
                        ))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.create(
                                Component.text("Create Rank", NamedTextColor.GREEN),
                                Component.text("Click to create the rank with the specified values.", NamedTextColor.GRAY),
                                100,
                                DialogAction.customClick(
                                        callback,
                                        ClickCallback.Options.builder().uses(1).build()
                                )
                        ),
                        ActionButton.create(
                                Component.text("Cancel", NamedTextColor.RED),
                                Component.text("Close this dialog without creating a rank.", NamedTextColor.GRAY),
                                100,
                                null
                        )
                ))
        );
    }

    /**
     * Shows the Create Rank dialog to a player
     *
     * @param player The player to show the dialog to
     * @param callback The callback to handle the form submission
     */
    public static void show(Player player, DialogActionCallback callback) {
        player.showDialog(create(callback));
    }
}
