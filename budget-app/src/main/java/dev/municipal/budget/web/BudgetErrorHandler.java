package dev.municipal.budget.web;

import dev.municipal.budget.CityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain, request binding, and PostgreSQL failures to consistent API errors. */
@RestControllerAdvice(assignableTypes = BudgetApi.class)
public class BudgetErrorHandler {
    private static final Logger LOG = LoggerFactory.getLogger(BudgetErrorHandler.class);

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

    @ExceptionHandler({ServletRequestBindingException.class, TypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError invalidParameters() {
        return new ApiError("INVALID_QUERY", "Provide the required query parameters in the expected format.");
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError storageFailure(DataAccessException exception) {
        LOG.error("PostgreSQL budget operation failed", exception);
        return new ApiError("STORAGE_FAILURE", "The budget could not be read or saved.");
    }
}
