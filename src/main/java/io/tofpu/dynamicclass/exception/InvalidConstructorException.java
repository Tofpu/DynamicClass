package io.tofpu.dynamicclass.exception;

public final class InvalidConstructorException extends Exception {
    public InvalidConstructorException(final Class<?> target) {
        super(target.getSimpleName() + " does not have a suitable constructor.");
    }
}
