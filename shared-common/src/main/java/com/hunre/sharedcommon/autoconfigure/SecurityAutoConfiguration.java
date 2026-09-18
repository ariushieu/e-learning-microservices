package com.hunre.sharedcommon.autoconfigure;

import com.hunre.sharedcommon.security.AuthenticatedUserArgumentResolver;
import com.hunre.sharedcommon.security.DevIdentityFilter;
import com.hunre.sharedcommon.security.JwtAuthenticationFilter;
import com.hunre.sharedcommon.security.JwtSecurityProperties;
import com.hunre.sharedcommon.security.JwtVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Bật xác thực JWT cho mọi service có shared-common, không service nào phải tự khai báo.
 *
 * <p>Tách khỏi {@link SharedCommonAutoConfiguration} có chủ đích: phần xử lý lỗi thì
 * service nào cũng cần và không có công tắc, còn phần xác thực thì tắt được, nên để lẫn
 * nhau sẽ khiến tắt xác thực kéo theo mất luôn handler lỗi.
 *
 * <p>Giống lớp kia, điều kiện {@code SERVLET} khiến api-gateway (chạy WebFlux) không bị
 * dính filter của Spring MVC. Gateway có filter riêng, chỉ dùng chung {@link JwtVerifier}.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(JwtSecurityProperties.class)
public class SecurityAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SecurityAutoConfiguration.class);

    /**
     * Đặt filter chạy sớm, trước các filter nghiệp vụ khác, nhưng vẫn sau filter mã hóa
     * ký tự của Spring để thông báo lỗi tiếng Việt không bị vỡ font.
     */
    private static final int FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 100;

    /**
     * Cho phép mọi controller khai {@code AuthenticatedUser} làm tham số. Đăng ký ở cả hai
     * chế độ bật và tắt, vì khi tắt vẫn có danh tính giả lập để trả về.
     */
    @Bean
    public WebMvcConfigurer authenticatedUserArgumentResolverConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(new AuthenticatedUserArgumentResolver());
            }
        };
    }

    /** Khi {@code elearning.security.enabled} bật hoặc không khai báo. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "elearning.security", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    static class JwtEnabledConfiguration {

        @Bean
        @ConditionalOnMissingBean
        JwtVerifier jwtVerifier(JwtSecurityProperties properties) {
            return new JwtVerifier(properties.getJwtSecret());
        }

        @Bean
        FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilter(
                JwtVerifier jwtVerifier, JwtSecurityProperties properties) {

            var registration = new FilterRegistrationBean<>(
                    new JwtAuthenticationFilter(jwtVerifier, properties.getPublicPaths()));
            registration.setOrder(FILTER_ORDER);
            return registration;
        }
    }

    /** Khi {@code elearning.security.enabled=false}, chỉ dùng lúc phát triển. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "elearning.security", name = "enabled", havingValue = "false")
    static class JwtDisabledConfiguration {

        @Bean
        FilterRegistrationBean<DevIdentityFilter> devIdentityFilter(JwtSecurityProperties properties) {
            log.warn("XÁC THỰC ĐANG TẮT (elearning.security.enabled=false). "
                            + "Mọi request được coi là người dùng id={} với vai trò {}. "
                            + "Chỉ dùng khi phát triển.",
                    properties.getDevUser().getId(), properties.getDevUser().getRoles());

            var registration = new FilterRegistrationBean<>(
                    new DevIdentityFilter(properties.getDevUser()));
            registration.setOrder(FILTER_ORDER);
            return registration;
        }
    }
}
