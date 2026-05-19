package unitario;

import br.com.ada.quarkus.util.OutputMaskFormatter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OutputMaskFormatterTest {

    @Test
    void deveFormatarCpfValidoCorretamente() {
        // ARRANGE
        String cpf = "12345678901";

        // ACT
        String resultado = OutputMaskFormatter.formatCpf(cpf);

        // ASSERT
        assertEquals("123.456.789-01", resultado);
    }

    @Test
    void deveRetornarNullQuandoCpfForNull() {
        // ARRANGE + ACT
        String resultado = OutputMaskFormatter.formatCpf(null);

        // ASSERT
        assertNull(resultado);
    }

    @Test
    void deveRetornarCpfSemFormatacaoQuandoFormatoInvalido() {
        // ARRANGE
        String cpfInvalido = "123";

        // ACT
        String resultado = OutputMaskFormatter.formatCpf(cpfInvalido);

        // ASSERT
        assertEquals("123", resultado);
    }

    @Test
    void deveFormatarNumeroDaContaValidoCorretamente() {
        // ARRANGE
        String numeroConta = "1234567890";

        // ACT
        String resultado = OutputMaskFormatter.formatAccountNumber(numeroConta);

        // ASSERT
        assertEquals("123456789-0", resultado);
    }

    @Test
    void deveRetornarNullQuandoNumeroDaContaForNull() {
        // ARRANGE + ACT
        String resultado = OutputMaskFormatter.formatAccountNumber(null);

        // ASSERT
        assertNull(resultado);
    }

    @Test
    void deveRetornarNumeroDaContaSemFormatacaoQuandoFormatoInvalido() {
        // ARRANGE
        String numeroInvalido = "123";

        // ACT
        String resultado = OutputMaskFormatter.formatAccountNumber(numeroInvalido);

        // ASSERT
        assertEquals("123", resultado);
    }
}