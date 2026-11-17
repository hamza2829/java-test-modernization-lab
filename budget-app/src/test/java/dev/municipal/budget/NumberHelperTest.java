package dev.municipal.budget;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NumberHelperTest {
    @Test
    void roundsRatiosToTwoDecimalPlaces() {
        assertThat(NumberHelper.divide(BigDecimal.ONE, BigDecimal.valueOf(8)))
                .isEqualTo(new BigDecimal("0.13"));
    }
}
