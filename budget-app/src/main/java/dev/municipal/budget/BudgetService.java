package dev.municipal.budget;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.municipal.budget.model.BudgetImport;
import dev.municipal.budget.model.BudgetItem;
import dev.municipal.budget.model.CityOption;
import dev.municipal.budget.model.ComparisonRow;
import dev.municipal.budget.model.KeyFigures;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Imports budgets and calculates the figures shown to residents. */
@Service
public class BudgetService {
    @Autowired
    private JdbcTemplate jdbc;

    @Transactional
    public void importBudget(BudgetImport budget) {
        jdbc.update("""
                INSERT INTO city (code, name) VALUES (?, ?)
                ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name
                """, budget.cityCode(), budget.cityName());
        jdbc.update("""
                INSERT INTO annual_budget (city_code, budget_year, population) VALUES (?, ?, ?)
                ON CONFLICT (city_code, budget_year) DO UPDATE SET population = EXCLUDED.population
                """, budget.cityCode(), budget.year(), budget.population());
        jdbc.update("DELETE FROM budget_item WHERE city_code = ? AND budget_year = ?", budget.cityCode(), budget.year());
        for (BudgetItem item : budget.items()) {
            jdbc.update("INSERT INTO budget_item (city_code, budget_year, category, amount) VALUES (?, ?, ?, ?)",
                    budget.cityCode(), budget.year(), item.category(), item.amount());
        }
    }

    public List<CityOption> cities() {
        return jdbc.query("SELECT code, name FROM city ORDER BY name, code",
                (row, index) -> new CityOption(row.getString("code"), row.getString("name")));
    }

    public KeyFigures figures(String cityCode, int year) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT c.name, b.population, i.category, i.amount
                FROM city c
                LEFT JOIN annual_budget b ON c.code = b.city_code AND b.budget_year = ?
                LEFT JOIN budget_item i ON b.city_code = i.city_code AND b.budget_year = i.budget_year
                WHERE c.code = ?
                ORDER BY i.id
                """, year, cityCode);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Unknown city: " + cityCode);
        }
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal education = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            BigDecimal amount = (BigDecimal) row.get("amount");
            if (amount != null) {
                total = total.add(amount);
                if ("EDUCATION".equals(row.get("category"))) {
                    education = education.add(amount);
                }
            }
        }
        Map<String, Object> first = rows.get(0);
        int population = first.get("population") == null ? 0 : (Integer) first.get("population");
        BigDecimal spending = NumberHelper.divide(total, BigDecimal.valueOf(population));
        BigDecimal share = NumberHelper.divide(education.multiply(BigDecimal.valueOf(100)), total);
        return new KeyFigures(cityCode, (String) first.get("name"), year, spending, share);
    }

    public List<ComparisonRow> compare(List<String> cityCodes, int year, String figure) {
        List<ComparisonRow> result = new ArrayList<>();
        for (String cityCode : cityCodes) {
            List<Map<String, Object>> rows = jdbc.queryForList("""
                    SELECT c.name, b.population, i.category, i.amount
                    FROM city c
                    LEFT JOIN annual_budget b ON c.code = b.city_code AND b.budget_year = ?
                    LEFT JOIN budget_item i ON b.city_code = i.city_code AND b.budget_year = i.budget_year
                    WHERE c.code = ?
                    ORDER BY i.id
                    """, year, cityCode);
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("Unknown city: " + cityCode);
            }
            BigDecimal total = BigDecimal.ZERO;
            BigDecimal education = BigDecimal.ZERO;
            for (Map<String, Object> row : rows) {
                BigDecimal amount = (BigDecimal) row.get("amount");
                if (amount != null) {
                    total = total.add(amount);
                    if ("EDUCATION".equals(row.get("category"))) {
                        education = education.add(amount);
                    }
                }
            }
            Map<String, Object> first = rows.get(0);
            int population = first.get("population") == null ? 0 : (Integer) first.get("population");
            BigDecimal spending = NumberHelper.divide(total, BigDecimal.valueOf(population));
            BigDecimal share = NumberHelper.divide(education.multiply(BigDecimal.valueOf(100)), total);
            BigDecimal value = "spending-per-resident".equals(figure) ? spending : share;
            result.add(new ComparisonRow(cityCode, (String) first.get("name"), value));
        }
        return result;
    }
}
