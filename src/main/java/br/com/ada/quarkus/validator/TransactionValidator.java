package br.com.ada.quarkus.validator;

import br.com.ada.quarkus.model.Transaction;
import br.com.ada.quarkus.model.TransactionType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;

@ApplicationScoped
public class TransactionValidator {

    public void validateTransactionConsistency(Transaction transaction) {
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