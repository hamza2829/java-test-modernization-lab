package dev.municipal.budget;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import dev.municipal.budget.model.BudgetImport;
import dev.municipal.budget.model.BudgetItem;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetImportTest {
    @Test
    void takesAnIndependentReadOnlyCopyOfTheItems() {
        var education = new BudgetItem("EDUCATION", BigDecimal.TEN);
        var sourceItems = new ArrayList<>(List.of(education));
        var budget = new BudgetImport("alder", "Alder Harbor", 1_000, 2024, sourceItems);

        sourceItems.clear();

        assertThat(budget.items()).containsExactly(education);
        assertThatThrownBy(() -> budget.items().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
