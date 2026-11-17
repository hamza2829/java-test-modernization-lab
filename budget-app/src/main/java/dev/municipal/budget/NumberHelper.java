package dev.municipal.budget;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Numeric conversions shared by the legacy reporting code. */
public final class NumberHelper {
    private NumberHelper() {
    }

    public static BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.signum() == 0) {
            return new BigDecimal("0.00");
        }
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
