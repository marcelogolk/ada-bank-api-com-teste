package br.com.ada.quarkus.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "account")
public class Account extends PanacheEntityBase {

    @Id
    @SequenceGenerator(
            name = "account_seq",
            sequenceName = "account_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_seq")
    private Long id;

    @Pattern(
            regexp = "\\d{10}",
            message = "O número da conta deve conter exatamente 10 dígitos (9 base + 1 verificador)"
    )
    @Column(name = "account_number", nullable = false, unique = true, length = 10)
    private String accountNumber;

    @NotNull(message = "O tipo da conta é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AccountType type;

    @NotNull(message = "O cliente da conta é obrigatório")
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Formula("(SELECT COALESCE(SUM(" +
            "CASE " +
            "WHEN t.type = 'DEPOSITO' AND t.destination_account_id = id THEN t.amount " +
            "WHEN t.type = 'SAQUE' AND t.source_account_id = id THEN -t.amount " +
            "WHEN t.type = 'TRANSFERENCIA' AND t.destination_account_id = id THEN t.amount " +
            "WHEN t.type = 'TRANSFERENCIA' AND t.source_account_id = id THEN -t.amount " +
            "ELSE 0 END" +
            "), 0) FROM bank_transaction t WHERE t.source_account_id = id OR t.destination_account_id = id)")
    private BigDecimal balance;

    public Account() {
    }

    public Account(Long id, String accountNumber, AccountType type, Long customerId) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.type = type;
        this.customerId = customerId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public AccountType getType() {
        return type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", accountNumber='" + accountNumber + '\'' +
                ", type=" + type +
                ", customerId=" + customerId +
                ", balance=" + balance +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Account account = (Account) o;
        return Objects.equals(id, account.id) &&
                Objects.equals(accountNumber, account.accountNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, accountNumber);
    }
}