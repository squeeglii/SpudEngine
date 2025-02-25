package me.cg360.spudengine.core.exception;

import me.cg360.spudengine.core.util.Registry;

public class DuplicateException extends RuntimeException {

    public DuplicateException(Registry registry, String identifier) {
        super("Duplicate entry for '%s' already found in registry %s".formatted(identifier, registry.getRegistryIdentifier()));
    }

}
