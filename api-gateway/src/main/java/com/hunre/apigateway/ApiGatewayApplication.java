package com.hunre.apigateway;

import com.hunre.sharedcommon.security.JwtSecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Auto-configuration của shared-common chỉ chạy cho ứng dụng servlet, mà gateway là
 * reactive, nên {@link JwtSecurityProperties} phải được bật thủ công ở đây.
 */
@SpringBootApplication
@EnableConfigurationProperties(JwtSecurityProperties.class)
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

}
