package com.ppedregal.typescript.maven;

public class TscInvocationException extends Exception {

    private static final long serialVersionUID = 4663783740895564533L;

    public TscInvocationException() {
        super();
    }

    public TscInvocationException(String message, Throwable cause) {
        super(message, cause);
    }

    public TscInvocationException(String message) {
        super(message);
    }

    public TscInvocationException(Throwable cause) {
        super(cause);
    }

}
