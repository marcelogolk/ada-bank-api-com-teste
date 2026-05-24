package unitario;

import br.com.ada.quarkus.model.*;
import br.com.ada.quarkus.repository.AccountRepository;
import br.com.ada.quarkus.service.*;
import br.com.ada.quarkus.util.PageResult;
import br.com.ada.quarkus.validator.AccountValidator;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.hibernate.query.NativeQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query; // Importar jakarta.persistence.Query

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    @InjectMock
    EntityManager entityManager;

    @InjectMock
    AccountRepository accountRepository;

    private Account account;
    private Account destinationAccount;
    private LoggedUser loggedUser;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        // PanacheMock.mock(Account.class); // REMOVIDO: Não é mais necessário mockar Account diretamente

        // ── Fixtures ─────────────────────────────────────────────────────
        account = new Account(1L, "0000000011", AccountType.CORRENTE, 10L);
        // Não definimos saldo aqui, pois é uma @Formula.
        // Se um teste precisar de um saldo específico, ele deve mockar o getBalance()
        // ou garantir que as transações mockadas resultem no saldo desejado.
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
        // PanacheMock.reset(); // REMOVIDO: Não é mais necessário resetar PanacheMock
    }

    // ════════════════════════════════════════════════════════════════════════
    // findById
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void findById_deveRetornarConta_quandoContaExiste() {
        // ARRANGE
        when(accountRepository.findByIdOptional(1L)).thenReturn(Optional.of(account));

        // ACT
        Account result = accountService.findById(1L);

        // ASSERT
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(accountRepository, times(1)).findByIdOptional(1L);
    }

    @Test
    void findById_deveLancarNotFoundException_quandoContaNaoExiste() {
        // ARRANGE
        when(accountRepository.findByIdOptional(99L)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(NotFoundException.class,
                () -> accountService.findById(99L));

        verify(accountRepository, times(1)).findByIdOptional(99L);
    }

    // ════════════════════════════════════════════════════════════════════════
    // deposit
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void deposit_deveRetornarTransacao_quandoDadosValidos() {
        // ARRANGE
        when(accountRepository.findByIdOptional(1L)).thenReturn(Optional.of(account));
        doNothing().when(accountValidator).validateAmount(any());
        doNothing().when(accountValidator).validateElectronicAccountForDeposit(any());
        when(transactionService.createDeposit(1L, BigDecimal.valueOf(200)))
                .thenReturn(transaction);

        // ACT
        Transaction result = accountService.deposit(1L, BigDecimal.valueOf(200));

        // ASSERT
        assertNotNull(result);
        verify(accountRepository, times(1)).findByIdOptional(1L);
        verify(accountValidator, times(1)).validateAmount(BigDecimal.valueOf(200));
        verify(accountValidator, times(1)).validateElectronicAccountForDeposit(account);
        verify(transactionService, times(1)).createDeposit(1L, BigDecimal.valueOf(200));
        verify(accountRepository, never()).persist(any(Account.class)); // Garante que persist NÃO foi chamado
    }

    @Test
    void deposit_deveLancarNotFoundException_quandoContaNaoExiste() {
        // ARRANGE
        when(accountRepository.findByIdOptional(99L)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(NotFoundException.class,
                () -> accountService.deposit(99L, BigDecimal.valueOf(200)));

        verify(accountRepository, times(1)).findByIdOptional(99L);
        verify(transactionService, never()).createDeposit(any(), any());
    }

    @Test
    void deposit_deveLancarBadRequestException_quandoValorInvalido() {
        // ARRANGE
        when(accountRepository.findByIdOptional(1L)).thenReturn(Optional.of(account));
        doThrow(new BadRequestException("O valor da operação deve ser maior que zero"))
                .when(accountValidator).validateAmount(BigDecimal.valueOf(-50));

        // ACT + ASSERT
        assertThrows(BadRequestException.class,
                () -> accountService.deposit(1L, BigDecimal.valueOf(-50)));

        verify(accountRepository, times(1)).findByIdOptional(1L);
        verify(transactionService, never()).createDeposit(any(), any());
    }

    @Test
    void deposit_deveLancarBadRequestException_quandoContaNaoPermiteDeposito() {
        // ARRANGE
        when(accountRepository.findByIdOptional(1L)).thenReturn(Optional.of(account));
        doNothing().when(accountValidator).validateAmount(any());
        doThrow(new BadRequestException("Conta do tipo ELETRONICA não permite depósito"))
                .when(accountValidator).validateElectronicAccountForDeposit(account);

        // ACT + ASSERT
        assertThrows(BadRequestException.class,
                () -> accountService.deposit(1L, BigDecimal.valueOf(200)));

        verify(accountRepository, times(1)).findByIdOptional(1L);
        verify(transactionService, never()).createDeposit(any(), any());
    }

    // ════════════════════════════════════════════════════════════════════════
    // withdraw
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void withdraw_deveRetornarTransacao_quandoDadosValidos() {
        // ARRANGE
        // Criar um mock da Account para poder controlar o getBalance()
        Account mockedAccount = mock(Account.class);
        when(mockedAccount.getId()).thenReturn(1L);
        when(mockedAccount.getAccountNumber()).thenReturn("0000000011");
        when(mockedAccount.getType()).thenReturn(AccountType.CORRENTE);
        when(mockedAccount.getCustomerId()).thenReturn(10L);
        // Mockar o getBalance() da conta para que a validação de saldo funcione
        when(mockedAccount.getBalance()).thenReturn(BigDecimal.valueOf(1000)); // Saldo suficiente para o saque

        // Configurar o accountRepository para retornar o mockedAccount
        when(accountRepository.findByIdOptional(1L)).thenReturn(Optional.of(mockedAccount));
        when(currentUserService.getLoggedUser()).thenReturn(loggedUser);

        // Usar any(Account.class) para os métodos do accountValidator que recebem Account
        doNothing().when(accountValidator).checkAccountOwnership(any(Account.class), any(LoggedUser.class));
        doNothing().when(accountValidator).validateAmount(any(BigDecimal.class));
        doNothing().when(accountValidator).validateElectronicAccountForWithdraw(any(Account.class));
        doNothing().when(accountValidator).validateSufficientBalance(any(Account.class), any(BigDecimal.class));

        when(transactionService.createWithdraw(1L, BigDecimal.valueOf(300)))
                .thenReturn(transaction);

        // ACT
        Transaction result = accountService.withdraw(1L, BigDecimal.valueOf(300));

        // ASSERT
        assertNotNull(result);
        verify(accountRepository, times(1)).findByIdOptional(1L);
        // Verificar as chamadas com o mockedAccount
        verify(accountValidator, times(1)).checkAccountOwnership(mockedAccount, loggedUser);
        verify(accountValidator, times(1)).validateAmount(BigDecimal.valueOf(300));
        verify(accountValidator, times(1)).validateElectronicAccountForWithdraw(mockedAccount);
        verify(accountValidator, times(1)).validateSufficientBalance(mockedAccount, BigDecimal.valueOf(300));
        verify(transactionService, times(1)).createWithdraw(1L, BigDecimal.valueOf(300));
        verify(accountRepository, never()).persist(any(Account.class)); // Garante que persist NÃO foi chamado
    }

    @Test
    void withdraw_deveLancarNotFoundException_quandoContaNaoExiste() {
        // ARRANGE
        when(accountRepository.findByIdOptional(99L)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(NotFoundException.class,
                () -> accountService.withdraw(99L, BigDecimal.valueOf(300)));

        verify(accountRepository, times(1)).findByIdOptional(99L);
        verify(transactionService, never()).createWithdraw(any(), any());
    }

    @Test
    void withdraw_deveLancarBadRequestException_quandoSaldoInsuficiente() {
        // ARRANGE
        // Criar um mock da Account para poder controlar o getBalance()
        Account mockedAccount = mock(Account.class); // <-- AQUI ESTÁ A MUDANÇA CHAVE

        // Configurar o mock para retornar os valores esperados
        when(mockedAccount.getId()).thenReturn(1L);
        when(mockedAccount.getAccountNumber()).thenReturn("0000000011");
        when(mockedAccount.getType()).thenReturn(AccountType.CORRENTE);
        when(mockedAccount.getCustomerId()).thenReturn(10L);
        when(mockedAccount.getBalance()).thenReturn(BigDecimal.valueOf(100)); // Agora sim, mockando o getBalance()

        // O accountRepository.findByIdOptional deve retornar o nosso mock
        when(accountRepository.findByIdOptional(1L)).thenReturn(Optional.of(mockedAccount));
        when(currentUserService.getLoggedUser()).thenReturn(loggedUser);
        doNothing().when(accountValidator).checkAccountOwnership(any(Account.class), any(LoggedUser.class)); // Usar any(Account.class)
        doNothing().when(accountValidator).validateAmount(any(BigDecimal.class));
        doNothing().when(accountValidator).validateElectronicAccountForWithdraw(any(Account.class)); // Usar any(Account.class)

        // A validação de saldo é que deve lançar a exceção
        doThrow(new BadRequestException("Saldo insuficiente para realizar a operação"))
                .when(accountValidator).validateSufficientBalance(mockedAccount, BigDecimal.valueOf(9999)); // Usar mockedAccount

        // ACT + ASSERT
        assertThrows(BadRequestException.class,
                () -> accountService.withdraw(1L, BigDecimal.valueOf(9999)));

        // VERIFY
        verify(accountRepository, times(1)).findByIdOptional(1L);
        verify(accountValidator, times(1)).validateSufficientBalance(mockedAccount, BigDecimal.valueOf(9999));
        verify(transactionService, never()).createWithdraw(any(), any());
    }

    @Test
    void withdraw_deveLancarForbiddenException_quandoContaNaoPertenceAoUsuario() {
        // ARRANGE
        when(accountRepository.findByIdOptional(1L)).thenReturn(Optional.of(account));
        when(currentUserService.getLoggedUser()).thenReturn(loggedUser);
        doThrow(new io.quarkus.security.ForbiddenException("Acesso negado: apenas o proprietário da conta ou um gerente pode realizar esta operação"))
                .when(accountValidator).checkAccountOwnership(account, loggedUser);

        // ACT + ASSERT
        assertThrows(io.quarkus.security.ForbiddenException.class,
                () -> accountService.withdraw(1L, BigDecimal.valueOf(100)));

        verify(accountRepository, times(1)).findByIdOptional(1L);
        verify(transactionService, never()).createWithdraw(any(), any());
    }

    @Test
    void withdraw_deveLancarBadRequestException_quandoValorInvalido() {
        // ARRANGE
        when(accountRepository.findByIdOptional(1L)).thenReturn(Optional.of(account));
        when(currentUserService.getLoggedUser()).thenReturn(loggedUser);

        doNothing().when(accountValidator).checkAccountOwnership(account, loggedUser);
        doThrow(new BadRequestException("O valor da operação deve ser maior que zero"))
                .when(accountValidator).validateAmount(BigDecimal.valueOf(-10));

        // ACT + ASSERT
        assertThrows(BadRequestException.class,
                () -> accountService.withdraw(1L, BigDecimal.valueOf(-10)));

        verify(accountRepository, times(1)).findByIdOptional(1L);
        verify(transactionService, never()).createWithdraw(any(), any());
    }

    // ════════════════════════════════════════════════════════════════════════
    // transfer
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void transfer_deveRetornarTransacao_quandoDadosValidos() {
        // ARRANGE
        // Criar mocks das Accounts para poder controlar o getBalance() e outros métodos
        Account mockedSourceAccount = mock(Account.class);
        when(mockedSourceAccount.getId()).thenReturn(1L);
        when(mockedSourceAccount.getAccountNumber()).thenReturn("0000000011");
        when(mockedSourceAccount.getType()).thenReturn(AccountType.CORRENTE);
        when(mockedSourceAccount.getCustomerId()).thenReturn(10L);
        when(mockedSourceAccount.getBalance()).thenReturn(BigDecimal.valueOf(1000)); // Saldo suficiente

        Account mockedDestinationAccount = mock(Account.class);
        when(mockedDestinationAccount.getId()).thenReturn(2L);
        when(mockedDestinationAccount.getAccountNumber()).thenReturn("0000000022");
        when(mockedDestinationAccount.getType()).thenReturn(AccountType.CORRENTE);
        when(mockedDestinationAccount.getCustomerId()).thenReturn(20L);
        // Não precisamos mockar o getBalance() da conta de destino para este teste específico,
        // pois a validação de saldo é feita apenas na conta de origem.

        when(accountRepository.findByIdOptional(1L)).thenReturn(Optional.of(mockedSourceAccount));
        when(accountRepository.findByIdOptional(2L)).thenReturn(Optional.of(mockedDestinationAccount));
        when(currentUserService.getLoggedUser()).thenReturn(loggedUser);
        doNothing().when(accountValidator).checkAccountOwnership(any(Account.class), any(LoggedUser.class));
        doNothing().when(accountValidator).validateAmount(any(BigDecimal.class));
        doNothing().when(accountValidator).validateDifferentAccounts(anyLong(), anyLong());
        doNothing().when(accountValidator).validateSufficientBalance(any(Account.class), any(BigDecimal.class));
        when(transactionService.createTransfer(1L, 2L, BigDecimal.valueOf(500)))
                .thenReturn(transaction);

        // ACT
        Transaction result = accountService.transfer(1L, 2L, BigDecimal.valueOf(500));

        // ASSERT
        assertNotNull(result);
        verify(accountRepository, times(1)).findByIdOptional(1L);
        verify(accountRepository, times(1)).findByIdOptional(2L);
        verify(accountValidator, times(1)).checkAccountOwnership(mockedSourceAccount, loggedUser);
        verify(accountValidator, times(1)).validateAmount(BigDecimal.valueOf(500));
        verify(accountValidator, times(1)).validateDifferentAccounts(1L, 2L);
        verify(accountValidator, times(1)).validateSufficientBalance(mockedSourceAccount, BigDecimal.valueOf(500));
        verify(transactionService, times(1)).createTransfer(1L, 2L, BigDecimal.valueOf(500));
        verify(accountRepository, never()).persist(any(Account.class)); // Garante que persist NÃO foi chamado
    }


    @Test
    void transfer_deveLancarNotFoundException_quandoContaOrigemNaoExiste() {
        // ARRANGE
        // Mocka que a conta de origem (99L) não existe
        when(accountRepository.findByIdOptional(99L)).thenReturn(Optional.empty());
        // Não precisamos mockar a conta de destino, pois ela nunca será buscada
        // quando a conta de origem não é encontrada.

        // ACT + ASSERT
        assertThrows(NotFoundException.class,
                () -> accountService.transfer(99L, 2L, BigDecimal.valueOf(500)));

        // Verifica que a busca pela conta de origem foi feita
        verify(accountRepository, times(1)).findByIdOptional(99L);
        // Verifica que a busca pela conta de destino NÃO foi feita
        verify(accountRepository, never()).findByIdOptional(2L);
        // Verifica que nenhuma transação foi criada
        verify(transactionService, never()).createTransfer(any(), any(), any());
    }

    @Test
    void transfer_deveLancarBadRequestException_quandoContasIguais() {
        // ARRANGE
        // Mockar a conta de origem e destino, que são a mesma neste teste
        Account mockedAccount = mock(Account.class);
        when(mockedAccount.getId()).thenReturn(1L);
        when(accountRepository.findByIdOptional(1L)).thenReturn(Optional.of(mockedAccount));

        when(currentUserService.getLoggedUser()).thenReturn(loggedUser);
        doNothing().when(accountValidator).checkAccountOwnership(any(Account.class), any(LoggedUser.class));
        doNothing().when(accountValidator).validateAmount(any(BigDecimal.class));
        doThrow(new BadRequestException("A conta de origem deve ser diferente da conta de destino"))
                .when(accountValidator).validateDifferentAccounts(1L, 1L);

        // ACT + ASSERT
        assertThrows(BadRequestException.class,
                () -> accountService.transfer(1L, 1L, BigDecimal.valueOf(500)));

        // A conta 1L será buscada duas vezes (uma como origem, outra como destino)
        verify(accountRepository, times(2)).findByIdOptional(1L);
        verify(transactionService, never()).createTransfer(any(), any(), any());
    }

    @Test
    void transfer_deveLancarBadRequestException_quandoSaldoInsuficiente() {
        // ARRANGE
        // Criar mocks para as contas de origem e destino
        Account mockedSourceAccount = Mockito.mock(Account.class);
        Account mockedDestinationAccount = Mockito.mock(Account.class);

        // Configurar os mocks para retornar os IDs e tipos corretos
        when(mockedSourceAccount.getId()).thenReturn(1L);
        when(mockedSourceAccount.getType()).thenReturn(AccountType.CORRENTE);
        when(mockedSourceAccount.getCustomerId()).thenReturn(10L);

        when(mockedDestinationAccount.getId()).thenReturn(2L);
        when(mockedDestinationAccount.getType()).thenReturn(AccountType.CORRENTE);
        when(mockedDestinationAccount.getCustomerId()).thenReturn(20L);

        // Configurar o accountRepository para retornar os mocks
        when(accountRepository.findByIdOptional(1L)).thenReturn(Optional.of(mockedSourceAccount));
        when(accountRepository.findByIdOptional(2L)).thenReturn(Optional.of(mockedDestinationAccount));

        when(currentUserService.getLoggedUser()).thenReturn(loggedUser);
        doNothing().when(accountValidator).checkAccountOwnership(any(Account.class), any(LoggedUser.class));
        doNothing().when(accountValidator).validateAmount(any(BigDecimal.class));
        doNothing().when(accountValidator).validateDifferentAccounts(anyLong(), anyLong());

        // Mockar o getBalance() da conta de origem para que a validação de saldo funcione
        // Agora, como mockedSourceAccount é um mock, podemos stubbar seu getBalance()
        when(mockedSourceAccount.getBalance()).thenReturn(BigDecimal.valueOf(100)); // Saldo insuficiente

        // Stubbar o validador para lançar a exceção de saldo insuficiente
        doThrow(new BadRequestException("Saldo insuficiente para realizar a operação"))
                .when(accountValidator).validateSufficientBalance(mockedSourceAccount, BigDecimal.valueOf(9999));

        // ACT + ASSERT
        assertThrows(BadRequestException.class,
                () -> accountService.transfer(1L, 2L, BigDecimal.valueOf(9999)));

        // VERIFY
        verify(accountRepository, times(1)).findByIdOptional(1L);
        verify(accountRepository, times(1)).findByIdOptional(2L); // A conta de destino é buscada antes da validação de saldo
        verify(accountValidator, times(1)).checkAccountOwnership(mockedSourceAccount, loggedUser);
        verify(accountValidator, times(1)).validateAmount(BigDecimal.valueOf(9999));
        verify(accountValidator, times(1)).validateDifferentAccounts(1L, 2L);
        verify(accountValidator, times(1)).validateSufficientBalance(mockedSourceAccount, BigDecimal.valueOf(9999));
        verify(transactionService, never()).createTransfer(any(), any(), any());
        verify(accountRepository, never()).persist(any(Account.class)); // Garante que persist NÃO foi chamado
    }

    // ════════════════════════════════════════════════════════════════════════
    // loggedUser
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void loggedUser_deveRetornarUsuarioLogado() {
        // ARRANGE
        when(currentUserService.getLoggedUser()).thenReturn(loggedUser);

        // ACT
        LoggedUser result = accountService.loggedUser();

        // ASSERT
        assertNotNull(result);
        assertEquals(loggedUser.id(), result.id());
        verify(currentUserService, times(1)).getLoggedUser();
    }

    // ════════════════════════════════════════════════════════════════════════
    // list
    // ════════════════════════════════════════════════════════════════════════


    // ════════════════════════════════════════════════════════════════════════
    // create
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void create_devePropagarExcecao_quandoClienteNaoExiste() {
        // ARRANGE
        Account novaConta = new Account(null, null, AccountType.CORRENTE, 99L);

        // customerService.findById lança NotFoundException
        when(customerService.findById(99L)).thenThrow(new NotFoundException("Cliente não encontrado"));

        // ACT + ASSERT
        assertThrows(NotFoundException.class, () -> accountService.create(novaConta));

        // Nesse cenário, nada de sequence, nem insert, nem findById da Account
        verify(customerService, times(1)).findById(99L);
        verify(accountRepository, never()).findByIdOptional(anyLong());
        verify(entityManager, never()).createNativeQuery(anyString());
    }
}