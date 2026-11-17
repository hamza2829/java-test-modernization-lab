INSERT INTO city (code, name)
VALUES ('alder-harbor', 'Alder Harbor'), ('birch-falls', 'Birch Falls')
ON CONFLICT (code) DO NOTHING;

WITH new_budgets AS (
    INSERT INTO annual_budget (city_code, budget_year, population)
    VALUES ('alder-harbor', 2024, 1000), ('birch-falls', 2024, 800)
    ON CONFLICT (city_code, budget_year) DO NOTHING
    RETURNING city_code, budget_year
)
INSERT INTO budget_item (city_code, budget_year, category, amount)
SELECT budgets.city_code, budgets.budget_year, items.category, items.amount
FROM new_budgets budgets
JOIN (VALUES ('alder-harbor', 'EDUCATION', 60000.00), ('alder-harbor', 'TRANSPORT', 40000.00),
             ('birch-falls', 'EDUCATION', 40000.00), ('birch-falls', 'PARKS', 60000.00))
    AS items (city_code, category, amount) ON items.city_code = budgets.city_code;
