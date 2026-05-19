package br.com.ada.quarkus.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AccountType {

    CORRENTE("CORRENTE"),

    POUPANCA("POUPANCA"),

    ELETRONICA("ELETRONICA");

    private final String value;

    AccountType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDescription() {
        return switch (this) {
            case CORRENTE -> "Conta Corrente";
            case POUPANCA -> "Conta Poupança";
            case ELETRONICA -> "Conta Eletrônica";
        };
    }
}