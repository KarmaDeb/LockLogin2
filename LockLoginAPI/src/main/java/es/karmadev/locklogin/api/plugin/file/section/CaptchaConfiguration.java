package es.karmadev.locklogin.api.plugin.file.section;

import java.io.Serializable;

/**
 * LockLogin captcha configuration
 */
public interface CaptchaConfiguration extends Serializable {

    /**
     * Get if the captchas are enabled
     *
     * @return if captchas should
     * be enabled
     */
    boolean enable();

    /**
     * Get the captcha length
     *
     * @return the captcha length
     */
    int length();

    /**
     * Get if the captcha includes
     * letters
     *
     * @return if the captcha contains
     * letters
     */
    boolean letters();

    /**
     * Get if the captcha should apply an strikethrough
     * effect
     *
     * @return if the captcha code has
     * an strikethrough effect
     */
    boolean strikethrough();

    /**
     * Get if the strikethrough effect
     * applies only to random characters,
     * making it difficult to read
     *
     * @return if the captcha should apply random
     * strikethrough
     */
    boolean randomStrike();
}
