CREATE TABLE IF NOT EXISTS city (
    code varchar(24) PRIMARY KEY,
    name varchar(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS annual_budget (
    city_code varchar(24) NOT NULL REFERENCES city(code),
    budget_year integer NOT NULL,
    population integer NOT NULL,
    PRIMARY KEY (city_code, budget_year)
);

CREATE TABLE IF NOT EXISTS budget_item (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    city_code varchar(24) NOT NULL,
    budget_year integer NOT NULL,
    category varchar(40) NOT NULL,
    amount numeric(18, 2) NOT NULL,
    FOREIGN KEY (city_code, budget_year) REFERENCES annual_budget(city_code, budget_year)
);

CREATE INDEX IF NOT EXISTS budget_item_city_year ON budget_item(city_code, budget_year);
