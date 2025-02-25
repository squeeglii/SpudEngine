package me.cg360.spudengine.core.exception;

public class EngineLimitExceededException extends RuntimeException {

    public EngineLimitExceededException(String limitName, int limit, int currentVal) {
        super("Engine limit exceeded: %s is limited to %s, but reached %s".formatted(limitName, limit, currentVal));
    }

}
