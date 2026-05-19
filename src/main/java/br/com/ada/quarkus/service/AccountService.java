package br.com.ada.quarkus.service;

import br.com.ada.quarkus.model.*;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.EntityManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import java.math.BigDecimal;



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

    public PageResult<Account> list(Long customerId, int page, int size) {
        var query = (customerId != null)
                ? Account.find("customerId", customerId)   // ← classe FILHA
                : Account.findAll(Sort.by("id"));          // ← classe FILHA

        var result = query.page(Page.of(page, size));

        return new PageResult<>(result.list(), page, size, result.count());
    }


        public Account findById(Long id) {
        return getRequiredAccount(id);
    }

//    public Account create(Account account) {
//        validateCustomerExists(account.getCustomerId());
//
//        Long nextId = ((Number) PanacheEntityBase.getEntityManager()
//                .createNativeQuery("select nextval('account_id_seq')")
//                .getSingleResult())
//                .longValue();
//
//        String baseNumber = String.format("%09d", nextId);
//        int checkDigit = accountValidator.calculateCheckDigit(baseNumber);
//        String fullAccountNumber = baseNumber + checkDigit;
//
//        PanacheEntityBase.getEntityManager()
//                .createNativeQuery("""
//                    INSERT INTO account (id, account_number, type, customer_id)
//                    VALUES (:id, :accountNumber, :type, :customerId)
//                    """)
//                .setParameter("id", nextId)
//                .setParameter("accountNumber", fullAccountNumber)
//                .setParameter("type", account.getType().name())
//                .setParameter("customerId", account.getCustomerId())
//                .executeUpdate();
//
//        return getRequiredAccount(nextId);
//    }

    public Account create(Account account) {
        validateCustomerExists(account.getCustomerId());
        Long nextId = ((Number) entityManager // &lt;-- Mude aqui de PanacheEntityBase para entityManager
                .createNativeQuery("select nextval('account_id_seq')")
                .getSingleResult())
                .longValue();

        String baseNumber = String.format("%09d", nextId);
        int checkDigit = accountValidator.calculateCheckDigit(baseNumber);
        String fullAccountNumber = baseNumber + checkDigit;

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


    public Transaction deposit(Long accountId, BigDecimal amount) {
        Account account = getRequiredAccount(accountId);

        accountValidator.validateAmount(amount);
        accountValidator.validateElectronicAccountForDeposit(account);

        return transactionService.createDeposit(accountId, amount);
    }

    public Transaction withdraw(Long accountId, BigDecimal amount) {
        Account account = getRequiredAccount(accountId);
        LoggedUser currentUser = currentUserService.getLoggedUser();

        accountValidator.checkAccountOwnership(account, currentUser);
        accountValidator.validateAmount(amount);
        accountValidator.validateElectronicAccountForWithdraw(account);
        accountValidator.validateSufficientBalance(account, amount);

        return transactionService.createWithdraw(accountId, amount);
    }

    public Transaction transfer(Long sourceAccountId, Long destinationAccountId, BigDecimal amount) {
        Account sourceAccount = getRequiredAccount(sourceAccountId);
        getRequiredAccount(destinationAccountId);
        LoggedUser currentUser = currentUserService.getLoggedUser();

        accountValidator.checkAccountOwnership(sourceAccount, currentUser);
        accountValidator.validateAmount(amount);
        accountValidator.validateDifferentAccounts(sourceAccountId, destinationAccountId);
        accountValidator.validateSufficientBalance(sourceAccount, amount);

        return transactionService.createTransfer(sourceAccountId, destinationAccountId, amount);
    }

    public LoggedUser loggedUser() {
        return currentUserService.getLoggedUser();
    }


private Account getRequiredAccount(Long id) {
    Account account = Account.findById(id);  // ← classe FILHA
    if (account == null) {
        throw new NotFoundException("Conta não encontrada");
    }
    return account;
}
    private void validateCustomerExists(Long customerId) {
        customerService.findById(customerId);
    }
}