package br.com.ada.quarkus.service;

import br.com.ada.quarkus.model.Customer;
import br.com.ada.quarkus.model.LoggedUser;
import br.com.ada.quarkus.model.PageResult;
import br.com.ada.quarkus.model.UserRole;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;


@ApplicationScoped
public class CustomerService {

    @Inject
    CurrentUserService currentUserService;

    @Inject
    PasswordService passwordService;

    public PageResult<Customer> list(int page, int size) {
        var query = Customer.findAll(Sort.by("id"));
        var result = query.page(Page.of(page, size));

        return new PageResult<>(result.list(), page, size, result.count());
    }

    public Customer findById(Long id) {
        return getRequiredCustomer(id);
    }

    public Customer findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);

        return (Customer) Customer.find("email", normalizedEmail)
                .firstResultOptional()
                .orElseThrow(() -> new NotFoundException(
                        "Cliente com o email informado não foi encontrado"
                ));
    }

    public Customer create(Customer customer) {
        validateUniqueCpf(customer.getCpf(), null);
        validateUniqueEmail(customer.getEmail(), null);

        Customer newCustomer = new Customer();
        newCustomer.setName(customer.getName());
        newCustomer.setCpf(customer.getCpf());
        newCustomer.setEmail(normalizeEmail(customer.getEmail()));
        newCustomer.setPassword(passwordService.hash(customer.getPassword()));
        newCustomer.setRole(UserRole.CUSTOMER);

        newCustomer.persist();

        return newCustomer;
    }

    public Customer update(Long id, String name, String email, String password) {
        Customer existingCustomer = getRequiredCustomer(id);

        validateUniqueEmail(email, id);

        existingCustomer.setName(name);
        existingCustomer.setEmail(normalizeEmail(email));
        existingCustomer.setPassword(passwordService.hash(password));

        return existingCustomer;
    }

    public LoggedUser loggedUser() {
        return currentUserService.getLoggedUser();
    }

    private Customer getRequiredCustomer(Long id) {
        Customer customer = Customer.findById(id);

        if (customer == null) {
            throw new NotFoundException("Cliente não encontrado");
        }

        return customer;
    }

    private void validateUniqueCpf(String cpf, Long currentId) {
        Customer existingCustomer = Customer.find("cpf", cpf).firstResult();

        if (existingCustomer != null && !existingCustomer.getId().equals(currentId)) {
            throw new BadRequestException("Já existe um cliente com o CPF informado");
        }
    }

    private void validateUniqueEmail(String email, Long currentId) {
        String normalizedEmail = normalizeEmail(email);
        Customer existingCustomer = Customer.find("email", normalizedEmail).firstResult();

        if (existingCustomer != null && !existingCustomer.getId().equals(currentId)) {
            throw new BadRequestException("Já existe um cliente com o email informado");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }

        return email.trim().toLowerCase();
    }
}