package com.hunre.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {
    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 150, message = "Họ và tên không được vượt quá 150 ký tự")
    private String fullName;

    @Pattern(regexp = "(?: *|[0-9+ ]{9,15})",
            message = "Số điện thoại phải có từ 9 đến 15 ký tự, chỉ gồm chữ số, dấu + và khoảng trắng")
    private String phone;
}
