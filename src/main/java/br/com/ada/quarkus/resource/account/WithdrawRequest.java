package br.com.ada.quarkus.resource.account;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
public record WithdrawRequest(

        @NotNull(message = "O valor do saque é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor do saque deve ser maior que zero")
        BigDecimal amount
) {
}