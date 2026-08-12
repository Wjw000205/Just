package com.justeam.rdp.common;

import java.io.IOException;

/** Raised while the servlet input stream is being consumed, before MVC materializes the whole body. */
public class RequestBodyTooLargeException extends IOException {
    public RequestBodyTooLargeException(String message) {
        super(message);
    }
}
