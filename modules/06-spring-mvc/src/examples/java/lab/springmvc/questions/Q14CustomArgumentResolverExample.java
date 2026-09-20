package lab.springmvc.questions;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class Q14CustomArgumentResolverExample {

    record AuthenticatedTenant(String tenantId) {}

    static class TenantArgumentResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().equals(AuthenticatedTenant.class);
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory) {
            String tenantHeader = webRequest.getHeader("X-Tenant-Id");
            return new AuthenticatedTenant(tenantHeader != null ? tenantHeader : "default");
        }
    }

    public static void main(String[] args) {
        TenantArgumentResolver resolver = new TenantArgumentResolver();
        boolean supports =
                resolver.supportsParameter(
                        new MethodParameter(
                                Q14CustomArgumentResolverExample.class.getDeclaredMethods()[0],
                                -1));

        System.out.println("Custom resolver created, supports parameter: " + !supports);
    }
}
