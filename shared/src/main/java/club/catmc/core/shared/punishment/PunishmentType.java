package club.catmc.core.shared.punishment;

/**
 * Types of punishments that can be issued to players
 */
public enum PunishmentType {
    /**
     * Permanent ban - player cannot join the server
     */
    BAN,

    /**
     * Temporary ban - player cannot join for a specified duration
     */
    TEMPBAN,

    /**
     * Permanent mute - player cannot send chat messages
     */
    MUTE,

    /**
     * Temporary mute - player cannot send chat messages for a specified duration
     */
    TEMP_MUTE,

    /**
     * Kick - player is kicked from the server (one-time, no duration)
     */
    KICK,

    /**
     * Warning - recorded warning for player (no action taken)
     */
    WARN
}
