package unitario;

import br.com.ada.quarkus.model.Customer;
import br.com.ada.quarkus.model.LoggedUser;
import br.com.ada.quarkus.model.UserRole;
import br.com.ada.quarkus.repository.CustomerRepository;
import br.com.ada.quarkus.service.CurrentUserService;
import br.com.ada.quarkus.service.CustomerService;
import br.com.ada.quarkus.service.PasswordService;
import br.com.ada.quarkus.util.PageResult;
import br.com.ada.quarkus.validator.CustomerValidator;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class CustomerServiceTest {

    @Inject
    CustomerService customerService;

    @InjectMock
    CustomerRepository customerRepository;

    @InjectMock
    CurrentUserService currentUserService;

    @InjectMock
    PasswordService passwordService;

    @InjectMock
    CustomerValidator customerValidator;

    private Customer customer1;
    private Customer customer2;
    private LoggedUser loggedCustomer;
    private LoggedUser loggedManager;

    @BeforeEach
    void setUp() {
        customer1 = new Customer(1L, "Alice Silva", "11122233344", "alice@email.com", "hashedPassword1");
        customer1.setRole(UserRole.CUSTOMER);

        customer2 = new Customer(2L, "Bob Lima", "55566677788", "bob@email.com", "hashedPassword2");
        customer2.setRole(UserRole.CUSTOMER);

        loggedCustomer = new LoggedUser(1L, "alice@email.com", "11122233344", "CLIENTE");
        loggedManager = new LoggedUser(10L, "manager@email.com", "99988877766", "GERENTE");

        // Reset mocks para cada teste
        Mockito.reset(customerRepository, currentUserService, passwordService, customerValidator);
    }

    // --- Testes para list ---

//    @Test
//    void list_deveRetornarListaDeClientesPaginada() {
//        // ARRANGE
//        int page = 0;
//        int size = 10;
//        List<Customer> customers = Arrays.asList(customer1, customer2);
//        PanacheQuery<Customer> panacheQuery = mock(PanacheQuery.class);
//
//        when(customerRepository.findAllCustomers(any(Sort.class))).thenReturn(panacheQuery);
//        when(panacheQuery.page(Page.of(page, size))).thenReturn(panacheQuery);
//        when(panacheQuery.list()).thenReturn(customers);
//        when(panacheQuery.count()).thenReturn((long) customers.size());
//
//        // ACT
//        PageResult<Customer> result = customerService.list(page, size);
//
//        // ASSERT
//        assertNotNull(result);
//        assertEquals(2, result.content().size());
//        assertEquals(customer1, result.content().get(0));
//        assertEquals(customer2, result.content().get(1));
//        assertEquals(page, result.page());
//        assertEquals(size, result.size());
//        assertEquals(2L, result.totalElements());
//
//        // VERIFY
//        verify(customerRepository, times(1)).findAllCustomers(any(Sort.class));
//        verify(panacheQuery, times(1)).page(Page.of(page, size));
//        verify(panacheQuery, times(1)).list();
//        verify(panacheQuery, times(1)).count();
//    }

//    @Test
//    void list_deveRetornarListaVazia_quandoNaoHaClientes() {
//        // ARRANGE
//        int page = 0;
//        int size = 10;
//        List<Customer> customers = Collections.emptyList();
//        PanacheQuery<Customer> panacheQuery = mock(PanacheQuery.class);
//
//        when(customerRepository.findAllCustomers(any(Sort.class))).thenReturn(panacheQuery);
//        when(panacheQuery.page(Page.of(page, size))).thenReturn(panacheQuery);
//        when(panacheQuery.list()).thenReturn(customers);
//        when(panacheQuery.count()).thenReturn(0L);
//
//        // ACT
//        PageResult<Customer> result = customerService.list(page, size);
//
//        // ASSERT
//        assertNotNull(result);
//        assertTrue(result.content().isEmpty());
//        assertEquals(0L, result.totalElements());
//
//        // VERIFY
//        verify(customerRepository, times(1)).findAllCustomers(any(Sort.class));
//        verify(panacheQuery, times(1)).page(Page.of(page, size));
//        verify(panacheQuery, times(1)).list();
//        verify(panacheQuery, times(1)).count();
//    }

    // --- Testes para findById ---

    @Test
    void findById_deveRetornarCliente_quandoExiste() {
        // ARRANGE
        when(customerRepository.findByIdOptional(1L)).thenReturn(Optional.of(customer1));

        // ACT
        Customer result = customerService.findById(1L);

        // ASSERT
        assertNotNull(result);
        assertEquals(customer1.getId(), result.getId());
        assertEquals(customer1.getName(), result.getName());

        // VERIFY
        verify(customerRepository, times(1)).findByIdOptional(1L);
    }

    @Test
    void findById_deveLancarNotFoundException_quandoNaoExiste() {
        // ARRANGE
        when(customerRepository.findByIdOptional(99L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> customerService.findById(99L));
        assertEquals("Cliente não encontrado", exception.getMessage());

        // VERIFY
        verify(customerRepository, times(1)).findByIdOptional(99L);
    }

    // --- Testes para findByEmail ---

    @Test
    void findByEmail_deveRetornarCliente_quandoExiste() {
        // ARRANGE
        String email = "alice@email.com";
        when(customerRepository.findByEmailOptional(email)).thenReturn(Optional.of(customer1));

        // ACT
        Customer result = customerService.findByEmail(email);

        // ASSERT
        assertNotNull(result);
        assertEquals(customer1.getEmail(), result.getEmail());

        // VERIFY
        verify(customerRepository, times(1)).findByEmailOptional(email);
    }

    @Test
    void findByEmail_deveLancarNotFoundException_quandoNaoExiste() {
        // ARRANGE
        String email = "naoexiste@email.com";
        when(customerRepository.findByEmailOptional(email)).thenReturn(Optional.empty());

        // ACT & ASSERT
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> customerService.findByEmail(email));
        assertEquals("Cliente com o email informado não foi encontrado", exception.getMessage());

        // VERIFY
        verify(customerRepository, times(1)).findByEmailOptional(email);
    }

    // --- Testes para create ---

    @Test
    void create_deveCriarNovoClienteComSucesso() {
        // ARRANGE
        Customer newCustomerData = new Customer(null, "Carlos", "12345678900", "carlos@email.com", "senha123");
        String hashedPassword = "hashedPasswordCarlos";

        doNothing().when(customerValidator).validateUniqueCpf(newCustomerData.getCpf(), null);
        doNothing().when(customerValidator).validateUniqueEmail(newCustomerData.getEmail(), null);
        when(passwordService.hash(newCustomerData.getPassword())).thenReturn(hashedPassword);
        doNothing().when(customerRepository).persist(any(Customer.class));

        // ACT
        Customer createdCustomer = customerService.create(newCustomerData);

        // ASSERT
        assertNotNull(createdCustomer);
        assertEquals(newCustomerData.getName(), createdCustomer.getName());
        assertEquals(newCustomerData.getCpf(), createdCustomer.getCpf());
        assertEquals(newCustomerData.getEmail(), createdCustomer.getEmail());
        assertEquals(hashedPassword, createdCustomer.getPassword());
        assertEquals(UserRole.CUSTOMER, createdCustomer.getRole());

        // VERIFY
        verify(customerValidator, times(1)).validateUniqueCpf(newCustomerData.getCpf(), null);
        verify(customerValidator, times(1)).validateUniqueEmail(newCustomerData.getEmail(), null);
        verify(passwordService, times(1)).hash(newCustomerData.getPassword());
        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository, times(1)).persist(customerCaptor.capture());
        Customer capturedCustomer = customerCaptor.getValue();
        assertEquals(newCustomerData.getName(), capturedCustomer.getName());
        assertEquals(newCustomerData.getCpf(), capturedCustomer.getCpf());
        assertEquals(newCustomerData.getEmail(), capturedCustomer.getEmail());
        assertEquals(hashedPassword, capturedCustomer.getPassword());
        assertEquals(UserRole.CUSTOMER, capturedCustomer.getRole());
    }

    @Test
    void create_deveLancarBadRequestException_quandoCpfDuplicado() {
        // ARRANGE
        Customer newCustomerData = new Customer(null, "Carlos", "11122233344", "carlos@email.com", "senha123");
        doThrow(new BadRequestException("Já existe um cliente com o CPF informado"))
                .when(customerValidator).validateUniqueCpf(newCustomerData.getCpf(), null);

        // ACT & ASSERT
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> customerService.create(newCustomerData));
        assertEquals("Já existe um cliente com o CPF informado", exception.getMessage());

        // VERIFY
        verify(customerValidator, times(1)).validateUniqueCpf(newCustomerData.getCpf(), null);
        verify(customerValidator, never()).validateUniqueEmail(anyString(), anyLong());
        verify(passwordService, never()).hash(anyString());
        verify(customerRepository, never()).persist(any(Customer.class));
    }

    @Test
    void create_deveLancarBadRequestException_quandoEmailDuplicado() {
        // ARRANGE
        Customer newCustomerData = new Customer(null, "Carlos", "12345678900", "alice@email.com", "senha123");
        doNothing().when(customerValidator).validateUniqueCpf(newCustomerData.getCpf(), null);
        doThrow(new BadRequestException("Já existe um cliente com o email informado"))
                .when(customerValidator).validateUniqueEmail(newCustomerData.getEmail(), null);

        // ACT & ASSERT
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> customerService.create(newCustomerData));
        assertEquals("Já existe um cliente com o email informado", exception.getMessage());

        // VERIFY
        verify(customerValidator, times(1)).validateUniqueCpf(newCustomerData.getCpf(), null);
        verify(customerValidator, times(1)).validateUniqueEmail(newCustomerData.getEmail(), null);
        verify(passwordService, never()).hash(anyString());
        verify(customerRepository, never()).persist(any(Customer.class));
    }

    // --- Testes para update ---

    @Test
    void update_deveAtualizarClienteComSucesso() {
        // ARRANGE
        Long customerId = 1L;
        String newName = "Alice Nova Silva";
        String newEmail = "alice.nova@email.com";
        String newPassword = "newHashedPassword";

        when(customerRepository.findByIdOptional(customerId)).thenReturn(Optional.of(customer1));
        doNothing().when(customerValidator).validateUniqueEmail(newEmail, customerId);
        when(passwordService.hash("newPassword")).thenReturn(newPassword);
        doNothing().when(customerRepository).persist(any(Customer.class));

        // ACT
        Customer updatedCustomer = customerService.update(customerId, newName, newEmail, "newPassword");

        // ASSERT
        assertNotNull(updatedCustomer);
        assertEquals(customerId, updatedCustomer.getId());
        assertEquals(newName, updatedCustomer.getName());
        assertEquals(newEmail, updatedCustomer.getEmail());
        assertEquals(newPassword, updatedCustomer.getPassword());
        assertEquals(customer1.getCpf(), updatedCustomer.getCpf()); // CPF não deve mudar

        // VERIFY
        verify(customerRepository, times(1)).findByIdOptional(customerId);
        verify(customerValidator, times(1)).validateUniqueEmail(newEmail, customerId);
        verify(passwordService, times(1)).hash("newPassword");
        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository, times(1)).persist(customerCaptor.capture());
        Customer capturedCustomer = customerCaptor.getValue();
        assertEquals(newName, capturedCustomer.getName());
        assertEquals(newEmail, capturedCustomer.getEmail());
        assertEquals(newPassword, capturedCustomer.getPassword());
    }

    @Test
    void update_deveLancarNotFoundException_quandoClienteNaoExiste() {
        // ARRANGE
        Long customerId = 99L;
        when(customerRepository.findByIdOptional(customerId)).thenReturn(Optional.empty());

        // ACT & ASSERT
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> customerService.update(customerId, "Nome", "email@email.com", "senha"));
        assertEquals("Cliente não encontrado", exception.getMessage());

        // VERIFY
        verify(customerRepository, times(1)).findByIdOptional(customerId);
        verify(customerValidator, never()).validateUniqueEmail(anyString(), anyLong());
        verify(passwordService, never()).hash(anyString());
        verify(customerRepository, never()).persist(any(Customer.class));
    }

    @Test
    void update_deveLancarBadRequestException_quandoEmailDuplicado() {
        // ARRANGE
        Long customerId = 1L;
        String newEmail = "bob@email.com"; // Email já existe para customer2
        when(customerRepository.findByIdOptional(customerId)).thenReturn(Optional.of(customer1));
        doThrow(new BadRequestException("Já existe um cliente com o email informado"))
                .when(customerValidator).validateUniqueEmail(newEmail, customerId);

        // ACT & ASSERT
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> customerService.update(customerId, "Nome", newEmail, "senha"));
        assertEquals("Já existe um cliente com o email informado", exception.getMessage());

        // VERIFY
        verify(customerRepository, times(1)).findByIdOptional(customerId);
        verify(customerValidator, times(1)).validateUniqueEmail(newEmail, customerId);
        verify(passwordService, never()).hash(anyString());
        verify(customerRepository, never()).persist(any(Customer.class));
    }

    // --- Testes para loggedUser ---

    @Test
    void loggedUser_deveRetornarUsuarioLogadoCliente() {
        // ARRANGE
        when(currentUserService.getLoggedUser()).thenReturn(loggedCustomer);

        // ACT
        LoggedUser result = customerService.loggedUser();

        // ASSERT
        assertNotNull(result);
        assertEquals(loggedCustomer.id(), result.id());
        assertEquals(loggedCustomer.email(), result.email());
        assertEquals(loggedCustomer.cpf(), result.cpf());
        assertEquals(loggedCustomer.role(), result.role());
        assertTrue(result.isCustomer());
        assertFalse(result.isManager());

        // VERIFY
        verify(currentUserService, times(1)).getLoggedUser();
    }

    @Test
    void loggedUser_deveRetornarUsuarioLogadoGerente() {
        // ARRANGE
        when(currentUserService.getLoggedUser()).thenReturn(loggedManager);

        // ACT
        LoggedUser result = customerService.loggedUser();

        // ASSERT
        assertNotNull(result);
        assertEquals(loggedManager.id(), result.id());
        assertEquals(loggedManager.email(), result.email());
        assertEquals(loggedManager.cpf(), result.cpf());
        assertEquals(loggedManager.role(), result.role());
        assertTrue(result.isManager());
        assertFalse(result.isCustomer());

        // VERIFY
        verify(currentUserService, times(1)).getLoggedUser();
    }
}