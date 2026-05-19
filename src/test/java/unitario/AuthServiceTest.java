package unitario;

import br.com.ada.quarkus.model.Customer;
import br.com.ada.quarkus.model.LoggedUser;
import br.com.ada.quarkus.resource.auth.TokenResponse;
import br.com.ada.quarkus.service.AuthService;
import br.com.ada.quarkus.service.CustomerService;
import br.com.ada.quarkus.service.PasswordService;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    CustomerService customerService;

    @Mock
    PasswordService passwordService;

    @Mock
    JsonWebToken jwt;

    @InjectMocks
    AuthService authService;


    @BeforeEach
    void setUp() throws Exception {
        var field = AuthService.class.getDeclaredField("issuer");
        field.setAccessible(true);
        field.set(authService, "ada_bank_api");
    }

    @Test
    void deveRetornarLoggedUserQuandoJwtValido() {
        // ARRANGE — o que você precisa mockar aqui?
        when(jwt.getName()).thenReturn("alice@banco.com");   // simula o email do token
        when(jwt.getClaim("userId")).thenReturn("1");         // simula o claim userId
        when(jwt.getClaim("cpf")).thenReturn("12345678901"); // simula o claim cpf
        when(jwt.getGroups()).thenReturn(Set.of("GERENTE"));
        // ACT — chame o método
        LoggedUser result = authService.getLoggedUser();

        // ASSERT
        assertEquals(1L, result.id());
        assertEquals("alice@banco.com", result.email());
        assertEquals("12345678901", result.cpf());
        assertEquals("GERENTE", result.role());

        // VERIFY
        verify(jwt, times(1)).getName();
        verify(jwt, times(1)).getClaim("userId");
        verify(jwt, times(1)).getClaim("cpf");
        verify(jwt, times(1)).getGroups();

    }

    @Test
    void deveLancarNotAuthorizedQuandoJwtNaoTemNome() {
        // ARRANGE
        when(jwt.getName()).thenReturn(null);
        // ACT
        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class, () -> authService.getLoggedUser());
        // ASSERT
        assertNotNull(ex);

        // VERIFY
        verify(jwt, times(1)).getName();
        verify(jwt, never()).getClaim(any(String.class));
        verify(jwt, never()).getGroups();
    }

    @Test
    void deveLancarNotFoundExceptionQuandoEmailNaoExiste() {
        // ARRANGE
        when(customerService.findByEmail("emailinexistente@banco.com"))
                .thenThrow(new NotFoundException("Cliente não encontrado"));
        // ACT
        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> authService.login("emailinexistente@banco.com",
                                     "$argon2id$v=19$m=65536,t=3,p=4$xyz"));
        assertNotNull(ex);

        // VERIFY
        verify(customerService, times(1)).findByEmail("emailinexistente@banco.com");
        verify(passwordService, never()).verify(any(), any());
    }

    @Test
    void deveLancarNotAuthorizedExceptionQuandoSenhaInvalida(){
        // ARRANGE
        Customer customer = new Customer(1L, "Alice Silva", "00000000001",
                "alice.silva@bancada.com.br", "$argon2id$v=19$m=65536,t=3,p=4$xyz");
        when(customerService.findByEmail("alice.silva@bancada.com.br"))
                .thenReturn(customer);
        when(passwordService.verify(any(), any())).thenReturn(false);

        // ACT
        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> authService.login("alice.silva@bancada.com.br",
                        "$argon2id$v=19$m=65536,t=3,p=4$xyz"));
        assertNotNull(ex);

        // VERIFY
        verify(customerService, times(1)).findByEmail("alice.silva@bancada.com.br");
        verify(passwordService, times(1)).verify(any(), any());
        verify(jwt, never()).getName();
        verify(jwt, never()).getClaim(any(String.class));
        verify(jwt, never()).getGroups();
    }

    @Test
    void deveRetornarTokenResponseQuandoCredenciaisValidas(){
        // ARRANGE
        Customer customer = new Customer(1L, "Alice Silva", "00000000001",
                "alice.silva@bancada.com.br", "$argon2id$v=19$m=65536,t=3,p=4$wwnDzatVCvv+lTpxtmKuAA$heZ3gWTqRNLKf6qJEJ4yPBtZ8cKBIqJ5HPPLjmYPN70");
        when(customerService.findByEmail("alice.silva@bancada.com.br"))
                .thenReturn(customer);
        when(passwordService.verify(any(), any())).thenReturn(true);

        //ACT
        TokenResponse result = authService.login("alice.silva@bancada.com.br", "123");

        // ASSERT
        assertNotNull(result.token());
        assertEquals("alice.silva@bancada.com.br", result.email());
        assertEquals("Alice Silva", result.name());

        // VERIFY
        verify(customerService, times(1)).findByEmail("alice.silva@bancada.com.br");
        verify(passwordService, times(1)).verify(any(), any());
        verify(jwt, never()).getName();
        verify(jwt, never()).getClaim(any(String.class));
        verify(jwt, never()).getGroups();
    }

    @Test
    void deveLancarNotAuthorizedExceptionQuandoCustomerNaoExiste() {
        // ARRANGE
        when(customerService.findByEmail("alice.silva@bancada.com.br"))
                .thenReturn(null);
        // ACT
        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> authService.login("alice.silva@bancada.com.br",
                        "123"));
        assertNotNull(ex);

        // VERIFY
        verify(customerService, times(1)).findByEmail("alice.silva@bancada.com.br");
        verify(passwordService, never()).verify(any(), any());
        verify(jwt, never()).getName();
        verify(jwt, never()).getClaim(any(String.class));
        verify(jwt, never()).getGroups();
    }

}