package example.timeflows.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RequestLoggingConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingConfig.class);

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(
                        new HandlerInterceptor() {
                            @Override
                            public void afterCompletion(
                                    HttpServletRequest request,
                                    HttpServletResponse response,
                                    Object handler,
                                    Exception exception) {
                                Principal principal = request.getUserPrincipal();
                                log.info(
                                        "HTTP {} {} -> {} user={}",
                                        request.getMethod(),
                                        request.getRequestURI(),
                                        response.getStatus(),
                                        principal == null ? "anonymous" : principal.getName());
                            }
                        })
                .excludePathPatterns("/css/**", "/js/**", "/favicon.ico");
    }
}
