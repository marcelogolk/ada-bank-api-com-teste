package br.com.ada.quarkus.repository;

import br.com.ada.quarkus.model.Account;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.hibernate.orm.panache.PanacheQuery; // Importar PanacheQuery

import java.util.Optional;

/**
 * Interface de repositório para a entidade Account.
 * Estende PanacheRepositoryBase para herdar métodos de persistência do Panache.
 * Anotada com @ApplicationScoped para que o CDI possa gerenciá-la e injetá-la.
 */
@ApplicationScoped
public interface AccountRepository extends PanacheRepositoryBase<Account, Long> {

    /**
     * Encontra contas por customerId.
     * Este método é um exemplo de como adicionar métodos personalizados ao repositório.
     * Panache já fornece find("field", value), mas aqui demonstramos a flexibilidade.
     *
     * @param customerId O ID do cliente.
     * @return Uma PanacheQuery para contas associadas ao customerId.
     */
    default PanacheQuery<Account> findByCustomerId(Long customerId) {
        return find("customerId", customerId);
    }
}