package com.hunre.sharedcommon.autoconfigure;

import com.hunre.sharedcommon.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Tự động đăng ký các bean dùng chung khi service có shared-common trong dependency.
 *
 * <p><b>Vì sao cần lớp này.</b> Spring chỉ quét component trong gói của lớp
 * {@code @SpringBootApplication} trở xuống. Gói của các service là
 * {@code com.hunre.authservice}, {@code com.hunre.courseservice}... còn
 * {@code GlobalExceptionHandler} nằm ở {@code com.hunre.sharedcommon}, nên nó
 * <b>không</b> được quét tới. Nếu chỉ đặt {@code @RestControllerAdvice} rồi để đó thì
 * handler im lặng không chạy, và mỗi service lại trả lỗi theo một kiểu khác nhau mà
 * không ai nhận ra.
 *
 * <p>Cách vá phổ biến là thêm {@code @ComponentScan("com.hunre")} vào từng service,
 * nhưng như vậy service nào quên là hỏng, và việc quét rộng ra dễ kéo theo bean không
 * mong muốn. Auto-configuration là cách Spring Boot dành riêng cho tình huống này:
 * lớp được liệt kê trong {@code META-INF/spring/....AutoConfiguration.imports} và
 * Boot tự nạp, service không phải khai báo gì.
 *
 * <p>Điều kiện áp dụng:
 * <ul>
 *   <li>{@code @ConditionalOnWebApplication(SERVLET)} - chỉ đăng ký cho service chạy
 *       Spring MVC. api-gateway chạy WebFlux nên dù có lỡ thêm shared-common vào cũng
 *       không bị dính handler của Spring MVC.</li>
 *   <li>{@code @ConditionalOnMissingBean} - service nào muốn tự viết handler riêng thì
 *       chỉ cần khai báo bean cùng kiểu, bean mặc định này sẽ tự nhường chỗ.</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SharedCommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
