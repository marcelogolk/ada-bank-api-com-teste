package br.com.ada.quarkus.resource;

public record Link(
        String rel,
        String href,
        String method
) {
}