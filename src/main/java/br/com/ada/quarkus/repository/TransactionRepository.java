package br.com.ada.quarkus.repository;

import br.com.ada.quarkus.model.Transaction;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TransactionRepository implements PanacheRepositoryBase<Transaction, Long> {

    public Optional<Transaction> findByIdOptional(Long id) {
        return find("id", id).firstResultOptional();
    }

    public List<Transaction> listByAccountId(Long accountId, int page, int size) {
        return find(
                "sourceAccountId = ?1 OR destinationAccountId = ?1",
                Sort.by("dateTime").descending(),
                accountId
        ).page(Page.of(page, size)).list();
    }

    public long countByAccountId(Long accountId) {
        return find(
                "sourceAccountId = ?1 OR destinationAccountId = ?1",
                accountId
        ).count();
    }

    public List<Transaction> listTodayByAccountId(Long accountId, LocalDate today, int page, int size) {
        return find(
                "(sourceAccountId = ?1 OR destinationAccountId = ?1) AND CAST(dateTime AS DATE) = ?2",
                Sort.by("dateTime").descending(),
                accountId,
                today
        ).page(Page.of(page, size)).list();
    }

    public long countTodayByAccountId(Long accountId, LocalDate today) {
        return find(
                "(sourceAccountId = ?1 OR destinationAccountId = ?1) AND CAST(dateTime AS DATE) = ?2",
                accountId,
                today
        ).count();
    }
}