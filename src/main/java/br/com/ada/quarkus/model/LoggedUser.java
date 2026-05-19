package br.com.ada.quarkus.model;

public record LoggedUser(
        Long id,
        String email,
        String cpf,
        String role
) {

    public boolean isManager() {
        return "GERENTE".equals(role);
    }

    public boolean isCustomer() {
        return "CLIENTE".equals(role);
    }
}