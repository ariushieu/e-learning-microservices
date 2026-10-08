package com.hunre.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class LoginRequest {

    public LoginRequest(String email, String password) {
        setEmail(email);
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}
