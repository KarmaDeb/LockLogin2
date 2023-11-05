package es.karmadev.locklogin.api.network;

public interface TextContainer extends NetComponent {

    /**
     * Send a message to the client
     *
     * @param message the message to send
     */
    void sendMessage(final String message);

    /**
     * Send an actionbar to the client
     *
     * @param actionbar the actionbar to send
     */
    void sendActionBar(final String actionbar);

    /**
     * Send a title to the client
     *
     * @param title the title
     * @param subtitle the subtitle
     * @param fadeIn the title fade in time
     * @param showTime the title show time
     * @param fadeOut the title fade out time
     */
    void sendTitle(final String title, final String subtitle, final int fadeIn, final int showTime, final int fadeOut);

    /**
     * Send a title to the client
     *
     * @param title the title to send
     * @param subtitle the subtitle
     */
    default void sendTitle(final String title, final String subtitle) {
        sendTitle(title, subtitle, 2, 5, 2);
    }
}
