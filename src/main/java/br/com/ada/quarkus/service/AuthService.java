package br.com.ada.quarkus.service;

import br.com.ada.quarkus.model.Customer;
import br.com.ada.quarkus.model.LoggedUser;
import br.com.ada.quarkus.resource.auth.TokenResponse;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Duration;
@ApplicationScoped
public class AuthService implements CurrentUserService {

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    @Inject
    CustomerService customerService;

    @Inject
    PasswordService passwordService;

    @Inject
    JsonWebToken jwt;

    @Override
    public LoggedUser getLoggedUser() {
        String email = jwt.getName();  // armazena em variável — chama só uma vez

        if (email == null) {
            throw new NotAuthorizedException("Nenhum usuário autenticado na requisição atual");
        }

        return new LoggedUser(
                getUserId(),
                email,                  // usa a variável
                jwt.getClaim("cpf"),
                getRole()
        );
    }

    private Long getUserId() {
        return Long.parseLong(jwt.getClaim("userId").toString());
    }

    private String getRole() {
        return jwt.getGroups()
                .stream()
                .findFirst()
                .orElse("CLIENTE");
    }

    public TokenResponse login(String email, String password) {
        Customer customer = customerService.findByEmail(email);

        validatePassword(customer, password);

        String token = generateToken(customer);

        return new TokenResponse(
                token,
                customer.getEmail(),
                customer.getName()
        );
    }

    private void validatePassword(Customer customer, String password) {
        boolean isValid = customer != null
                && passwordService.verify(customer.getPassword(), password);

        if (!isValid) {
            throw new NotAuthorizedException("Credenciais inválidas");
        }
    }

    private String generateToken(Customer customer) {
        return Jwt.issuer(issuer)
                .upn(customer.getEmail())
                .groups(customer.getRole().getValue())
                .claim("userId", customer.getId())
                .claim("cpf", customer.getCpf())
                .expiresIn(Duration.ofMinutes(30))
                .sign();
    }
}