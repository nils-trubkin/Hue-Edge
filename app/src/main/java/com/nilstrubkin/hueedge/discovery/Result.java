package com.nilstrubkin.hueedge.discovery;

public abstract class Result<T> {
    private Result() {}

    public static final class Success<T> extends Result<T> {
        public final T data;

        public Success(T data) {
            this.data = data;
        }
    }

    public static final class Error<T> extends Result<T> {
        public final Exception exception;

        public Error(Exception exception) {
            this.exception = exception;
        }
    }
}