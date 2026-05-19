package unitario;

import br.com.ada.quarkus.service.PasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordServiceTest {

    private PasswordService passwordService;

    @BeforeEach
    void setUp() throws Exception {
        passwordService = new PasswordService();
        var method = PasswordService.class.getDeclaredMethod("init");
        method.setAccessible(true);
        method.invoke(passwordService);
    }

    @Test
    void deveRetornarHashQuandoSenhaValida() {
        // ACT
        String result = passwordService.hash("123");

        // ASSERT
        assertNotNull(result);
        assertTrue(passwordService.verify(result, "123"));
    }

    @Test
    void deveRetornarFalseQuandoHashedPasswordEhNulo() {
        // ACT
        boolean result = passwordService.verify(null, "123");

        // ASSERT
        assertFalse(result);
    }

    @Test
    void deveRetornarFalseQuandoRawPasswordEhNulo() {
        // ARRANGE
        String hash = passwordService.hash("123");

        // ACT
        boolean result = passwordService.verify(hash, null);

        // ASSERT
        assertFalse(result);
    }

    @Test
    void deveRetornarTrueQuandoSenhaValida() {
        // ACT
        boolean result = passwordService.verify(passwordService.hash("123"), "123");

        // ASSERT
        assertTrue(result);
    }

    @Test
    void deveRetornarFalseQuandoSenhaInvalida() {
        // ACT
        boolean result = passwordService.verify(passwordService.hash("123"), "000");

        // ASSERT
        assertFalse(result);
    }
}