package me.cg360.spudengine.core.render.geometry;

public class ModelValidationException extends RuntimeException {

    public ModelValidationException(String message) {
        super(message);
    }

    public ModelValidationException(Throwable throwable, String message) {
        super(message, throwable);
    }

}
