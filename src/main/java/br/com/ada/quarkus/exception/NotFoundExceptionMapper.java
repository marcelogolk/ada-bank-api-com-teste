package br.com.ada.quarkus.exception;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NotFoundExceptionMapper
        implements ExceptionMapper<NotFoundException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(NotFoundException exception) {

        ErrorResponse error = new ErrorResponse(
                Response.Status.NOT_FOUND.getStatusCode(),
                Response.Status.NOT_FOUND.getReasonPhrase(),
                exception.getMessage(),
                uriInfo.getPath()
        );

        return Response
                .status(Response.Status.NOT_FOUND)
                .entity(error)
                .build();
    }
}