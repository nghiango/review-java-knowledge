package lab.java25boot4.springmvc.broken.apiversioning;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class LegacyVersionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String versionHeader = request.getHeader("X-API-Version");
        if (versionHeader == null || (!versionHeader.equals("1.0") && !versionHeader.equals("2.0"))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        request.setAttribute("resolvedVersion", versionHeader);
        return true;
    }
}
