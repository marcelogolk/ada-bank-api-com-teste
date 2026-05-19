package br.com.ada.quarkus.resource.account;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
public record TransferRequest(

        @NotNull(message = "O ID da conta de destino é obrigatório")
        Long destinationAccountId,

        @NotNull(message = "O valor da transferência é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor da transferência deve ser maior que zero")
        BigDecimal amount
) {
}