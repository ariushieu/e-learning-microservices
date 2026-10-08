package com.hunre.courseservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** URL tuyệt đối dùng cho liên kết tài liệu, không cho phép giao thức thực thi mã. */
@Target(FIELD)
@Retention(RUNTIME)
@Constraint(validatedBy = HttpUrlValidator.class)
public @interface HttpUrl {
    String message() default "URL tài liệu phải là địa chỉ http:// hoặc https:// hợp lệ";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
