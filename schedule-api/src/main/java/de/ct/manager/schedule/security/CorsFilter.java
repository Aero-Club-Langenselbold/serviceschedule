package de.ct.manager.schedule.security;

import static org.springframework.http.HttpHeaders.*;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter implements Filter {

    @Value("#{'${cors.allowed-origins}'.split(',')}")
    private Set<String> origins;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;

        String origin = request.getHeader(ORIGIN);

        if (StringUtils.isEmpty(origin)) {
            chain.doFilter(req, res);
        } else if (origins.contains(origin)) {
            response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.setHeader(ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PATCH, PUT, DELETE, OPTIONS, HEAD");
            response.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            response.setHeader(
                ACCESS_CONTROL_ALLOW_HEADERS,
                "accept,accept-language,authorization,content-type,content-disposition,tix-selected-account,crnk-compact"
            );
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);

            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                chain.doFilter(req, res);
            }
        } else {
            response.setHeader(VARY, "Origin");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }

}
