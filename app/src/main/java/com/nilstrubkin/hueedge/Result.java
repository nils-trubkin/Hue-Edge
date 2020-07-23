package com.nilstrubkin.hueedge;

public abstract class Result<T> {
    private Result() {}

    public static final class Success<T> extends Result<T> {
        public T data;

        public Success(T data) {
            this.data = data;
        }
    }

    public static final class Error<T> extends Result<T> {
        public Exception exception;
        public int errorCode;

        public Error(Exception exception) {
            this.exception = exception;
        }

        public Error(int errorCode) {
            this.errorCode = errorCode;
        }
    }
}