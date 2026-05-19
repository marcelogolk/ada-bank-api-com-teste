package br.com.ada.quarkus.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TransactionType {

    DEPOSITO("DEPOSITO"),

    SAQUE("SAQUE"),

    TRANSFERENCIA("TRANSFERENCIA");

    private final String value;

    TransactionType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDescription() {
        return switch (this) {
            case DEPOSITO -> "Depósito em conta";
            case SAQUE -> "Saque de numerário";
            case TRANSFERENCIA -> "Transferência entre contas";
        };
    }
}