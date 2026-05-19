# Trabalho Final — Testes de Software
## Simulação de Banco Digital (Projeto Quarkus)

## Contexto

Durante o semestre vocês desenvolveram uma aplicação de simulação de banco digital utilizando Quarkus, com operações de conta bancária organizadas nas camadas **Resource**, **Service**, **Repository** e **DTOs**.

O objetivo deste trabalho é aplicar os conceitos de testes de software vistos em aula diretamente sobre esse projeto — sem reescrever a lógica de negócio, apenas adicionando a cobertura de testes adequada para cada camada.

---

## O que deve ser entregue

O projeto Quarkus original com as seguintes adições:

```
src/
  test/
    java/
      ├── unitario/
      │     ├── ContaServiceTest.java        ← testes unitários do Service
      │     └── (outros services se houver)
      │
      ├── integracao/
      │     └── ContaResourceTest.java       ← testes de integração via RestAssured
      │
      └── (demais classes de teste)
  resources/
      └── application.properties            ← configuração do banco para testes
```

---

## Parte 1 — Testes Unitários da Camada de Serviço

### O que testar

Escreva testes unitários para **todos os métodos públicos** do `ContaService` (e de outros services se o projeto tiver mais de um).

Para cada método, cubra obrigatoriamente:

- O **cenário de sucesso** (caminho feliz)
- Os **cenários de erro** esperados (saldo insuficiente, conta inexistente, valor inválido, etc.)
- **Verificações de comportamento** com `verify()` — confirme que o Repository foi chamado corretamente

### Padrão obrigatório de escrita

Cada teste deve seguir o padrão **AAA**:

```java
@Test
void deveDebitarValorDaSacado() {
    // ARRANGE — prepara os dados e configura os mocks
    Conta conta = new Conta(1, "Ana", BigDecimal.valueOf(1000));
    when(contaRepository.findById(1)).thenReturn(Optional.of(conta));

    // ACT — executa o método sendo testado
    contaService.sacar(1, BigDecimal.valueOf(300));

    // ASSERT — valida o resultado
    assertEquals(BigDecimal.valueOf(700), conta.getSaldo());
    verify(contaRepository, times(1)).save(conta);
}
```

---

## Parte 2 — Testes de Integração da Camada de Resource

### O que é testado aqui

Os testes de integração verificam o **fluxo completo**: requisição HTTP → Resource → Service → Repository → banco de dados em memória (H2) → resposta HTTP.

Não há mocks nessa camada — tudo é real.

### Cenários mínimos esperados

| Endpoint | Cenário | Status esperado | O que verificar |
|---|---|---|---|
| `POST /contas` | Criar conta válida | 201 | Body com id gerado e saldo correto |
| `GET /contas` | Listar (banco vazio) | 200 | Array vazio `[]` |
| `GET /contas` | Listar (com dados) | 200 | Array com quantidade correta |
| `GET /contas/{id}` | Buscar existente | 200 | Campos corretos no body |
| `GET /contas/{id}` | Buscar inexistente | 404 | Mensagem de erro |
| `POST /contas/{id}/depositar` | Depositar valor válido | 200 | Saldo atualizado no body |
| `POST /contas/{id}/sacar` | Sacar com saldo suficiente | 200 | Saldo atualizado no body |
| `POST /contas/{id}/sacar` | Sacar sem saldo | 422 ou 400 | Mensagem de erro |

> Adapte os endpoints e status codes ao que o **seu** projeto implementa.

---

## Parte 3 — Cobertura de Testes com Jacoco

> **Meta mínima:** 70% de cobertura de linha · 60% de cobertura de branch

---

## Critérios de Avaliação

| Critério | Peso | O que será avaliado |
|---|------|---|
| Testes unitários — quantidade | 20%  | Todos os métodos públicos do Service têm ao menos um teste |
| Testes unitários — qualidade | 20%  | Uso correto de AAA, `assertThrows`, `verify()`, cenários de erro |
| Testes de integração | 25%  | Endpoints cobertos, status HTTP corretos, verificação de body com RestAssured |
| Cobertura Jacoco | 25%  | Meta mínima atingida (70% linha / 60% branch); build passando |
| Organização do projeto | 10%  | Estrutura de pastas, nomes de métodos expressivos, `@BeforeEach` usado corretamente |

---

## Regras de entrega

```
✅ O projeto deve compilar com mvn clean install sem erros
✅ Todos os testes devem passar (nenhum @Disabled sem justificativa)
✅ Entregar o link do repositório Git

❌ Não serão aceitos testes sem assertivas (testes que não verificam nada)
❌ Não serão aceitos testes que dependem da ordem de execução
❌ Não será aceita cobertura obtida por testes vazios ou sem assert
```
