package org.fcrepo.transform;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;

/**
 * The requested transform could not be found
 * 
 * @author whikloj
 * @since 2016-03-25
 */
public class TransformNotFoundException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Ordinary constructor.
     *
     * @param msg the message
     */
    public TransformNotFoundException(final String msg) {
        super(msg);
    }

}
