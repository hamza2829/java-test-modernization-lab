package dev.municipal.budget;

import java.math.RoundingMode;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * Configures the default year and displayed precision of budget reports.
 *
 * @param defaultYear year used when a report request omits it
 * @param decimalPlaces decimal places displayed for ratios
 * @param roundingMode rule used to round both figures
 */
@ConstructorBinding
@ConfigurationProperties("budget.reporting")
public record ReportingProperties(int defaultYear, int decimalPlaces, RoundingMode roundingMode) {
    private static final int MAX_DECIMAL_PLACES = 6;

    public ReportingProperties {
        if (decimalPlaces < 0 || decimalPlaces > MAX_DECIMAL_PLACES) {
            throw new IllegalArgumentException("Reporting decimal places must be between 0 and " + MAX_DECIMAL_PLACES);
        }
        Objects.requireNonNull(roundingMode, "Reporting rounding mode is required");
    }
}
