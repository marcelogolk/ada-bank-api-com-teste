package br.com.ada.quarkus.resource.auth;

import br.com.ada.quarkus.service.AuthService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/login")
    @PermitAll
    public Response login(@Valid LoginRequest request) {
        TokenResponse tokenResponse = authService.login(
                request.email(),
                request.password()
        );

        return Response.ok(tokenResponse).build();
    }
}