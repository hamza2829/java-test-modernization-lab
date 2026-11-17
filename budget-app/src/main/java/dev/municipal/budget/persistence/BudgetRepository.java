package dev.municipal.budget.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.municipal.budget.model.BudgetImport;
import dev.municipal.budget.model.BudgetItem;
import dev.municipal.budget.model.BudgetSnapshot;
import dev.municipal.budget.model.CityOption;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

/** Stores budget snapshots in PostgreSQL and maps query results to typed records. */
@Repository
public class BudgetRepository {
    private final JdbcOperations jdbc;

    public BudgetRepository(JdbcOperations jdbc) {
        this.jdbc = jdbc;
    }

    /** Replaces a snapshot inside the service's import transaction. */
    public void replace(BudgetImport budget) {
        saveCity(budget.cityCode(), budget.cityName());
        saveAnnualBudget(budget.cityCode(), budget.year(), budget.population());
        replaceItems(budget.cityCode(), budget.year(), budget.items());
    }

    public List<CityOption> cities() {
        return jdbc.query("SELECT code, name FROM city ORDER BY name, code",
                (row, index) -> new CityOption(row.getString("code"), row.getString("name")));
    }

    /** Returns an empty result for an unknown city and a zero snapshot for a missing year. */
    public Optional<BudgetSnapshot> findSnapshot(String cityCode, int year) {
        return jdbc.query("""
                SELECT c.name, b.population, i.category, i.amount
                FROM city c
                LEFT JOIN annual_budget b ON c.code = b.city_code AND b.budget_year = ?
                LEFT JOIN budget_item i ON b.city_code = i.city_code AND b.budget_year = i.budget_year
                WHERE c.code = ?
                ORDER BY i.id
                """, rows -> {
            return readSnapshot(cityCode, year, rows);
        }, year, cityCode);
    }

    private Optional<BudgetSnapshot> readSnapshot(String cityCode, int year, ResultSet rows) throws SQLException {
        if (!rows.next()) {
            return Optional.empty();
        }

        var city = new CityOption(cityCode, rows.getString("name"));
        int population = rows.getInt("population"); // SQL NULL in the left join maps to zero.
        List<BudgetItem> items = new ArrayList<>();
        do {
            BigDecimal amount = rows.getBigDecimal("amount");
            if (amount != null) {
                items.add(new BudgetItem(rows.getString("category"), amount));
            }
        } while (rows.next());
        return Optional.of(new BudgetSnapshot(city, year, population, items));
    }

    private void saveCity(String cityCode, String cityName) {
        jdbc.update("""
                INSERT INTO city (code, name) VALUES (?, ?)
                ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name
                """, cityCode, cityName);
    }

    private void saveAnnualBudget(String cityCode, int year, int population) {
        jdbc.update("""
                INSERT INTO annual_budget (city_code, budget_year, population) VALUES (?, ?, ?)
                ON CONFLICT (city_code, budget_year) DO UPDATE SET population = EXCLUDED.population
                """, cityCode, year, population);
    }

    private void replaceItems(String cityCode, int year, List<BudgetItem> items) {
        jdbc.update("DELETE FROM budget_item WHERE city_code = ? AND budget_year = ?", cityCode, year);
        for (BudgetItem item : items) {
            jdbc.update("INSERT INTO budget_item (city_code, budget_year, category, amount) VALUES (?, ?, ?, ?)",
                    cityCode, year, item.category(), item.amount());
        }
    }
}
