package dev.municipal.budget;

/** Signals that a comparison query cannot produce a meaningful report. */
public final class InvalidComparisonException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidComparisonException(String message) {
        super(message);
    }
}
