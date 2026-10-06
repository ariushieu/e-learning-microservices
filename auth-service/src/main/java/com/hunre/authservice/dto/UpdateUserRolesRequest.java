package com.hunre.authservice.dto;

import com.hunre.authservice.domain.RoleCode;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRolesRequest {

    @NotEmpty(message = "Danh sách vai trò không được để trống")
    private Set<RoleCode> roles;
}
