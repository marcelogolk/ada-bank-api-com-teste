package br.com.ada.quarkus.resource.account;

import br.com.ada.quarkus.model.AccountType;
import br.com.ada.quarkus.resource.transaction.TransactionResponse;

import java.math.BigDecimal;
import java.util.List;
public record AccountDetailsResponse(
        Long id,
        String accountNumber,
        AccountType type,
        BigDecimal balance,
        CustomerSummaryResponse holder,
        List<TransactionResponse> transactions,
        AccountLinksResponse _links
) {
}