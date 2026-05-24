package br.com.ada.quarkus.repository;

import br.com.ada.quarkus.model.Account;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase; // Use PanacheRepositoryBase
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.hibernate.orm.panache.PanacheQuery;

import java.util.Optional;

@ApplicationScoped
public class AccountRepository implements PanacheRepositoryBase<Account, Long> { // Implemente PanacheRepositoryBase com 2 argumentos
    // O método findByCustomerId que você tinha antes pode ser mantido,
    // mas não é um método padrão de PanacheRepositoryBase, então se você
    // quiser que ele funcione, ele precisa ser implementado aqui.
    // No entanto, para o erro atual, o foco é a assinatura da interface.

    // Se você quer um método customizado, ele ficaria assim:
    public PanacheQuery<Account> findByCustomerId(Long customerId) {
        return find("customerId", customerId);
    }

    // Se você estava usando findByIdOptional ou findAll diretamente no AccountService,
    // eles já são fornecidos por PanacheRepositoryBase.
}

//package br.com.ada.quarkus.repository;
//
//import br.com.ada.quarkus.model.Account;
//import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
//import io.quarkus.panache.common.Sort;
//import jakarta.enterprise.context.ApplicationScoped;
//import io.quarkus.hibernate.orm.panache.PanacheQuery;
//
//import java.util.Optional;
//
//@ApplicationScoped
//public interface AccountRepository extends PanacheRepositoryBase<Account, Long> {
//
//    default PanacheQuery<Account> findByCustomerId(Long customerId) {
//        return find("customerId", customerId);
//    }
//}