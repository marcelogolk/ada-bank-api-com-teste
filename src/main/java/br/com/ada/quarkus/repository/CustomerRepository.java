package br.com.ada.quarkus.repository;

import br.com.ada.quarkus.model.Customer;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class CustomerRepository implements PanacheRepositoryBase<Customer, Long> {

    public Optional<Customer> findByEmailOptional(String email) {
        return find("email", email).firstResultOptional();
    }

    public Optional<Customer> findByCpfOptional(String cpf) {
        return find("cpf", cpf).firstResultOptional();
    }

    public PanacheQuery<Customer> findAllCustomers(Sort sort) {
        return findAll(sort);
    }
}