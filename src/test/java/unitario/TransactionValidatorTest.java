package unitario;

import br.com.ada.quarkus.model.Transaction;
import br.com.ada.quarkus.model.TransactionType;
import br.com.ada.quarkus.validator.TransactionValidator;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TransactionValidatorTest {

    @Inject
    TransactionValidator transactionValidator;

    private Transaction baseTransaction;

    @BeforeEach
    void setUp() {
        baseTransaction = new Transaction(
                null, // ID será gerado pelo banco, não relevante para validação de consistência
                null, // Tipo será definido em cada teste
                BigDecimal.valueOf(100.00),
                LocalDateTime.now(),
                null, // SourceAccountId será definido em cada teste
                null  // DestinationAccountId será definido em cada teste
        );
    }

    // --- Testes para DEPOSITO ---

    @Test
    void validateTransactionConsistency_devePassarParaDepositoValido() {
        // ARRANGE
        baseTransaction.setType(TransactionType.DEPOSITO);
        baseTransaction.setDestinationAccountId(1L);
        baseTransaction.setSourceAccountId(null); // Depósito não deve ter conta de origem

        // ACT & ASSERT
        assertDoesNotThrow(() -> transactionValidator.validateTransactionConsistency(baseTransaction));
    }

    @Test
    void validateTransactionConsistency_deveLancarBadRequestException_paraDepositoComSourceAccount() {
        // ARRANGE
        baseTransaction.setType(TransactionType.DEPOSITO);
        baseTransaction.setDestinationAccountId(1L);
        baseTransaction.setSourceAccountId(2L); // ERRO: Depósito com conta de origem

        // ACT & ASSERT
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> transactionValidator.validateTransactionConsistency(baseTransaction));
        assertEquals("Transação do tipo DEPOSITO não deve possuir conta de origem", exception.getMessage());
    }

    @Test
    void validateTransactionConsistency_deveLancarBadRequestException_paraDepositoSemDestinationAccount() {
        // ARRANGE
        baseTransaction.setType(TransactionType.DEPOSITO);
        baseTransaction.setDestinationAccountId(null); // ERRO: Depósito sem conta de destino
        baseTransaction.setSourceAccountId(null);

        // ACT & ASSERT
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> transactionValidator.validateTransactionConsistency(baseTransaction));
        assertEquals("Transação do tipo DEPOSITO deve possuir conta de destino", exception.getMessage());
    }

    // --- Testes para SAQUE ---

    @Test
    void validateTransactionConsistency_devePassarParaSaqueValido() {
        // ARRANGE
        baseTransaction.setType(TransactionType.SAQUE);
        baseTransaction.setSourceAccountId(1L);
        baseTransaction.setDestinationAccountId(null); // Saque não deve ter conta de destino

        // ACT & ASSERT
        assertDoesNotThrow(() -> transactionValidator.validateTransactionConsistency(baseTransaction));
    }

    @Test
    void validateTransactionConsistency_deveLancarBadRequestException_paraSaqueSemSourceAccount() {
        // ARRANGE
        baseTransaction.setType(TransactionType.SAQUE);
        baseTransaction.setSourceAccountId(null); // ERRO: Saque sem conta de origem
        baseTransaction.setDestinationAccountId(null);

        // ACT & ASSERT
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> transactionValidator.validateTransactionConsistency(baseTransaction));
        assertEquals("Transação do tipo SAQUE deve possuir conta de origem", exception.getMessage());
    }

    @Test
    void validateTransactionConsistency_deveLancarBadRequestException_paraSaqueComDestinationAccount() {
        // ARRANGE
        baseTransaction.setType(TransactionType.SAQUE);
        baseTransaction.setSourceAccountId(1L);
        baseTransaction.setDestinationAccountId(2L); // ERRO: Saque com conta de destino

        // ACT & ASSERT
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> transactionValidator.validateTransactionConsistency(baseTransaction));
        assertEquals("Transação do tipo SAQUE não deve possuir conta de destino", exception.getMessage());
    }

    // --- Testes para TRANSFERENCIA ---

    @Test
    void validateTransactionConsistency_devePassarParaTransferenciaValida() {
        // ARRANGE
        baseTransaction.setType(TransactionType.TRANSFERENCIA);
        baseTransaction.setSourceAccountId(1L);
        baseTransaction.setDestinationAccountId(2L);

        // ACT & ASSERT
        assertDoesNotThrow(() -> transactionValidator.validateTransactionConsistency(baseTransaction));
    }

    @Test
    void validateTransactionConsistency_deveLancarBadRequestException_paraTransferenciaSemSourceAccount() {
        // ARRANGE
        baseTransaction.setType(TransactionType.TRANSFERENCIA);
        baseTransaction.setSourceAccountId(null); // ERRO: Transferência sem conta de origem
        baseTransaction.setDestinationAccountId(2L);

        // ACT & ASSERT
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> transactionValidator.validateTransactionConsistency(baseTransaction));
        assertEquals("Transação do tipo TRANSFERENCIA deve possuir conta de origem", exception.getMessage());
    }

    @Test
    void validateTransactionConsistency_deveLancarBadRequestException_paraTransferenciaSemDestinationAccount() {
        // ARRANGE
        baseTransaction.setType(TransactionType.TRANSFERENCIA);
        baseTransaction.setSourceAccountId(1L);
        baseTransaction.setDestinationAccountId(null); // ERRO: Transferência sem conta de destino

        // ACT & ASSERT
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> transactionValidator.validateTransactionConsistency(baseTransaction));
        assertEquals("Transação do tipo TRANSFERENCIA deve possuir conta de destino", exception.getMessage());
    }

    // --- Teste para tipo de transação nulo ---

    @Test
    void validateTransactionConsistency_deveLancarBadRequestException_paraTipoNulo() {
        // ARRANGE
        baseTransaction.setType(null); // ERRO: Tipo de transação nulo

        // ACT & ASSERT
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> transactionValidator.validateTransactionConsistency(baseTransaction));
        assertEquals("O tipo da transação é obrigatório", exception.getMessage());
    }
}