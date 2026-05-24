package br.com.ada.quarkus.validator;

import br.com.ada.quarkus.model.Customer;
import br.com.ada.quarkus.repository.CustomerRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import java.util.Optional;

@ApplicationScoped
public class CustomerValidator {

    @Inject
    CustomerRepository customerRepository;

    public void validateUniqueCpf(String cpf, Long currentId) {
        Optional<Customer> existingCustomer = customerRepository.findByCpfOptional(cpf);

        if (existingCustomer.isPresent() && !existingCustomer.get().getId().equals(currentId)) {
            throw new BadRequestException("Já existe um cliente com o CPF informado");
        }
    }

    public void validateUniqueEmail(String email, Long currentId) {
        String normalizedEmail = normalizeEmail(email);
        Optional<Customer> existingCustomer = customerRepository.findByEmailOptional(normalizedEmail);

        if (existingCustomer.isPresent() && !existingCustomer.get().getId().equals(currentId)) {
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