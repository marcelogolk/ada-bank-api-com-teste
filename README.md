# ada-bank-api-com-testes

Este projeto utiliza o framework Java Quarkus.

Se você quiser aprender mais sobre o Quarkus, visite o site oficial: <https://quarkus.io/>.

## Executando a aplicação em modo de desenvolvimento

Você pode executar a aplicação em modo de desenvolvimento, que permite live coding, utilizando:

```shell script
./mvnw quarkus:dev
```

> **_Nota:_** O Quarkus agora possui uma interface Dev UI, disponível apenas no modo de desenvolvimento em: <http://localhost:8080/q/dev/>.

## Empacotando e executando a aplicação

A aplicação pode ser empacotada utilizando:

```shell script
./mvnw package
```
Isso irá gerar o arquivo `quarkus-run.jar` no diretório: 
```shell script
`target/quarkus-app/`
``` 

Observe que este não é um über-jar, pois as dependências são copiadas para: `target/quarkus-app/lib/`

A aplicação pode ser executada com: `java -jar target/quarkus-app/quarkus-run.jar`.

Gerando um _über-jar_, Se desejar gerar um über-jar, execute:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

A aplicação empacotada como über-jar pode ser executada com: `java -jar target/*-runner.jar`.

## Criando um executável nativo

Você pode gerar um executável nativo com:

```shell script
./mvnw package -Dnative
```

Caso não tenha o GraalVM instalado, é possível gerar o executável nativo via container:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

Após a geração, execute o binário com: `./target/ada-bank-api-1.0.0-SNAPSHOT-runner`

Se quiser saber mais sobre executáveis nativos, consulte: <https://quarkus.io/guides/maven-tooling>.

## Guias relacionados

- REST ([guide](https://quarkus.io/guides/rest)): Implementação Jakarta REST utilizando processamento em tempo de build e Vert.x. Esta extensão não é compatível com `quarkus-resteasy` nem com extensões que dependem dela.
- Hibernate Validator ([guide](https://quarkus.io/guides/validation)): Validação de propriedades de objetos (campos, getters) e parâmetros de métodos para seus beans (REST, CDI, Jakarta Persistence).
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Suporte à serialização JSON com Jackson para Quarkus REST. Esta extensão não é compatível com `quarkus-resteasy` nem com extensões que dependem dela.

## Provided Code

### REST

Permite iniciar rapidamente serviços web REST.

[Consulte a seção do guia correspondente...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
