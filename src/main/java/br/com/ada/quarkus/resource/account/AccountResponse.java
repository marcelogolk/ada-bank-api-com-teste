package br.com.ada.quarkus.resource.account;

import br.com.ada.quarkus.model.Account;
import br.com.ada.quarkus.model.AccountType;
import br.com.ada.quarkus.util.OutputMaskFormatter;

import java.math.BigDecimal;

public record AccountResponse(
        Long id,
        String accountNumber,
        AccountType type,
        BigDecimal balance
) {

    public static AccountResponse fromEntity(Account account) {
        return new AccountResponse(
                account.getId(),
                OutputMaskFormatter.formatAccountNumber(account.getAccountNumber()),
                account.getType(),
                account.getBalance()
        );
    }
}