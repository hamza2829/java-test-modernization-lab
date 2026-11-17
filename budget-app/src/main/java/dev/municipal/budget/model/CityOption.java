package dev.municipal.budget.model;

/**
 * An entry in the city catalog.
 *
 * @param code stable city identifier
 * @param name display name
 */
public record CityOption(String code, String name) {
}
