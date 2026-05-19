package br.com.ada.quarkus.resource.account;

import br.com.ada.quarkus.model.AccountType;
import jakarta.validation.constraints.NotNull;
public record CreateAccountRequest(

        @NotNull(message = "O tipo da conta é obrigatório")
        AccountType type,

        @NotNull(message = "O ID do cliente é obrigatório")
        Long customerId
) {
}