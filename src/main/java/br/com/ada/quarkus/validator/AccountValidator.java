package br.com.ada.quarkus.validator;

import br.com.ada.quarkus.model.Account;
import br.com.ada.quarkus.model.AccountType;
import br.com.ada.quarkus.model.LoggedUser;
import br.com.ada.quarkus.service.CurrentUserService;
import io.quarkus.security.ForbiddenException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import java.math.BigDecimal;

@ApplicationScoped
public class AccountValidator {

@Inject
CurrentUserService currentUserService;



public void validateAmount(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("O valor da operação deve ser maior que zero");
    }
}

    public void validateDifferentAccounts(Long sourceAccountId, Long destinationAccountId) {
        if (sourceAccountId.equals(destinationAccountId)) {
            throw new BadRequestException("A conta de origem deve ser diferente da conta de destino");
        }
    }

    public void validateElectronicAccountForDeposit(Account account) {
        if (account.getType() == AccountType.ELETRONICA) {
            throw new BadRequestException("Conta do tipo ELETRONICA não permite depósito");
        }
    }

    public void validateElectronicAccountForWithdraw(Account account) {
        if (account.getType() == AccountType.ELETRONICA) {
            throw new BadRequestException("Conta do tipo ELETRONICA não permite saque");
        }
    }

    public void validateSufficientBalance(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Saldo insuficiente para realizar a operação");
        }
    }

    public void checkAccountOwnership(Account account, LoggedUser currentUser) {
        if (currentUser.isManager()) {
            return;
        }
        if (currentUser.id().equals(account.getCustomerId())) {
            return;
        }
        throw new ForbiddenException(
                "Acesso negado: apenas o proprietário da conta ou um gerente pode realizar esta operação"
        );
    }

    public int calculateCheckDigit(String baseNumber) {
        int sum = 0;
        for (char digit : baseNumber.toCharArray()) {
            sum += digit - '0';
        }
        return 9 - (sum % 10);
    }
}