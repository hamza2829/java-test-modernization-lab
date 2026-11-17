package dev.municipal.budget.web;

import java.util.List;

/**
 * A machine-readable error returned by the budget API.
 *
 * @param code stable identifier for the failure
 * @param message human-readable explanation
 * @param fields any import fields that failed validation
 */
public record ApiError(String code, String message, List<FieldIssue> fields) {
    public ApiError {
        fields = List.copyOf(fields);
    }

    public ApiError(String code, String message) {
        this(code, message, List.of());
    }

    /**
     * Describes one invalid value in the input JSON.
     *
     * @param field path in the request body
     * @param message constraint that failed
     */
    public record FieldIssue(String field, String message) {
    }
}
