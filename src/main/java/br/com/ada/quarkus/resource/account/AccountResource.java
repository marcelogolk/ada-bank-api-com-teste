package br.com.ada.quarkus.resource.account;

import br.com.ada.quarkus.model.Account;
import br.com.ada.quarkus.model.LoggedUser;
import br.com.ada.quarkus.model.Transaction;
import br.com.ada.quarkus.resource.transaction.TransactionResponse;
import br.com.ada.quarkus.resource.transaction.TransactionResponseMapper;
import br.com.ada.quarkus.service.AccountService;
import br.com.ada.quarkus.service.CurrentUserService;
import br.com.ada.quarkus.service.CustomerService;
import br.com.ada.quarkus.service.TransactionService;
import br.com.ada.quarkus.util.OutputMaskFormatter;
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
@Path("/contas")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AccountResource {

    @Inject
    AccountService accountService;

    @Inject
    CurrentUserService currentUserService;

    @Inject
    CustomerService customerService;

    @Inject
    TransactionService transactionService;

    @Inject
    TransactionResponseMapper transactionResponseMapper;

    @POST
    @Transactional
    @RolesAllowed("GERENTE")
    public Response create(
            @Valid CreateAccountRequest request,
            @Context UriInfo uriInfo) {

        Account account = accountService.create(toAccount(request));
        AccountResponse response = toResponse(account);

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(response.id().toString())
                .build();

        return Response.created(location)
                .entity(response)
                .build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"GERENTE", "CLIENTE"})
    public AccountDetailsResponse findById(@PathParam("id") Long id) {

        if (id == null) {
            throw new BadRequestException("O ID da conta é obrigatório");
        }

        Account account = accountService.findById(id);
        validateAccountOwnership(account);

        var customer = customerService.findById(account.getCustomerId());

        CustomerSummaryResponse holder =
                new CustomerSummaryResponse(
                        customer.getId(),
                        customer.getName(),
                        customer.getEmail()
                );

        var todayTransactions =
                transactionService
                        .listTodayByAccountId(id, 0, 10)
                        .content()
                        .stream()
                        .map(transactionResponseMapper::toResponse)
                        .toList();

        AccountLinksResponse links =
                new AccountLinksResponse(
                        "/transacoes?accountId=" + id
                );

        return new AccountDetailsResponse(
                account.getId(),
                OutputMaskFormatter.formatAccountNumber(account.getAccountNumber()),
                account.getType(),
                account.getBalance(),
                holder,
                todayTransactions,
                links
        );
    }

    @POST
    @Path("/{id}/deposito")
    @Transactional
    @PermitAll
    public TransactionResponse deposit(
            @PathParam("id") Long id,
            @Valid DepositRequest request) {

        Transaction transaction = accountService.deposit(id, request.amount());
        return transactionResponseMapper.toResponse(transaction);
    }

    @POST
    @Path("/{id}/saque")
    @Transactional
    @RolesAllowed({"GERENTE", "CLIENTE"})
    public TransactionResponse withdraw(
            @PathParam("id") Long id,
            @Valid WithdrawRequest request) {

        Account account = accountService.findById(id);
        validateAccountOwnership(account);

        Transaction transaction = accountService.withdraw(id, request.amount());
        return transactionResponseMapper.toResponse(transaction);
    }

    @POST
    @Path("/{id}/transferencia")
    @Transactional
    @RolesAllowed({"GERENTE", "CLIENTE"})
    public TransactionResponse transfer(
            @PathParam("id") Long id,
            @Valid TransferRequest request) {

        Account sourceAccount = accountService.findById(id);
        validateAccountOwnership(sourceAccount);

        Transaction transaction = accountService.transfer(
                id,
                request.destinationAccountId(),
                request.amount()
        );

        return transactionResponseMapper.toResponse(transaction);
    }

    private void validateAccountOwnership(Account account) {
        LoggedUser currentUser = currentUserService.getLoggedUser();

        if (currentUser.isManager()) {
            return;
        }

        if (!currentUser.id().equals(account.getCustomerId())) {
            throw new ForbiddenException(
                    "Acesso negado: apenas o proprietário da conta ou um gerente pode realizar esta operação"
            );
        }
    }

    private AccountResponse toResponse(Account account) {
        return AccountResponse.fromEntity(account);
    }

    private Account toAccount(CreateAccountRequest request) {
        Account account = new Account();
        account.setType(request.type());
        account.setCustomerId(request.customerId());
        return account;
    }
}