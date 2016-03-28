package org.fcrepo.transform.http;

import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.fcrepo.transform.TransformNotFoundException;

/**
 * Handle TransformNotFoundExceptions
 *
 * @author whikloj
 * @since 2016-03-25
 */
@Provider
public class TransformNotFoundExceptionMapper implements ExceptionMapper<TransformNotFoundException> {

    @Override
    public Response toResponse(final TransformNotFoundException e) {
        final String msg = e.getMessage();
        return status(BAD_REQUEST).entity(msg).build();
    }

}
