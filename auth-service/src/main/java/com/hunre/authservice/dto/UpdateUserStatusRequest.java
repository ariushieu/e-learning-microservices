package com.hunre.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserStatusRequest {
    @NotBlank(message = "Trạng thái không được để trống")
    @Pattern(regexp = "ACTIVE|LOCKED", message = "Trạng thái phải là ACTIVE hoặc LOCKED")
    private String status;
}
