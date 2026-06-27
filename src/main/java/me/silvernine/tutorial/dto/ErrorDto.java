package me.silvernine.tutorial.dto;

import java.util.List;

public record ErrorDto(int status, String message, List<FieldError> fieldErrors) {

    public ErrorDto(int status, String message) {
        this(status, message, List.of());
    }

    public record FieldError(String objectName, String field, String message) {
    }
}
