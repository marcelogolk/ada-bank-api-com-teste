package br.com.ada.quarkus.exception;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ForbiddenExceptionMapper
        implements ExceptionMapper<ForbiddenException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ForbiddenException exception) {
        ErrorResponse error = new ErrorResponse(
                Response.Status.FORBIDDEN.getStatusCode(),
                Response.Status.FORBIDDEN.getReasonPhrase(),
                exception.getMessage(),
                uriInfo.getPath()
        );

        return Response
                .status(Response.Status.FORBIDDEN)
                .entity(error)
                .build();
    }
}