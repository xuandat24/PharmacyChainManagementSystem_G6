package fu.se.pharmacy.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login", "/logout",
                        // BUG FIX 6: /api/payment/callback bị AuthInterceptor chặn
                        // → gateway gọi callback nhận 302 redirect to /login
                        "/api/**",
                        "/css/**", "/js/**", "/images/**",
                        "/favicon.ico"
                );
    }
}