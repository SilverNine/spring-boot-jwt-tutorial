package me.silvernine.tutorial.exception;

public class NotFoundMemberException extends RuntimeException {

    public NotFoundMemberException(String message) {
        super(message);
    }
}
