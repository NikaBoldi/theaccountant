package com.TheAccountant.app.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorsFilter implements Filter {
    
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
    
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, PUT, GET, OPTIONS, DELETE");
        response.setHeader("Access-Control-Allow-Headers", "x-requested-with, Content-Type, "
                + "XSFR-TOKEN, X-CSRF-TOKEN, X-XSRF-TOKEN, Authorization, Access-Control-Allow-Origin");
        response.setHeader("Access-Control-Max-Age", "3600");
        if (request.getMethod() != "OPTIONS") {
            chain.doFilter(req, res);
        }
    }
    
    public void init(FilterConfig filterConfig) {
    
    }
    
    public void destroy() {
    
    }
    
}
