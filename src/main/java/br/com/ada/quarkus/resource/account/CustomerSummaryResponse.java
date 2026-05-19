package br.com.ada.quarkus.resource.account;

public record CustomerSummaryResponse(
        Long id,
        String name,
        String email
) {
}