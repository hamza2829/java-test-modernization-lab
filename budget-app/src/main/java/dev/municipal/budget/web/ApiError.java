package dev.municipal.budget.web;

/**
 * A machine-readable error returned by the budget API.
 *
 * @param code stable identifier for the failure
 * @param message human-readable explanation
 */
public record ApiError(String code, String message) {
}
