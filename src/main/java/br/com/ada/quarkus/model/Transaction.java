package br.com.ada.quarkus.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
@Entity
@Table(name = "bank_transaction")
public class Transaction extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "O tipo da transação é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 25)
    private TransactionType type;

    @NotNull(message = "O valor da transação é obrigatório")
    @DecimalMin(value = "0.01", message = "O valor da transação deve ser no mínimo 0,01")
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "A data e hora da transação é obrigatória")
    @Column(name = "date_time", nullable = false)
    private LocalDateTime dateTime;

    @Column(name = "source_account_id")
    private Long sourceAccountId;

    @Column(name = "destination_account_id")
    private Long destinationAccountId;

    public Transaction() {
    }

    public Transaction(Long id, TransactionType type, BigDecimal amount, LocalDateTime dateTime,
                       Long sourceAccountId, Long destinationAccountId) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.dateTime = dateTime;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public Long getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(Long sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public Long getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(Long destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", type=" + type +
                ", amount=" + amount +
                ", dateTime=" + dateTime +
                ", sourceAccountId=" + sourceAccountId +
                ", destinationAccountId=" + destinationAccountId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}