
package unitario;

import br.com.ada.quarkus.model.*;
import br.com.ada.quarkus.service.*;
import org.hibernate.query.NativeQuery; // Adicione este import
import jakarta.persistence.Query; // Mantenha este import se ainda for usado em outros lugares
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.*;
import jakarta.persistence.EntityManager;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static groovy.xml.Entity.gt;
import static groovy.xml.Entity.lt;
import static io.quarkus.hibernate.orm.panache.PanacheEntityBase.findById;
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
        when(findById(99L)).thenReturn(null);

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
        when(findById(1L)).thenReturn(account);
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
        when(findById(99L)).thenReturn(null);

        // ACT + ASSERT
        assertThrows(NotFoundException.class,
                () -> accountService.deposit(99L, BigDecimal.valueOf(200)));

        verify(transactionService, never()).createDeposit(any(), any());
    }

    @Test
    void deposit_deveLancarExcecao_quandoValorInvalido() {
        // ARRANGE
        PanacheMock.mock(Account.class);
        when(findById(1L)).thenReturn(account);
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
        when(findById(1L)).thenReturn(account);
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
        when(findById(99L)).thenReturn(null);

        // ACT + ASSERT
        assertThrows(NotFoundException.class,
                () -> accountService.withdraw(99L, BigDecimal.valueOf(300)));

        verify(transactionService, never()).createWithdraw(any(), any());
    }

    @Test
    void withdraw_deveLancarExcecao_quandoSaldoInsuficiente() {
        // ARRANGE
        PanacheMock.mock(Account.class);
        when(findById(1L)).thenReturn(account);
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
        when(findById(1L)).thenReturn(account);
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

    @Test
    void transfer_deveRetornarTransacao_quandoDadosValidos() {
        when(Account.<Account>findById(1L)).thenReturn(account); // Usando o account do @BeforeEach
        when(Account.<Account>findById(2L)).thenReturn(destinationAccount); // Usando o destinationAccount do @BeforeEach
        when(currentUserService.getLoggedUser()).thenReturn(loggedUser);
        doNothing().when(accountValidator).checkAccountOwnership(any(), any());
        doNothing().when(accountValidator).validateAmount(any());
        doNothing().when(accountValidator).validateDifferentAccounts(any(), any());
        doNothing().when(accountValidator).validateSufficientBalance(any(), any());
        when(transactionService.createTransfer(1L, 2L, BigDecimal.valueOf(500)))
                .thenReturn(transaction);

        // ACT
        Transaction result = accountService.transfer(1L, 2L, BigDecimal.valueOf(500));

        // ASSERT
        assertNotNull(result);
        verify(accountValidator, times(1)).validateDifferentAccounts(1L, 2L);
        verify(accountValidator, times(1)).validateSufficientBalance(account, BigDecimal.valueOf(500));
        verify(transactionService, times(1)).createTransfer(1L, 2L, BigDecimal.valueOf(500));
        // Adicionei as verificações para as chamadas do PanacheMock
        PanacheMock.verify(Account.class, times(1)).findById(1L);
        PanacheMock.verify(Account.class, times(1)).findById(2L);
    }


    @Test
    void transfer_deveLancarNotFoundException_quandoContaOrigemNaoExiste() {
        // ARRANGE
        PanacheMock.mock(Account.class);
        when(findById(99L)).thenReturn(null);

        // ACT + ASSERT
        assertThrows(NotFoundException.class,
                () -> accountService.transfer(99L, 2L, BigDecimal.valueOf(500)));

        verify(transactionService, never()).createTransfer(any(), any(), any());
    }

    @Test
    void transfer_deveLancarNotFoundException_quandoContaDestinoNaoExiste() {
        // ARRANGE
        PanacheMock.mock(Account.class);
        when(findById(1L)).thenReturn(account);
        when(findById(2L)).thenReturn(null);

        // ACT + ASSERT
        assertThrows(NotFoundException.class,
                () -> accountService.transfer(1L, 2L, BigDecimal.valueOf(500)));

        verify(transactionService, never()).createTransfer(any(), any(), any());
    }

    @Test
    void transfer_deveLancarExcecao_quandoContasIguais() {
        // ARRANGE
        PanacheMock.mock(Account.class);
        when(findById(1L)).thenReturn(account);
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

    @Test
    void transfer_deveLancarExcecao_quandoSaldoInsuficiente() {
        // ARRANGE
        Account destination = new Account();
        when(findById(1L)).thenReturn(account);
        when(findById(2L)).thenReturn(destination);
        when(currentUserService.getLoggedUser()).thenReturn(loggedUser);
        doNothing().when(accountValidator).checkAccountOwnership(any(), any());
        doNothing().when(accountValidator).validateAmount(any());
        doNothing().when(accountValidator).validateDifferentAccounts(any(), any());
        doThrow(new IllegalArgumentException("Saldo insuficiente"))
                .when(accountValidator).validateSufficientBalance(account, BigDecimal.valueOf(9999));

        // ACT + ASSERT
        assertThrows(IllegalArgumentException.class,
                () -> accountService.transfer(1L, 2L, BigDecimal.valueOf(9999)));

        verify(transactionService, never()).createTransfer(any(), any(), any());
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
//    @Test
//    void list_deveRetornarPageResultDeContas_quandoCustomerIdNulo() {
//        // ARRANGE
//        // PanacheMock.mock(Account.class); // Já está no @BeforeEach, garantindo que Account é mockável
//
//        Account account1 = new Account(1L, "0000000011", AccountType.CORRENTE, 10L);
//        Account account2 = new Account(2L, "0000000022", AccountType.POUPANCA, 10L);
//        List<Account> accounts = List.of(account1, account2);
//
//        // 1. Crie o mock do PanacheQuery<Account>
//        PanacheQuery<Account> panacheQueryMock = Mockito.mock(PanacheQuery.class);
//
//        // 2. Configure o comportamento do panacheQueryMock
//        when(panacheQueryMock.page(Page.of(0, 10))).thenReturn(panacheQueryMock);
//        when(panacheQueryMock.list()).thenReturn(accounts);
//        when(panacheQueryMock.count()).thenReturn((long) accounts.size());
//
//        // 3. Configure o método estático Account.findAll para retornar o panacheQueryMock
//        // A chave aqui é garantir que o PanacheMock esteja interceptando corretamente.
//        // Se o problema for que o 'when' não está sendo aplicado,
//        // pode ser necessário garantir que o PanacheMock esteja "pronto" para isso.
//        // A linha abaixo é a forma padrão e deve funcionar.
//        when(Account.<Account>findAll(Sort.by("id"))).thenReturn(panacheQueryMock);
//
//
//        // ACT
//        PageResult<Account> result = accountService.list(null, 0, 10);
//
//        // ASSERT
//        assertNotNull(result);
//        assertEquals(2, result.totalElements());
//        assertEquals(2, result.content().size());
//        assertEquals(0, result.page());
//        assertEquals(10, result.size());
//        assertEquals(account1, result.content().get(0));
//        assertEquals(account2, result.content().get(1));
//
//        PanacheMock.verify(Account.class, times(1)).findAll(Sort.by("id"));
//        PanacheMock.verify(Account.class, never()).find(eq("customerId"), (Object) any());
//    }

//    @Test
//    void create_deveCriarConta_quandoClienteExiste() {
//        // ARRANGE
//        Account novaConta = new Account(null, null, AccountType.CORRENTE, 10L);
//        Customer customer = new Customer();
//        when(customerService.findById(10L)).thenReturn(customer);
//
//        when(accountValidator.calculateCheckDigit(anyString())).thenReturn(7);
//
//        // MOCKANDO O ENTITYMANAGER
//        // Mock para o nextval
//        NativeQuery mockQueryNextVal = Mockito.mock(NativeQuery.class); // <-- Mude aqui para NativeQuery
//        when(entityManager.createNativeQuery("select nextval('account_id_seq')")).thenReturn(mockQueryNextVal);
//        when(mockQueryNextVal.getSingleResult()).thenReturn(1L); // Retorna o ID gerado
//        // Mock para o INSERT
//        Query mockQueryInsert = Mockito.mock(Query.class); // Garanta que 'Query' aqui é jakarta.persistence.Query
//
//        // Mock para o INSERT
//        NativeQuery mockQueryInsert = Mockito.mock(NativeQuery.class); // <-- Mude aqui para NativeQuery
//        when(entityManager.createNativeQuery(anyString())).thenReturn(mockQueryInsert); // Mocka qualquer createNativeQuery
//        when(mockQueryInsert.setParameter(anyString(), any())).thenReturn(mockQueryInsert); // Permite encadeamento
//        when(mockQueryInsert.executeUpdate()).thenReturn(1); // Retorna 1 para indicar sucesso
//
//        // Para o getRequiredAccount(nextId) no final:
//        Account contaCriada = new Account(1L, "0000000017", AccountType.CORRENTE, 10L);
//        when(Account.<Account>findById(1L)).thenReturn(contaCriada); // Corrigido o <Account>
//
//        // ACT
//        Account result = accountService.create(novaConta);
//
//        // ASSERT
//        assertNotNull(result);
//        assertEquals(1L, result.getId());
//        assertEquals("0000000017", result.getAccountNumber());
//        assertEquals(AccountType.CORRENTE, result.getType());
//        assertEquals(10L, result.getCustomerId());
//
//        // verify
//        verify(customerService, times(1)).findById(10L);
//        verify(accountValidator, times(1)).calculateCheckDigit(anyString());
//        PanacheMock.verify(Account.class, times(1)).findById(1L);
//
//        // Verificações do EntityManager
//        verify(entityManager, times(1)).createNativeQuery("select nextval('account_id_seq')");
//        verify(mockQueryNextVal, times(1)).getSingleResult();
//        verify(entityManager, times(1)).createNativeQuery(
//                argThat(sql -> sql.contains("INSERT INTO account"))); // Corrigido o '->'
//        verify(mockQueryInsert, times(1)).setParameter("id", 1L);
//        verify(mockQueryInsert, times(1)).setParameter("accountNumber", "0000000017");
//        verify(mockQueryInsert, times(1)).setParameter("type", AccountType.CORRENTE.name());
//        verify(mockQueryInsert, times(1)).setParameter("customerId", 10L);
//        verify(mockQueryInsert, times(1)).executeUpdate();
//    }


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
        PanacheMock.verify(Account.class, never()).findById(anyLong());
    }

    @Test
    void withdraw_deveLancarExcecao_quandoValorInvalido() {
        // ARRANGE
        when(findById(1L)).thenReturn(account);
        when(currentUserService.getLoggedUser()).thenReturn(loggedUser);

        doNothing().when(accountValidator).checkAccountOwnership(account, loggedUser);
        doThrow(new IllegalArgumentException("Valor inválido"))
                .when(accountValidator).validateAmount(BigDecimal.valueOf(-10));

        // ACT + ASSERT
        assertThrows(IllegalArgumentException.class,
                () -> accountService.withdraw(1L, BigDecimal.valueOf(-10)));

        verify(transactionService, never()).createWithdraw(any(), any());
    }
}

