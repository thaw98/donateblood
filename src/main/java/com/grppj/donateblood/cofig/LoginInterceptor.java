package com.grppj.donateblood.cofig;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);

        // if no user logged in → redirect to /login
        if (session == null || session.getAttribute("loggedUser") == null) {
            response.sendRedirect("/login");
            return false;
        }

        String activeRole = (String) session.getAttribute("activeRole");
        if (activeRole == null) {
            session.invalidate();
            response.sendRedirect("/login");
            return false;
        }

        String uri = request.getRequestURI();

        // Donor/Receiver pages
        if ((uri.startsWith("/index") || uri.startsWith("/indexR") || uri.startsWith("/profile") || uri.startsWith("/appointment"))
                && !("donor".equals(activeRole) || "receiver".equals(activeRole))) {
            session.invalidate();
            response.sendRedirect("/login");
            return false;
        }

        // Admin pages
        if (uri.startsWith("/admin") && !"admin".equals(activeRole)) {
            session.invalidate();
            response.sendRedirect("/login");
            return false;
        }

        // Superadmin pages
        if (uri.startsWith("/superadmin") && !"superadmin".equals(activeRole)) {
            session.invalidate();
            response.sendRedirect("/login");
            return false;
        }

        return true; // allow access
    }
}
