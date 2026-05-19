package br.com.ada.quarkus.exception;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;


@Provider
public class BadRequestExceptionMapper
        implements ExceptionMapper<BadRequestException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(BadRequestException exception) {
        ErrorResponse error = new ErrorResponse(
                Response.Status.BAD_REQUEST.getStatusCode(),
                Response.Status.BAD_REQUEST.getReasonPhrase(),
                exception.getMessage(),
                uriInfo.getPath()
        );

        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(error)
                .build();
    }
}