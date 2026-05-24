package unitario;

import br.com.ada.quarkus.model.Customer;
import br.com.ada.quarkus.repository.CustomerRepository;
import br.com.ada.quarkus.validator.CustomerValidator;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class CustomerValidatorTest {

    @Inject
    CustomerValidator customerValidator;

    @InjectMock
    CustomerRepository customerRepository;

    // --- Testes para validateUniqueCpf ---

    @Test
    void validateUniqueCpf_devePermitirCpfUnico_quandoNaoExisteOutroCliente() {
        // ARRANGE
        String cpf = "12345678901";
        Long currentId = null; // Criando novo cliente
        when(customerRepository.findByCpfOptional(cpf)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertDoesNotThrow(() -> customerValidator.validateUniqueCpf(cpf, currentId));

        // VERIFY
        verify(customerRepository, times(1)).findByCpfOptional(cpf);
    }

    @Test
    void validateUniqueCpf_devePermitirCpfExistente_quandoEhDoProprioCliente() {
        // ARRANGE
        String cpf = "12345678901";
        Long currentId = 1L; // Atualizando cliente existente
        Customer existingCustomer = new Customer(currentId, "Nome", cpf, "email@test.com", "senha");
        when(customerRepository.findByCpfOptional(cpf)).thenReturn(Optional.of(existingCustomer));

        // ACT & ASSERT
        assertDoesNotThrow(() -> customerValidator.validateUniqueCpf(cpf, currentId));

        // VERIFY
        verify(customerRepository, times(1)).findByCpfOptional(cpf);
    }

    @Test
    void validateUniqueCpf_deveLancarBadRequestException_quandoCpfJaExisteParaOutroCliente() {
        // ARRANGE
        String cpf = "12345678901";
        Long currentId = 2L; // Atualizando cliente diferente
        Customer existingCustomer = new Customer(1L, "Nome", cpf, "email@test.com", "senha");
        when(customerRepository.findByCpfOptional(cpf)).thenReturn(Optional.of(existingCustomer));

        // ACT & ASSERT
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> customerValidator.validateUniqueCpf(cpf, currentId));
        assertEquals("Já existe um cliente com o CPF informado", exception.getMessage());

        // VERIFY
        verify(customerRepository, times(1)).findByCpfOptional(cpf);
    }

    // --- Testes para validateUniqueEmail ---

    @Test
    void validateUniqueEmail_devePermitirEmailUnico_quandoNaoExisteOutroCliente() {
        // ARRANGE
        String email = "novo@email.com";
        Long currentId = null; // Criando novo cliente
        when(customerRepository.findByEmailOptional(email)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertDoesNotThrow(() -> customerValidator.validateUniqueEmail(email, currentId));

        // VERIFY
        verify(customerRepository, times(1)).findByEmailOptional(email);
    }

    @Test
    void validateUniqueEmail_devePermitirEmailExistente_quandoEhDoProprioCliente() {
        // ARRANGE
        String email = "existente@email.com";
        Long currentId = 1L; // Atualizando cliente existente
        Customer existingCustomer = new Customer(currentId, "Nome", "11122233344", email, "senha");
        when(customerRepository.findByEmailOptional(email)).thenReturn(Optional.of(existingCustomer));

        // ACT & ASSERT
        assertDoesNotThrow(() -> customerValidator.validateUniqueEmail(email, currentId));

        // VERIFY
        verify(customerRepository, times(1)).findByEmailOptional(email);
    }

    @Test
    void validateUniqueEmail_deveLancarBadRequestException_quandoEmailJaExisteParaOutroCliente() {
        // ARRANGE
        String email = "existente@email.com";
        Long currentId = 2L; // Atualizando cliente diferente
        Customer existingCustomer = new Customer(1L, "Nome", "11122233344", email, "senha");
        when(customerRepository.findByEmailOptional(email)).thenReturn(Optional.of(existingCustomer));

        // ACT & ASSERT
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> customerValidator.validateUniqueEmail(email, currentId));
        assertEquals("Já existe um cliente com o email informado", exception.getMessage());

        // VERIFY
        verify(customerRepository, times(1)).findByEmailOptional(email);
    }
}