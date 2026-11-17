package dev.municipal.budget;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import dev.municipal.budget.model.BudgetItem;
import dev.municipal.budget.model.BudgetSnapshot;
import dev.municipal.budget.model.CityOption;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FigureCalculatorTest {
    private final CityOption city = new CityOption("alder", "Alder Harbor");
    private final FigureCalculator calculator = new FigureCalculator(
            new ReportingProperties(2024, 2, RoundingMode.HALF_UP));

    @Test
    void roundsBothFiguresHalfUp() {
        var budget = new BudgetSnapshot(city, 2024, 8, List.of(
                new BudgetItem("EDUCATION", new BigDecimal("1.00")),
                new BudgetItem("PARKS", new BigDecimal("2.00"))));

        var figures = calculator.calculate(budget);

        assertThat(figures.spendingPerResident()).isEqualTo(new BigDecimal("0.38"));
        assertThat(figures.educationShare()).isEqualTo(new BigDecimal("33.33"));
    }

    @Test
    void reportsScaledZeroWhenBothDenominatorsAreZero() {
        var figures = calculator.calculate(new BudgetSnapshot(city, 2023, 0, List.of()));

        assertThat(figures.spendingPerResident()).isEqualTo(new BigDecimal("0.00"));
        assertThat(figures.educationShare()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void usesConfiguredPrecisionAndRounding() {
        var calculator = new FigureCalculator(new ReportingProperties(2024, 1, RoundingMode.DOWN));
        var budget = new BudgetSnapshot(city, 2024, 6, List.of(new BudgetItem("EDUCATION", BigDecimal.ONE)));

        var figures = calculator.calculate(budget);

        assertThat(figures.spendingPerResident()).isEqualTo(new BigDecimal("0.1"));
        assertThat(figures.educationShare()).isEqualTo(new BigDecimal("100.0"));
    }
}
