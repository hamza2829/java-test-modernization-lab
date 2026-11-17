package dev.municipal.budget;

/** Signals that a requested city does not exist in the budget catalog. */
public final class CityNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CityNotFoundException(String cityCode) {
        super("City " + cityCode + " was not found.");
    }
}
