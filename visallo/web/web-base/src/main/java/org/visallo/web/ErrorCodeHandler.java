package org.visallo.web;

import com.v5analytics.webster.Handler;
import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.RequestResponseHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Sends the error code specified in the constructor for every request. This class is
 * meant to be used as an exception handler.
 */
public class ErrorCodeHandler implements RequestResponseHandler {
    private final int errorCode;

    public ErrorCodeHandler(int errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, HandlerChain handlerChain) throws Exception {
        httpServletResponse.sendError(this.errorCode);
    }
}
