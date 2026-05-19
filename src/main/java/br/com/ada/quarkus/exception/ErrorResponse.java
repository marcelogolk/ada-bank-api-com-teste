package br.com.ada.quarkus.exception;

public record ErrorResponse(
        int status,
        String error,
        String message,
        String path
) {
}