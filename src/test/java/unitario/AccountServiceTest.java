package unitario;

import br.com.ada.quarkus.model.*;
import br.com.ada.quarkus.service.*;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class AccountServiceTest {

    @Inject
    AccountService accountService;

    @InjectMock
    CustomerService customerService;

    @InjectMock
    CurrentUserService currentUserService;

    @InjectMock
    TransactionService transactionService;

    @InjectMock
    AccountValidator accountValidator;

    private Account account;
    private Account destinationAccount;
    private LoggedUser loggedUser;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        // ── Ativa o mock do Panache ANTES de qualquer teste ──────────────
        PanacheMock.mock(Account.class);

        // ── Fixtures ─────────────────────────────────────────────────────
        account = new Account(1L, "0000000011", AccountType.CORRENTE, 10L);
        destinationAccount = new Account(2L, "0000000022", AccountType.CORRENTE, 20L);
        loggedUser = new LoggedUser(10L, "cliente@email.com", "123.456.789-00", "CLIENTE");
        transaction = new Transaction(
                99L,
                TransactionType.DEPOSITO,
                BigDecimal.valueOf(200),
                LocalDateTime.now(),
                null,
                1L
        );
    }

    @AfterEach
    void tearDown() {
        PanacheMock.reset();
    }

    // ════════════════════════════════════════════════════════════════════════
    // findById
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void findById_deveRetornarConta_quandoContaExiste() {
        // ARRANGE
        when(Account.<Account>findById(1L)).thenReturn(account);

        // ACT
        Account result = accountService.findById(1L);

        // ASSERT
        assertNotNull(result);
        assertEquals(1L, result.getId());
        PanacheMock.verify(Account.class, times(1)).findById(1L);
    }

    @Test
    void findById_deveLancarNotFoundException_quandoContaNaoExiste() {
        // ARRANGE
        PanacheMock.mock(Account.class);
        when(Account.findById(99L)).thenReturn(null);

        // ACT + ASSERT
        assertThrows(NotFoundException.class,
                () -> accountService.findById(99L));
    }

    // ════════════════════════════════════════════════════════════════════════
    // deposit
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void deposit_deveRetornarTransacao_quandoDadosValidos() {
        // ARRANGE
        PanacheMock.mock(Account.class);
        when(Account.findById(1L)).thenReturn(account);
        doNothing().when(accountValidator).validateAmount(any());
        doNothing().when(accountValidator).validateElectronicAccountForDeposit(any());
        when(transactionService.createDeposit(1L, BigDecimal.valueOf(200)))
                .thenReturn(transaction);

        // ACT
        Transaction result = accountService.deposit(1L, BigDecimal.valueOf(200));

        // ASSERT
        assertNotNull(result);
        verify(accountValidator, times(1)).validateAmount(BigDecimal.valueOf(200));
        verify(accountValidator, times(1)).validateElectronicAccountForDeposit(account);
        verify(transactionService, times(1)).createDeposit(1L, BigDecimal.valueOf(200));
    }

    @Test
    void deposit_deveLancarNotFoundException_quandoContaNaoExiste() {
        // ARRANGE
        PanacheMock.mock(Account.class);
        when(Account.findById(99L)).thenReturn(null);

        // ACT + ASSERT
        assertThrows(NotFoundException.class,
                () -> accountService.deposit(99L, BigDecimal.valueOf(200)));

        verify(transactionService, never()).createDeposit(any(), any());
    }

    @Test
    void deposit_deveLancarExcecao_quandoValorInvalido() {
        // ARRANGE
        PanacheMock.mock(Account.class);
        when(Account.findById(1L)).thenReturn(account);
        doThrow(new IllegalArgumentException("Valor inválido"))
                .when(accountValidator).validateAmount(BigDecimal.valueOf(-50));

        // ACT + ASSERT
        assertThrows(IllegalArgumentException.class,
                () -> accountService.deposit(1L, BigDecimal.valueOf(-50)));

        verify(transactionService, never()).createDeposit(any(), any());
    }

    @Test
    void deposit_deveLancarExcecao_quandoContaNaoPermiteDeposito() {
        // ARRANGE
        PanacheMock.mock(Account.class);
        when(Account.findById(1L)).thenReturn(account);
        doNothing().when(accountValidator).validateAmount(any());
        doThrow(new IllegalArgumentException("Conta não permite depósito"))
                .when(accountValidator).validateElectronicAccountForDeposit(account);

        // ACT + ASSERT
        assertThrows(IllegalArgumentException.class,
                () -> accountService.deposit(1L, BigDecimal.valueOf(200)));

        verify(transactionService, never()).createDeposit(any(), any());
    }

    // ════════════════════════════════════════════════════════════════════════
    // withdraw
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void withdraw_deveRetornarTransacao_quandoDadosValidos() {
        // ARRANGE
        PanacheMock.mock(Account.class);
        when(Account.findById(1L)).thenReturn(account);
        when(currentUserService.getLoggedUser()).thenReturn(loggedUser);
        doNothing().when(accountValidator).checkAccountOwnership(any(), any());
        doNothing().when(accountValidator).validateAmount(any());
        doNothing().when(accountValidator).validateElectronicAccountForWithdraw(any());
        doNothing().when(accountValidator).validateSufficientBalance(any(), any());
        when(transactionService.createWithdraw(1L, BigDecimal.valueOf(300)))
                .thenReturn(transaction);

        // ACT
        Transaction result = accountService.withdraw(1L, BigDecimal.valueOf(300));

        // ASSERT
        assertNotNull(result);
        verify(accountValidator, times(1)).checkAccountOwnership(account, loggedUser);
        verify(accountValidator, times(1)).validateAmount(BigDecimal.valueOf(300));
        verify(accountValidator, times(1)).validateSufficientBalance(account, BigDecimal.valueOf(300));
        verify(transactionService, times(1)).createWithdraw(1L, BigDecimal.valueOf(300));
    }

    @Test
    void withdraw_deveLancarNotFoundException_quandoContaNaoExiste() {
        // ARRANGE
        PanacheMock.mock(Account.class);
        when(Account.findById(99L)).thenReturn(null);

        // ACT + ASSERT
        assertThrows(NotFoundException.class,
                () -> accountService.withdraw(99L, BigDecimal.valueOf(300)));

        verify(transactionService, never()).createWithdraw(any(), any());
    }

    @Test
    void withdraw_deveLancarExcecao_quandoSaldoInsuficiente() {
        // ARRANGE
        PanacheMock.mock(Account.class);
        when(Account.findById(1L)).thenReturn(account);
        when(currentUserService.getLoggedUser()).thenReturn(loggedUser);
        doNothing().when(accountValidator).checkAccountOwnership(any(), any());
        doNothing().when(accountValidator).validateAmount(any());
        doNothing().when(accountValidator).validateElectronicAccountForWithdraw(any());
        doThrow(new IllegalArgumentException("Saldo insuficiente"))
                .when(accountValidator).validateSufficientBalance(account, BigDecimal.valueOf(9999));

        // ACT + ASSERT
        assertThrows(IllegalArgumentException.class,
                () -> accountService.withdraw(1L, BigDecimal.valueOf(9999)));

        verify(transactionService, never()).createWithdraw(any(), any());
    }

    @Test
    void withdraw_deveLancarExcecao_quandoContaNaoPertenceAoUsuario() {
        // ARRANGE
        PanacheMock.mock(Account.class);
        when(Account.findById(1L)).thenReturn(account);
        when(currentUserService.getLoggedUser()).thenReturn(loggedUser);
        doThrow(new jakarta.ws.rs.ForbiddenException("Acesso negado"))
                .when(accountValidator).checkAccountOwnership(account, loggedUser);

        // ACT + ASSERT
        assertThrows(jakarta.ws.rs.ForbiddenException.class,
                () -> accountService.withdraw(1L, BigDecimal.valueOf(100)));

        verify(transactionService, never()).createWithdraw(any(), any());
    }

    // ════════════════════════════════════════════════════════════════════════
    // transfer
    // ════════════════════════════════════════════════════════════════════════

//    @Test
//    void transfer_deveRetornarTransacao_quandoDadosValidos() {
//        // ARRANGE
//        Account destination = new Account();
//        destination.getId() = 2L;
//
//        PanacheMock.mock(Account.class);
//        when(Account.findById(1L)).thenReturn(account);
//        when(Account.findById(2L)).thenReturn(destination);
//        when(currentUserService.getLoggedUser()).thenReturn(loggedUser);
//        doNothing().when(accountValidator).checkAccountOwnership(any(), any());
//        doNothing().when(accountValidator).validateAmount(any());
//        doNothing().when(accountValidator).validateDifferentAccounts(any(), any());
//        doNothing().when(accountValidator).validateSufficientBalance(any(), any());
//        when(transactionService.createTransfer(1L, 2L, BigDecimal.valueOf(500)))
//                .thenReturn(transaction);
//
//        // ACT
//        Transaction result = accountService.transfer(1L, 2L, BigDecimal.valueOf(500));
//
//        // ASSERT
//        assertNotNull(result);
//        verify(accountValidator, times(1)).validateDifferentAccounts(1L, 2L);
//        verify(accountValidator, times(1)).validateSufficientBalance(account, BigDecimal.valueOf(500));
//        verify(transactionService, times(1)).createTransfer(1L, 2L, BigDecimal.valueOf(500));
//    }

    @Test
    void transfer_deveLancarNotFoundException_quandoContaOrigemNaoExiste() {
        // ARRANGE
        PanacheMock.mock(Account.class);
        when(Account.findById(99L)).thenReturn(null);

        // ACT + ASSERT
        assertThrows(NotFoundException.class,
                () -> accountService.transfer(99L, 2L, BigDecimal.valueOf(500)));

        verify(transactionService, never()).createTransfer(any(), any(), any());
    }

    @Test
    void transfer_deveLancarNotFoundException_quandoContaDestinoNaoExiste() {
        // ARRANGE
        PanacheMock.mock(Account.class);
        when(Account.findById(1L)).thenReturn(account);
        when(Account.findById(2L)).thenReturn(null);

        // ACT + ASSERT
        assertThrows(NotFoundException.class,
                () -> accountService.transfer(1L, 2L, BigDecimal.valueOf(500)));

        verify(transactionService, never()).createTransfer(any(), any(), any());
    }

    @Test
    void transfer_deveLancarExcecao_quandoContasIguais() {
        // ARRANGE
        PanacheMock.mock(Account.class);
        when(Account.findById(1L)).thenReturn(account);
        when(currentUserService.getLoggedUser()).thenReturn(loggedUser);
        doNothing().when(accountValidator).checkAccountOwnership(any(), any());
        doNothing().when(accountValidator).validateAmount(any());
        doThrow(new IllegalArgumentException("Contas devem ser diferentes"))
                .when(accountValidator).validateDifferentAccounts(1L, 1L);

        // ACT + ASSERT
        assertThrows(IllegalArgumentException.class,
                () -> accountService.transfer(1L, 1L, BigDecimal.valueOf(500)));

        verify(transactionService, never()).createTransfer(any(), any(), any());
    }

//    @Test
//    void transfer_deveLancarExcecao_quandoSaldoInsuficiente() {
//        // ARRANGE
//        Account destination = new Account();
//        destination.id = 2L;
//
//        PanacheMock.mock(Account.class);
//        when(Account.findById(1L)).thenReturn(account);
//        when(Account.findById(2L)).thenReturn(destination);
//        when(currentUserService.getLoggedUser()).thenReturn(loggedUser);
//        doNothing().when(accountValidator).checkAccountOwnership(any(), any());
//        doNothing().when(accountValidator).validateAmount(any());
//        doNothing().when(accountValidator).validateDifferentAccounts(any(), any());
//        doThrow(new IllegalArgumentException("Saldo insuficiente"))
//                .when(accountValidator).validateSufficientBalance(account, BigDecimal.valueOf(9999));
//
//        // ACT + ASSERT
//        assertThrows(IllegalArgumentException.class,
//                () -> accountService.transfer(1L, 2L, BigDecimal.valueOf(9999)));
//
//        verify(transactionService, never()).createTransfer(any(), any(), any());
//    }

    // ════════════════════════════════════════════════════════════════════════
    // loggedUser
    // ════════════════════════════════════════════════════════════════════════

//    @Test
//    void loggedUser_deveRetornarUsuarioLogado() {
//        // ARRANGE
//        when(currentUserService.getLoggedUser()).thenReturn(loggedUser);
//
//        // ACT
//        LoggedUser result = accountService.loggedUser();
//
//        // ASSERT
//        assertNotNull(result);
//        assertEquals(loggedUser.getCustomerId(), result.getCustomerId());
//        verify(currentUserService, times(1)).getLoggedUser();
//    }
}