package br.com.ada.quarkus.resource.customer;

import br.com.ada.quarkus.model.Customer;
import br.com.ada.quarkus.model.LoggedUser;
import br.com.ada.quarkus.resource.PageResponse;
import br.com.ada.quarkus.service.CurrentUserService;
import br.com.ada.quarkus.service.CustomerService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
@Path("/clientes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CustomerResource {

    private static final int MAX_PAGE_SIZE = 100;

    @Inject
    CustomerService customerService;

    @Inject
    CurrentUserService currentUserService;

    @GET
    @RolesAllowed("GERENTE")
    public PageResponse<CustomerResponse> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size) {

        validatePagination(page, size);

        return PageResponse.from(
                customerService.list(page, size),
                this::toResponse
        );
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"GERENTE", "CLIENTE"})
    public CustomerResponse findById(@PathParam("id") Long id) {

        if (id == null) {
            throw new BadRequestException("O ID do cliente é obrigatório");
        }

        validateOwnershipOrManager(id);

        return toResponse(customerService.findById(id));
    }

    @POST
    @Transactional
    @PermitAll
    public Response create(
            @Valid CreateCustomerRequest request,
            @Context UriInfo uriInfo) {

        Customer customer = customerService.create(toCustomer(request));
        CustomerResponse response = toResponse(customer);

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(response.id().toString())
                .build();

        return Response.created(location)
                .entity(response)
                .build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @RolesAllowed({"GERENTE", "CLIENTE"})
    public CustomerResponse update(
            @PathParam("id") Long id,
            @Valid UpdateCustomerRequest request) {

        if (id == null) {
            throw new BadRequestException("O ID do cliente é obrigatório");
        }

        validateOwnershipOrManager(id);

        Customer updatedCustomer = customerService.update(
                id,
                request.name(),
                request.email(),
                request.password()
        );

        return toResponse(updatedCustomer);
    }

    private void validateOwnershipOrManager(Long customerId) {
        LoggedUser currentUser = currentUserService.getLoggedUser();

        if (currentUser.isManager()) {
            return;
        }

        if (!currentUser.id().equals(customerId)) {
            throw new ForbiddenException(
                    "Acesso negado: apenas o próprio cliente ou um gerente pode realizar esta operação"
            );
        }
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("page deve ser >= 0");
        }

        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size deve estar entre 1 e " + MAX_PAGE_SIZE);
        }
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail()
        );
    }

    private Customer toCustomer(CreateCustomerRequest request) {
        return new Customer(
                null,
                request.name(),
                request.cpf(),
                request.email(),
                request.password()
        );
    }
}