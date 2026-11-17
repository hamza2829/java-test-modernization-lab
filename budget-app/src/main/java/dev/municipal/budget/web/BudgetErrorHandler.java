package dev.municipal.budget.web;

import java.util.Comparator;
import java.util.List;

import dev.municipal.budget.CityNotFoundException;
import dev.municipal.budget.InvalidComparisonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain, request binding, and PostgreSQL failures to consistent API errors. */
@RestControllerAdvice(assignableTypes = BudgetApi.class)
public class BudgetErrorHandler {
    private static final Logger LOG = LoggerFactory.getLogger(BudgetErrorHandler.class);
    private static final String INVALID_QUERY = "INVALID_QUERY";

    @ExceptionHandler(CityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError cityNotFound(CityNotFoundException exception) {
        return new ApiError("CITY_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError unreadableBody() {
        return new ApiError("INVALID_BODY", "Send a valid budget import JSON object.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError invalidImport(MethodArgumentNotValidException exception) {
        List<ApiError.FieldIssue> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldIssue(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(ApiError.FieldIssue::field).thenComparing(ApiError.FieldIssue::message))
                .toList();
        return new ApiError("INVALID_BUDGET", "Budget import has invalid fields.", fields);
    }

    @ExceptionHandler({ServletRequestBindingException.class, TypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError invalidParameters() {
        return new ApiError(INVALID_QUERY, "Provide the required query parameters in the expected format.");
    }

    @ExceptionHandler(InvalidComparisonException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError invalidComparison(InvalidComparisonException exception) {
        return new ApiError(INVALID_QUERY, exception.getMessage());
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError storageFailure(DataAccessException exception) {
        LOG.error("PostgreSQL budget operation failed", exception);
        return new ApiError("STORAGE_FAILURE", "The budget could not be read or saved.");
    }
}
