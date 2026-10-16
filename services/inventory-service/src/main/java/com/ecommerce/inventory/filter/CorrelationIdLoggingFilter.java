package com.ecommerce.inventory.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that captures the distributed tracing correlation ID and instance ID
 * into the SLF4J MDC context for consistent log outputs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdLoggingFilter implements Filter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_CORRELATION_ID = "X-Correlation-Id";
    public static final String MDC_INSTANCE_ID = "instanceId";

    private final String instanceId = UUID.randomUUID().toString().substring(0, 8);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest httpRequest) {
                String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
                if (correlationId == null || correlationId.trim().isEmpty()) {
                    correlationId = UUID.randomUUID().toString();
                }

                MDC.put(MDC_CORRELATION_ID, correlationId);
                MDC.put(MDC_INSTANCE_ID, instanceId);

                if (response instanceof HttpServletResponse httpResponse) {
                    httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
                }
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_CORRELATION_ID);
            MDC.remove(MDC_INSTANCE_ID);
        }
    }
}
