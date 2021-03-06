package com.TheAccountant.app.filter;

import com.TheAccountant.app.authentication.SessionAuthentication;
import com.TheAccountant.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class AuthenticationFilter implements Filter {
    
    @Autowired
    private SessionService sessionService;
    
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        if (httpRequest.getMethod().equals("OPTIONS")) {
            return;
        }
        
        final String authorization = httpRequest.getHeader("Authorization");
        String[] credentials = sessionService.extractUsernameAndPassword(authorization);
        String loginUsername = credentials.length == 0 ? null : credentials[0];
        String clientIpAddress = extractClientIpAddress(httpRequest);
        SecurityContextHolder.getContext().setAuthentication(new SessionAuthentication(loginUsername, clientIpAddress));
        
        if (isAllowedURL(httpRequest.getRequestURI())) {
            chain.doFilter(request, response);
        } else if ((authorization != null && sessionService.isAValidAuthenticationString(authorization, clientIpAddress))) {
            chain.doFilter(request, response);
        } else {
            httpResponse.setStatus(401);
        }
    }
    
    private boolean isAllowedURL(String url) {
    
        boolean isAllowed = false;
        if (url != null) {
            if (url.contains("?")) {
                url = url.substring(0, url.indexOf('?'));
            }
            if (url.contains("/user/login")
                    || url.contains("/user/logout")
                    || url.contains("/user/activation/")
                    || url.contains("/user/forgot_password")
                    || url.contains("/user/renew_forgot_password")
                    || url.contains("/user/add")
                    || url.contains("/user/description")
                    || url.contains("/.well-known/acme-challenge")
                    || url.equals("/")) {
                isAllowed = true;
            }
        }
        return isAllowed;
    }
    
    public void init(FilterConfig filterConfig) {
    
    }
    
    public void destroy() {
    
    }
    
    private String extractClientIpAddress(HttpServletRequest request) {
    
        //is client behind something?
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
    
}
