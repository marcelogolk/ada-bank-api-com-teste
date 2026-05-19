package br.com.ada.quarkus.service;

import br.com.ada.quarkus.model.PageResult;
import br.com.ada.quarkus.model.Transaction;
import br.com.ada.quarkus.model.TransactionType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
@ApplicationScoped
public class TransactionService {

    public Transaction findById(Long id) {
        return getRequiredTransaction(id);
    }

    public PageResult<Transaction> listByAccountId(Long accountId, int page, int size) {
        var query = Transaction.find(
                "sourceAccountId = ?1 OR destinationAccountId = ?1",
                Sort.by("dateTime").descending(),
                accountId
        );
        var result = query.page(Page.of(page, size));

        return new PageResult<>(result.list(), page, size, result.count());
    }
    public PageResult<Transaction> listTodayByAccountId(Long accountId, int page, int size) {
        LocalDate today = LocalDate.now();

        var query = PanacheEntityBase.find(
                "(sourceAccountId = ?1 OR destinationAccountId = ?1) AND CAST(dateTime AS DATE) = ?2",
                Sort.by("dateTime").descending(),
                accountId,
                today
        );
        var result = query.page(Page.of(page, size));

        return new PageResult<>(result.list(), page, size, result.count());
    }

    public Transaction createDeposit(Long accountId, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setType(TransactionType.DEPOSITO);
        transaction.setAmount(amount);
        transaction.setDateTime(LocalDateTime.now());
        transaction.setSourceAccountId(null);
        transaction.setDestinationAccountId(accountId);

        validateTransactionConsistency(transaction);
        transaction.persist();

        return transaction;
    }

    public Transaction createWithdraw(Long accountId, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setType(TransactionType.SAQUE);
        transaction.setAmount(amount);
        transaction.setDateTime(LocalDateTime.now());
        transaction.setSourceAccountId(accountId);
        transaction.setDestinationAccountId(null);

        validateTransactionConsistency(transaction);
        transaction.persist();

        return transaction;
    }

    public Transaction createTransfer(Long sourceAccountId, Long destinationAccountId, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setType(TransactionType.TRANSFERENCIA);
        transaction.setAmount(amount);
        transaction.setDateTime(LocalDateTime.now());
        transaction.setSourceAccountId(sourceAccountId);
        transaction.setDestinationAccountId(destinationAccountId);

        validateTransactionConsistency(transaction);
        transaction.persist();

        return transaction;
    }

    private Transaction getRequiredTransaction(Long id) {
        Transaction transaction = PanacheEntityBase.findById(id);

        if (transaction == null) {
            throw new NotFoundException("Transação não encontrada");
        }

        return transaction;
    }

    private void validateTransactionConsistency(Transaction transaction) {
        if (transaction.getType() == null) {
            throw new BadRequestException("O tipo da transação é obrigatório");
        }

        TransactionType type = transaction.getType();

        if (type == TransactionType.DEPOSITO) {
            if (transaction.getSourceAccountId() != null) {
                throw new BadRequestException(
                        "Transação do tipo DEPOSITO não deve possuir conta de origem"
                );
            }
            if (transaction.getDestinationAccountId() == null) {
                throw new BadRequestException(
                        "Transação do tipo DEPOSITO deve possuir conta de destino"
                );
            }
        } else if (type == TransactionType.SAQUE) {
            if (transaction.getSourceAccountId() == null) {
                throw new BadRequestException(
                        "Transação do tipo SAQUE deve possuir conta de origem"
                );
            }
            if (transaction.getDestinationAccountId() != null) {
                throw new BadRequestException(
                        "Transação do tipo SAQUE não deve possuir conta de destino"
                );
            }
        } else if (type == TransactionType.TRANSFERENCIA) {
            if (transaction.getSourceAccountId() == null) {
                throw new BadRequestException(
                        "Transação do tipo TRANSFERENCIA deve possuir conta de origem"
                );
            }
            if (transaction.getDestinationAccountId() == null) {
                throw new BadRequestException(
                        "Transação do tipo TRANSFERENCIA deve possuir conta de destino"
                );
            }
        }
    }
}