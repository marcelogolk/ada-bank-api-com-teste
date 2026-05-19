package br.com.ada.quarkus.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum UserRole {

    MANAGER("GERENTE"),

    CUSTOMER("CLIENTE");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDescription() {
        return switch (this) {
            case MANAGER -> "Gerente do Banco";
            case CUSTOMER -> "Cliente do Banco";
        };
    }
}