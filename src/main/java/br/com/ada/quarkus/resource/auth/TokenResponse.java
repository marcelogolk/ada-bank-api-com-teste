package br.com.ada.quarkus.resource.auth;
public record TokenResponse(
        String token,
        String email,
        String name
) {
}