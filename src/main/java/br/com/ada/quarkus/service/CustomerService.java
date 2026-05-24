package br.com.ada.quarkus.service;

import br.com.ada.quarkus.model.Customer;
import br.com.ada.quarkus.model.LoggedUser;
import br.com.ada.quarkus.model.UserRole;
import br.com.ada.quarkus.repository.CustomerRepository;
import br.com.ada.quarkus.util.PageResult;
import br.com.ada.quarkus.validator.CustomerValidator;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class CustomerService {

    @Inject
    CurrentUserService currentUserService;

    @Inject
    PasswordService passwordService;

    @Inject
    CustomerRepository customerRepository; // Novo: Injeta o CustomerRepository

    @Inject
    CustomerValidator customerValidator; // Novo: Injeta o CustomerValidator

    @Transactional
    public PageResult<Customer> list(int page, int size) {
        var query = customerRepository.findAllCustomers(Sort.by("id"));
        var result = query.page(io.quarkus.panache.common.Page.of(page, size));

        return new PageResult<>(result.list(), page, size, result.count());
    }

    @Transactional
    public Customer findById(Long id) {
        return getRequiredCustomer(id);
    }

    @Transactional
    public Customer findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);

        return customerRepository.findByEmailOptional(normalizedEmail)
                .orElseThrow(() -> new NotFoundException(
                        "Cliente com o email informado não foi encontrado"
                ));
    }

    @Transactional
    public Customer create(Customer customer) {
        customerValidator.validateUniqueCpf(customer.getCpf(), null);
        customerValidator.validateUniqueEmail(customer.getEmail(), null);

        Customer newCustomer = new Customer();
        newCustomer.setName(customer.getName());
        newCustomer.setCpf(customer.getCpf());
        newCustomer.setEmail(normalizeEmail(customer.getEmail()));
        newCustomer.setPassword(passwordService.hash(customer.getPassword()));
        newCustomer.setRole(UserRole.CUSTOMER);

        customerRepository.persist(newCustomer); // Usa o repositório para persistir

        return newCustomer;
    }

    @Transactional
    public Customer update(Long id, String name, String email, String password) {
        Customer existingCustomer = getRequiredCustomer(id);

        customerValidator.validateUniqueEmail(email, id); // Usa o validador

        existingCustomer.setName(name);
        existingCustomer.setEmail(normalizeEmail(email));
        existingCustomer.setPassword(passwordService.hash(password));

        customerRepository.persist(existingCustomer); // PanacheEntityBase.persist() ou customerRepository.persist()

        return existingCustomer;
    }

    public LoggedUser loggedUser() {
        return currentUserService.getLoggedUser();
    }

    private Customer getRequiredCustomer(Long id) {
        return customerRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado"));
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}