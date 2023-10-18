package es.karmadev.locklogin.api.plugin.file.spawn;

/**
 * Represents a teleport cancellation
 * policy
 */
public enum CancelPolicy {
    /**
     * When the client moves
     */
    MOVEMENT,
    /**
     * When the client enters
     * a PVP fight
     */
    PVP,
    /**
     * When the client enters
     * a PvE fight
     */
    PVE
}
