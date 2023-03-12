package es.karmadev.locklogin.common.api.web.license.data;

import es.karmadev.locklogin.api.plugin.license.data.LicenseExpiration;
import lombok.AllArgsConstructor;

import java.time.*;

@AllArgsConstructor
public class CLicenseDate implements LicenseExpiration {

    private final long granted;
    private final long expired;

    /**
     * Get when the license was granted
     *
     * @return when the license was granted
     */
    @Override
    public Instant granted() {
        if (granted == -1) return Instant.now();
        return Instant.ofEpochMilli(granted);
    }

    /**
     * Get when the license will expire
     *
     * @return when the license will expire
     */
    @Override
    public Instant expiration() {
        if (expired == -1) return Instant.now();
        return Instant.ofEpochMilli(expired);
    }

    /**
     * Get if the license has expired
     *
     * @return if the license is expired
     */
    @Override
    public boolean isExpired() {
        if (granted == -1 || expired == -1) return false;
        Instant exp = Instant.ofEpochMilli(expired);

        return exp.isBefore(Instant.now());
    }

    /**
     * Get if the license has an expiration
     * date
     *
     * @return if the license has an expiration date
     */
    @Override
    public boolean hasExpiration() {
        return expired != -1;
    }

    /**
     * Get the amount of years before
     * the license expires
     *
     * @return the license years lifetime
     */
    @Override
    public int expireYears() {
        if (expired == -1) return 1;
        Instant exp = Instant.ofEpochMilli(expired);

        LocalDate now = LocalDate.now();
        LocalDate expire = exp.atZone(ZoneId.systemDefault()).toLocalDate();

        Period period = Period.between(now, expire);
        return period.getYears();
    }

    /**
     * Get the amount of months before
     * the license expires
     *
     * @return the license months lifetime
     */
    @Override
    public int expireMonths() {
        if (expired == -1) return 1;
        Instant exp = Instant.ofEpochMilli(expired);

        LocalDate now = LocalDate.now();
        LocalDate expire = exp.atZone(ZoneId.systemDefault()).toLocalDate();

        Period period = Period.between(now, expire);
        return period.getMonths();
    }

    /**
     * Get the amount of weeks before
     * the license expires
     *
     * @return the license weeks lifetime
     */
    @Override
    public int expireWeeks() {
        if (expired == -1) return 1;
        Instant exp = Instant.ofEpochMilli(expired);

        LocalDate now = LocalDate.now();
        LocalDate expire = exp.atZone(ZoneId.systemDefault()).toLocalDate();

        Period period = Period.between(now, expire);
        int days = period.getDays();
        return days / 7;
    }

    /**
     * Get the amount of days before
     * the license expires
     *
     * @return the license days lifetime
     */
    @Override
    public int expireDays() {
        if (expired == -1) return 1;
        Instant exp = Instant.ofEpochMilli(expired);

        LocalDate now = LocalDate.now();
        LocalDate expire = exp.atZone(ZoneId.systemDefault()).toLocalDate();

        Period period = Period.between(now, expire);
        return period.getDays();
    }

    /**
     * Get the amount of hours before
     * the license expires
     *
     * @return the license hours lifetime
     */
    @Override
    public int expireHours() {
        if (expired == -1) return 1;
        Instant exp = Instant.ofEpochMilli(expired);
        LocalDate expire = exp.atZone(ZoneId.systemDefault()).toLocalDate();

        Instant currentInstant = Instant.now();
        Instant destinationInstant = expire.atStartOfDay(ZoneId.systemDefault()).toInstant();

        Duration duration = Duration.between(currentInstant, destinationInstant);

        long totalSeconds = duration.getSeconds();
        int totalMinutes = (int) (totalSeconds / 60);
        int totalHours = totalMinutes / 60;
        return totalHours % 24;
    }

    /**
     * Get the amount of minutes before
     * the license expires
     *
     * @return the license minutes lifetime
     */
    @Override
    public int expireMinutes() {
        if (expired == -1) return 1;
        Instant exp = Instant.ofEpochMilli(expired);
        LocalDate expire = exp.atZone(ZoneId.systemDefault()).toLocalDate();

        Instant currentInstant = Instant.now();
        Instant destinationInstant = expire.atStartOfDay(ZoneId.systemDefault()).toInstant();

        Duration duration = Duration.between(currentInstant, destinationInstant);
        long totalSeconds = duration.getSeconds();
        int totalMinutes = (int) (totalSeconds / 60);
        return totalMinutes % 60;
    }

    /**
     * Get the amount of seconds before
     * the license expires
     *
     * @return the license seconds lifetime
     */
    @Override
    public int expireSeconds() {
        if (expired == -1) return 1;
        Instant exp = Instant.ofEpochMilli(expired);
        LocalDate expire = exp.atZone(ZoneId.systemDefault()).toLocalDate();

        Instant currentInstant = Instant.now();
        Instant destinationInstant = expire.atStartOfDay(ZoneId.systemDefault()).toInstant();

        Duration duration = Duration.between(currentInstant, destinationInstant);
        long totalSeconds = duration.getSeconds();
        return (int) (totalSeconds % 60);
    }
}
