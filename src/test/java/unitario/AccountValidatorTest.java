package unitario;

import br.com.ada.quarkus.model.Account;
import br.com.ada.quarkus.model.AccountType;
import br.com.ada.quarkus.model.LoggedUser;
import br.com.ada.quarkus.service.AccountValidator;
import io.quarkus.security.ForbiddenException;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class AccountValidatorTest {

    @InjectMocks
    AccountValidator accountValidator;

    // ===================== validateAmount =====================

    @Test
    void deveLancarExcecaoQuandoAmountNulo() {
        assertThrows(BadRequestException.class, () ->
                accountValidator.validateAmount(null)
        );
    }

    @Test
    void deveLancarExcecaoQuandoAmountZero() {
        assertThrows(BadRequestException.class, () ->
                accountValidator.validateAmount(BigDecimal.ZERO)
        );
    }

    @Test
    void deveLancarExcecaoQuandoAmountNegativo() {
        assertThrows(BadRequestException.class, () ->
                accountValidator.validateAmount(BigDecimal.valueOf(-1))
        );
    }

    @Test
    void naoDeveLancarExcecaoQuandoAmountValido() {
        assertDoesNotThrow(() ->
                accountValidator.validateAmount(BigDecimal.valueOf(100))
        );
    }

    // ===================== validateDifferentAccounts =====================

    @Test
    void deveLancarExcecaoQuandoContasIguais() {
        assertThrows(BadRequestException.class, () ->
                accountValidator.validateDifferentAccounts(1L, 1L)
        );
    }

    @Test
    void naoDeveLancarExcecaoQuandoContasDiferentes() {
        assertDoesNotThrow(() ->
                accountValidator.validateDifferentAccounts(1L, 2L)
        );
    }

    // ===================== validateElectronicAccountForDeposit =====================

    @Test
    void deveLancarExcecaoQuandoContaEletronicaRecebeDeposito() {
        Account account = new Account();
        account.setType(AccountType.ELETRONICA);

        assertThrows(BadRequestException.class, () ->
                accountValidator.validateElectronicAccountForDeposit(account)
        );
    }

    @Test
    void naoDeveLancarExcecaoQuandoContaCorrenteRecebeDeposito() {
        Account account = new Account();
        account.setType(AccountType.CORRENTE);

        assertDoesNotThrow(() ->
                accountValidator.validateElectronicAccountForDeposit(account)
        );
    }

    // ===================== validateElectronicAccountForWithdraw =====================

    @Test
    void deveLancarExcecaoQuandoContaEletronicaRealizaSaque() {
        Account account = new Account();
        account.setType(AccountType.ELETRONICA);

        assertThrows(BadRequestException.class, () ->
                accountValidator.validateElectronicAccountForWithdraw(account)
        );
    }

    @Test
    void naoDeveLancarExcecaoQuandoContaCorrenteRealizaSaque() {
        Account account = new Account();
        account.setType(AccountType.CORRENTE);

        assertDoesNotThrow(() ->
                accountValidator.validateElectronicAccountForWithdraw(account)
        );
    }

    // ===================== validateSufficientBalance =====================
    private Account createAccountWithBalance(BigDecimal balance) throws Exception {
        Account account = new Account();
        var balanceField = Account.class.getDeclaredField("balance");
        balanceField.setAccessible(true);
        balanceField.set(account, balance);
        return account;
    }

    @Test
    void deveLancarExcecaoQuandoSaldoInsuficiente() throws Exception {
        Account account = createAccountWithBalance(BigDecimal.valueOf(50));

        assertThrows(BadRequestException.class, () ->
                accountValidator.validateSufficientBalance(account, BigDecimal.valueOf(100))
        );
    }

    @Test
    void naoDeveLancarExcecaoQuandoSaldoSuficiente() throws Exception {
        Account account = createAccountWithBalance(BigDecimal.valueOf(200));

        assertDoesNotThrow(() ->
                accountValidator.validateSufficientBalance(account, BigDecimal.valueOf(100))
        );
    }


    // ===================== checkAccountOwnership =====================
    @Test
    void naoDeveLancarExcecaoQuandoUsuarioEGerente() {
        Account account = new Account();
        account.setCustomerId(5L);

        LoggedUser gerente = new LoggedUser(1L, "gerente@banco.com", "11111111111", "GERENTE");

        assertDoesNotThrow(() ->
                accountValidator.checkAccountOwnership(account, gerente)
        );
    }

    @Test
    void naoDeveLancarExcecaoQuandoUsuarioEDonoDaConta() {
        Account account = new Account();
        account.setCustomerId(3L);

        LoggedUser dono = new LoggedUser(3L, "dono@banco.com", "33333333333", "CLIENTE");

        assertDoesNotThrow(() ->
                accountValidator.checkAccountOwnership(account, dono)
        );
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoEDonoDaConta() {
        Account account = new Account();
        account.setCustomerId(5L);

        LoggedUser outro = new LoggedUser(99L, "outro@banco.com", "99999999999", "CLIENTE");

        assertThrows(ForbiddenException.class, () ->
                accountValidator.checkAccountOwnership(account, outro)
        );
    }
    @Test
    void deveCalcularDigitoVerificadorParaBaseZerada() {
        // sum = 0 → 9 - (0 % 10) = 9
        int result = accountValidator.calculateCheckDigit("000000000");
        assertEquals(9, result);
    }

    @Test
    void deveCalcularDigitoVerificadorParaBaseUm() {
        // sum = 1 → 9 - (1 % 10) = 8
        int result = accountValidator.calculateCheckDigit("000000001");
        assertEquals(8, result);
    }

    @Test
    void deveCalcularDigitoVerificadorParaBaseNove() {
        // sum = 9 → 9 - (9 % 10) = 0
        int result = accountValidator.calculateCheckDigit("000000009");
        assertEquals(0, result);
    }

    @Test
    void deveCalcularDigitoVerificadorParaBaseCinco() {
        // sum = 5 → 9 - (5 % 10) = 4
        int result = accountValidator.calculateCheckDigit("000000005");
        assertEquals(4, result);
    }
}