package unitario;

import br.com.ada.quarkus.model.Transaction;
import br.com.ada.quarkus.model.TransactionType;
import br.com.ada.quarkus.repository.TransactionRepository;
import br.com.ada.quarkus.service.TransactionService;
import br.com.ada.quarkus.util.PageResult;
import br.com.ada.quarkus.validator.TransactionValidator;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class TransactionServiceTest {

    @Inject
    TransactionService transactionService;

    @InjectMock
    TransactionRepository transactionRepository;

    @InjectMock
    TransactionValidator transactionValidator;

    private Transaction transaction;
    private Transaction depositTransaction;
    private Transaction withdrawTransaction;
    private Transaction transferTransaction;

    @BeforeEach
    void setUp() {
        transaction = new Transaction(
                1L,
                TransactionType.DEPOSITO,
                BigDecimal.valueOf(100.00),
                LocalDateTime.now(),
                null,
                10L
        );

        depositTransaction = new Transaction(
                2L,
                TransactionType.DEPOSITO,
                BigDecimal.valueOf(200.00),
                LocalDateTime.now(),
                null,
                10L
        );

        withdrawTransaction = new Transaction(
                3L,
                TransactionType.SAQUE,
                BigDecimal.valueOf(50.00),
                LocalDateTime.now(),
                10L,
                null
        );

        transferTransaction = new Transaction(
                4L,
                TransactionType.TRANSFERENCIA,
                BigDecimal.valueOf(150.00),
                LocalDateTime.now(),
                10L,
                20L
        );
    }

    // --- Testes para findById ---

    @Test
    void findById_deveRetornarTransacao_quandoExiste() {
        // ARRANGE
        when(transactionRepository.findByIdOptional(1L)).thenReturn(Optional.of(transaction));

        // ACT
        Transaction result = transactionService.findById(1L);

        // ASSERT
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(TransactionType.DEPOSITO, result.getType());
        verify(transactionRepository, times(1)).findByIdOptional(1L);
    }

    @Test
    void findById_deveLancarNotFoundException_quandoNaoExiste() {
        // ARRANGE
        when(transactionRepository.findByIdOptional(99L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> transactionService.findById(99L));
        assertEquals("Transação não encontrada", exception.getMessage());
        verify(transactionRepository, times(1)).findByIdOptional(99L);
    }

    // --- Testes para listByAccountId ---

    @Test
    void listByAccountId_deveRetornarListaPaginada_quandoTransacoesExistem() {
        // ARRANGE
        List<Transaction> transactions = Arrays.asList(transaction, depositTransaction);
        when(transactionRepository.listByAccountId(10L, 0, 10)).thenReturn(transactions);
        when(transactionRepository.countByAccountId(10L)).thenReturn(2L);

        // ACT
        PageResult<Transaction> result = transactionService.listByAccountId(10L, 0, 10);

        // ASSERT
        assertNotNull(result);
        assertEquals(2, result.content().size());
        assertEquals(2L, result.totalElements());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        verify(transactionRepository, times(1)).listByAccountId(10L, 0, 10);
        verify(transactionRepository, times(1)).countByAccountId(10L);
    }

    @Test
    void listByAccountId_deveRetornarListaVazia_quandoNaoHaTransacoes() {
        // ARRANGE
        when(transactionRepository.listByAccountId(10L, 0, 10)).thenReturn(Collections.emptyList());
        when(transactionRepository.countByAccountId(10L)).thenReturn(0L);

        // ACT
        PageResult<Transaction> result = transactionService.listByAccountId(10L, 0, 10);

        // ASSERT
        assertNotNull(result);
        assertTrue(result.content().isEmpty());
        assertEquals(0L, result.totalElements());
        verify(transactionRepository, times(1)).listByAccountId(10L, 0, 10);
        verify(transactionRepository, times(1)).countByAccountId(10L);
    }

    // --- Testes para listTodayByAccountId ---

    @Test
    void listTodayByAccountId_deveRetornarListaPaginada_quandoTransacoesExistemHoje() {
        // ARRANGE
        List<Transaction> transactions = Arrays.asList(depositTransaction, withdrawTransaction);
        LocalDate today = LocalDate.now();
        when(transactionRepository.listTodayByAccountId(10L, today, 0, 10)).thenReturn(transactions);
        when(transactionRepository.countTodayByAccountId(10L, today)).thenReturn(2L);

        // ACT
        PageResult<Transaction> result = transactionService.listTodayByAccountId(10L, 0, 10);

        // ASSERT
        assertNotNull(result);
        assertEquals(2, result.content().size());
        assertEquals(2L, result.totalElements());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        verify(transactionRepository, times(1)).listTodayByAccountId(eq(10L), any(LocalDate.class), eq(0), eq(10));
        verify(transactionRepository, times(1)).countTodayByAccountId(eq(10L), any(LocalDate.class));
    }

    @Test
    void listTodayByAccountId_deveRetornarListaVazia_quandoNaoHaTransacoesHoje() {
        // ARRANGE
        LocalDate today = LocalDate.now();
        when(transactionRepository.listTodayByAccountId(10L, today, 0, 10)).thenReturn(Collections.emptyList());
        when(transactionRepository.countTodayByAccountId(10L, today)).thenReturn(0L);

        // ACT
        PageResult<Transaction> result = transactionService.listTodayByAccountId(10L, 0, 10);

        // ASSERT
        assertNotNull(result);
        assertTrue(result.content().isEmpty());
        assertEquals(0L, result.totalElements());
        verify(transactionRepository, times(1)).listTodayByAccountId(eq(10L), any(LocalDate.class), eq(0), eq(10));
        verify(transactionRepository, times(1)).countTodayByAccountId(eq(10L), any(LocalDate.class));
    }

    // --- Testes para createDeposit ---

    @Test
    void createDeposit_deveCriarTransacaoDeDeposito_quandoDadosValidos() {
        // ARRANGE
        Long accountId = 10L;
        BigDecimal amount = BigDecimal.valueOf(100.00);
        // Mock do persist para não fazer nada, já que estamos testando a service
        doNothing().when(transactionRepository).persist(any(Transaction.class));
        // Não precisamos mockar o validator para sucesso, apenas para erros

        // Captura o argumento passado para persist
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        // ACT
        Transaction result = transactionService.createDeposit(accountId, amount);

        // ASSERT
        assertNotNull(result);
        assertEquals(TransactionType.DEPOSITO, result.getType());
        assertEquals(amount, result.getAmount());
        assertNull(result.getSourceAccountId());
        assertEquals(accountId, result.getDestinationAccountId());

        // VERIFY
        verify(transactionValidator, times(1)).validateTransactionConsistency(transactionCaptor.capture());
        verify(transactionRepository, times(1)).persist(transactionCaptor.getValue());

        // Verifica o objeto capturado
        Transaction capturedTransaction = transactionCaptor.getValue();
        assertEquals(TransactionType.DEPOSITO, capturedTransaction.getType());
        assertEquals(amount, capturedTransaction.getAmount());
        assertNull(capturedTransaction.getSourceAccountId());
        assertEquals(accountId, capturedTransaction.getDestinationAccountId());
        assertNotNull(capturedTransaction.getDateTime());
    }

    @Test
    void createDeposit_deveLancarBadRequestException_quandoValidacaoFalha() {
        // ARRANGE
        Long accountId = 10L;
        BigDecimal amount = BigDecimal.valueOf(100.00);
        doThrow(new BadRequestException("Erro de validação")).when(transactionValidator)
                .validateTransactionConsistency(any(Transaction.class));

        // ACT & ASSERT
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> transactionService.createDeposit(accountId, amount));
        assertEquals("Erro de validação", exception.getMessage());

        // VERIFY
        verify(transactionValidator, times(1)).validateTransactionConsistency(any(Transaction.class));
        verify(transactionRepository, never()).persist(any(Transaction.class));
    }

    // --- Testes para createWithdraw ---

    @Test
    void createWithdraw_deveCriarTransacaoDeSaque_quandoDadosValidos() {
        // ARRANGE
        Long accountId = 10L;
        BigDecimal amount = BigDecimal.valueOf(50.00);
        doNothing().when(transactionRepository).persist(any(Transaction.class));
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        // ACT
        Transaction result = transactionService.createWithdraw(accountId, amount);

        // ASSERT
        assertNotNull(result);
        assertEquals(TransactionType.SAQUE, result.getType());
        assertEquals(amount, result.getAmount());
        assertEquals(accountId, result.getSourceAccountId());
        assertNull(result.getDestinationAccountId());

        // VERIFY
        verify(transactionValidator, times(1)).validateTransactionConsistency(transactionCaptor.capture());
        verify(transactionRepository, times(1)).persist(transactionCaptor.getValue());

        Transaction capturedTransaction = transactionCaptor.getValue();
        assertEquals(TransactionType.SAQUE, capturedTransaction.getType());
        assertEquals(amount, capturedTransaction.getAmount());
        assertEquals(accountId, capturedTransaction.getSourceAccountId());
        assertNull(capturedTransaction.getDestinationAccountId());
        assertNotNull(capturedTransaction.getDateTime());
    }

    @Test
    void createWithdraw_deveLancarBadRequestException_quandoValidacaoFalha() {
        // ARRANGE
        Long accountId = 10L;
        BigDecimal amount = BigDecimal.valueOf(50.00);
        doThrow(new BadRequestException("Erro de validação")).when(transactionValidator)
                .validateTransactionConsistency(any(Transaction.class));

        // ACT & ASSERT
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> transactionService.createWithdraw(accountId, amount));
        assertEquals("Erro de validação", exception.getMessage());

        // VERIFY
        verify(transactionValidator, times(1)).validateTransactionConsistency(any(Transaction.class));
        verify(transactionRepository, never()).persist(any(Transaction.class));
    }

    // --- Testes para createTransfer ---

    @Test
    void createTransfer_deveCriarTransacaoDeTransferencia_quandoDadosValidos() {
        // ARRANGE
        Long sourceAccountId = 10L;
        Long destinationAccountId = 20L;
        BigDecimal amount = BigDecimal.valueOf(150.00);
        doNothing().when(transactionRepository).persist(any(Transaction.class));
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        // ACT
        Transaction result = transactionService.createTransfer(sourceAccountId, destinationAccountId, amount);

        // ASSERT
        assertNotNull(result);
        assertEquals(TransactionType.TRANSFERENCIA, result.getType());
        assertEquals(amount, result.getAmount());
        assertEquals(sourceAccountId, result.getSourceAccountId());
        assertEquals(destinationAccountId, result.getDestinationAccountId());

        // VERIFY
        verify(transactionValidator, times(1)).validateTransactionConsistency(transactionCaptor.capture());
        verify(transactionRepository, times(1)).persist(transactionCaptor.getValue());

        Transaction capturedTransaction = transactionCaptor.getValue();
        assertEquals(TransactionType.TRANSFERENCIA, capturedTransaction.getType());
        assertEquals(amount, capturedTransaction.getAmount());
        assertEquals(sourceAccountId, capturedTransaction.getSourceAccountId());
        assertEquals(destinationAccountId, capturedTransaction.getDestinationAccountId());
        assertNotNull(capturedTransaction.getDateTime());
    }

    @Test
    void createTransfer_deveLancarBadRequestException_quandoValidacaoFalha() {
        // ARRANGE
        Long sourceAccountId = 10L;
        Long destinationAccountId = 20L;
        BigDecimal amount = BigDecimal.valueOf(150.00);
        doThrow(new BadRequestException("Erro de validação")).when(transactionValidator)
                .validateTransactionConsistency(any(Transaction.class));

        // ACT & ASSERT
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> transactionService.createTransfer(sourceAccountId, destinationAccountId, amount));
        assertEquals("Erro de validação", exception.getMessage());

        // VERIFY
        verify(transactionValidator, times(1)).validateTransactionConsistency(any(Transaction.class));
        verify(transactionRepository, never()).persist(any(Transaction.class));
    }
}