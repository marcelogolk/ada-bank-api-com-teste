package br.com.ada.quarkus.resource.customer;

public record CustomerResponse(
        Long id,
        String name,
        String email
    ){

}