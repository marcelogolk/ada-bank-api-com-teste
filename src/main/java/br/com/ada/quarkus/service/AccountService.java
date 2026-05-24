package br.com.ada.quarkus.service;

import br.com.ada.quarkus.model.*;
import br.com.ada.quarkus.repository.AccountRepository;
import br.com.ada.quarkus.util.PageResult; // Import correto
import br.com.ada.quarkus.validator.AccountValidator;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class AccountService {

    @Inject
    CustomerService customerService;

    @Inject
    CurrentUserService currentUserService;

    @Inject
    TransactionService transactionService;

    @Inject
    AccountValidator accountValidator;

    @Inject
    EntityManager entityManager;

    @Inject
    AccountRepository accountRepository;

    private Account getRequiredAccount(Long accountId) {
        return accountRepository.findByIdOptional(accountId)
                .orElseThrow(() -> new NotFoundException("Conta não encontrada com ID: " + accountId));
    }

    @Transactional
    public PageResult<Account> list(Long customerId, int page, int size) {
        PanacheQuery<Account> query;
        if (customerId != null) {
            query = accountRepository.findByCustomerId(customerId);
        } else {
            query = accountRepository.findAll(Sort.by("id"));
        }

        List<Account> accounts = query.page(Page.of(page, size)).list();
        long totalElements = query.count();
        // int totalPages = query.pageCount(); // totalPages não é mais necessário para o construtor do record

        return new PageResult<>(accounts, page, size, totalElements); // Ajustado para o construtor do record
    }

    @Transactional
    public Account findById(Long id) {
        return getRequiredAccount(id);
    }

    @Transactional
    public Account create(Account account) {
        customerService.findById(account.getCustomerId());

        Long nextId = ((Number) entityManager
                .createNativeQuery("select nextval('account_id_seq')")
                .getSingleResult())
                .longValue();

        String baseNumber = String.format("%09d", nextId);
        int checkDigit = accountValidator.calculateCheckDigit(baseNumber);
        String fullAccountNumber = baseNumber + checkDigit;

        // Ajustado: removido 'balance' do INSERT nativo, pois não há coluna 'balance' na tabela
        entityManager
                .createNativeQuery("""
                    INSERT INTO account (id, account_number, type, customer_id)
                    VALUES (:id, :accountNumber, :type, :customerId)
                    """)
                .setParameter("id", nextId)
                .setParameter("accountNumber", fullAccountNumber)
                .setParameter("type", account.getType().name())
                .setParameter("customerId", account.getCustomerId())
                .executeUpdate();

        return getRequiredAccount(nextId);
    }

    @Transactional
    public Transaction deposit(Long accountId, BigDecimal amount) {
        Account account = getRequiredAccount(accountId);

        accountValidator.validateAmount(amount);
        accountValidator.validateElectronicAccountForDeposit(account);

        // REMOVIDO: account.deposit(amount); e accountRepository.persist(account);
        // O saldo é calculado via @Formula, não é atualizado diretamente na entidade.
        // A criação da transação é que impacta o saldo.
        return transactionService.createDeposit(accountId, amount);
    }

    @Transactional
    public Transaction withdraw(Long accountId, BigDecimal amount) {
        Account account = getRequiredAccount(accountId);

        accountValidator.checkAccountOwnership(account, currentUserService.getLoggedUser());
        accountValidator.validateAmount(amount);
        accountValidator.validateElectronicAccountForWithdraw(account);
        accountValidator.validateSufficientBalance(account, amount);

        // REMOVIDO: account.withdraw(amount); e accountRepository.persist(account);
        // O saldo é calculado via @Formula, não é atualizado diretamente na entidade.
        // A criação da transação é que impacta o saldo.
        return transactionService.createWithdraw(accountId, amount);
    }

    @Transactional
    public Transaction transfer(Long sourceAccountId, Long destinationAccountId, BigDecimal amount) {
        Account sourceAccount = getRequiredAccount(sourceAccountId);
        Account destinationAccount = getRequiredAccount(destinationAccountId);

        accountValidator.checkAccountOwnership(sourceAccount, currentUserService.getLoggedUser());
        accountValidator.validateAmount(amount);
        accountValidator.validateDifferentAccounts(sourceAccountId, destinationAccountId);
        accountValidator.validateSufficientBalance(sourceAccount, amount);

        // REMOVIDO: sourceAccount.withdraw(amount); destinationAccount.deposit(amount);
        // e accountRepository.persist(sourceAccount); accountRepository.persist(destinationAccount);
        // O saldo é calculado via @Formula, não é atualizado diretamente na entidade.
        // A criação da transação é que impacta o saldo.
        return transactionService.createTransfer(sourceAccountId, destinationAccountId, amount);
    }

    public LoggedUser loggedUser() {
        return currentUserService.getLoggedUser();
    }
}