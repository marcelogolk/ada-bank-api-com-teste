package br.com.ada.quarkus.service;

import br.com.ada.quarkus.util.PageResult;
import br.com.ada.quarkus.model.Transaction;
import br.com.ada.quarkus.model.TransactionType;
import br.com.ada.quarkus.repository.TransactionRepository;
import br.com.ada.quarkus.validator.TransactionValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@ApplicationScoped
public class TransactionService {

    @Inject
    TransactionRepository transactionRepository;

    @Inject
    TransactionValidator transactionValidator;

    public Transaction findById(Long id) {
        return getRequiredTransaction(id);
    }

    public PageResult<Transaction> listByAccountId(Long accountId, int page, int size) {
        var result = transactionRepository.listByAccountId(accountId, page, size);
        long totalElements = transactionRepository.countByAccountId(accountId);
        return new PageResult<>(result, page, size, totalElements);
    }

    public PageResult<Transaction> listTodayByAccountId(Long accountId, int page, int size) {
        LocalDate today = LocalDate.now();
        var result = transactionRepository.listTodayByAccountId(accountId, today, page, size);
        long totalElements = transactionRepository.countTodayByAccountId(accountId, today);
        return new PageResult<>(result, page, size, totalElements);
    }

    @Transactional
    public Transaction createDeposit(Long accountId, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setType(TransactionType.DEPOSITO);
        transaction.setAmount(amount);
        transaction.setDateTime(LocalDateTime.now());
        transaction.setSourceAccountId(null);
        transaction.setDestinationAccountId(accountId);

        transactionValidator.validateTransactionConsistency(transaction);
        transactionRepository.persist(transaction);

        return transaction;
    }

    @Transactional
    public Transaction createWithdraw(Long accountId, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setType(TransactionType.SAQUE);
        transaction.setAmount(amount);
        transaction.setDateTime(LocalDateTime.now());
        transaction.setSourceAccountId(accountId);
        transaction.setDestinationAccountId(null);

        transactionValidator.validateTransactionConsistency(transaction);
        transactionRepository.persist(transaction);

        return transaction;
    }

    @Transactional
    public Transaction createTransfer(Long sourceAccountId, Long destinationAccountId, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setType(TransactionType.TRANSFERENCIA);
        transaction.setAmount(amount);
        transaction.setDateTime(LocalDateTime.now());
        transaction.setSourceAccountId(sourceAccountId);
        transaction.setDestinationAccountId(destinationAccountId);

        transactionValidator.validateTransactionConsistency(transaction);
        transactionRepository.persist(transaction);

        return transaction;
    }

    private Transaction getRequiredTransaction(Long id) {
        return transactionRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Transação não encontrada"));
    }
}