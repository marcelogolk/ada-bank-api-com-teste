package unitario;

import br.com.ada.quarkus.model.LoggedUser;
import br.com.ada.quarkus.service.CustomerService;
import br.com.ada.quarkus.service.CurrentUserService;
import br.com.ada.quarkus.service.PasswordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

    @Mock
    CurrentUserService currentUserService;

    @Mock
    PasswordService passwordService;

    @InjectMocks
    CustomerService customerService;

    @Test
    void deveRetornarUsuarioLogadoCliente() {
        // ARRANGE
        LoggedUser loggedUser = new LoggedUser(
                3L,
                "carlos.oliveira@bancada.com.br",
                "00000000003",
                "CLIENTE"
        );
        when(currentUserService.getLoggedUser()).thenReturn(loggedUser);

        // ACT
        var result = customerService.loggedUser();

        // ASSERT
        assertNotNull(result);
        assertEquals(3L, result.id());
        assertEquals("carlos.oliveira@bancada.com.br", result.email());
        assertEquals("00000000003", result.cpf());
        assertEquals("CLIENTE", result.role());
        assertTrue(result.isCustomer());
        assertFalse(result.isManager());

        verify(currentUserService, times(1)).getLoggedUser();
    }

   @Test
    void deveRetornarUsuarioLogadoGerente() {
        // ARRANGE
        LoggedUser loggedUser = new LoggedUser(
                10L,
                "alice.silva@bancada.com.br",
                "00000000001",
                "GERENTE"
        );
        when(currentUserService.getLoggedUser()).thenReturn(loggedUser);

        // ACT
        var result = customerService.loggedUser();

        // ASSERT
        assertNotNull(result);
        assertEquals(10L, result.id());
        assertEquals("alice.silva@bancada.com.br", result.email());
        assertEquals("00000000001", result.cpf());
        assertEquals("GERENTE", result.role());
        assertTrue(result.isManager());
        assertFalse(result.isCustomer());

        verify(currentUserService, times(1)).getLoggedUser();
    }
}
