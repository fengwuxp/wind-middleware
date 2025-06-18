package com.wind.server.flow;

import com.alibaba.csp.sentinel.slots.block.BlockException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handler for the blocked request.
 *
 * @author wuxp
 * @date 2024-03-07 17:49
 **/
public interface SentinelBlockExceptionHandler {

    /**
     * Handle the request when blocked.
     *
     * @param request   Servlet request
     * @param response  Servlet response
     * @param exception the block exception
     */
    void handle(HttpServletRequest request, HttpServletResponse response, BlockException exception);
}
