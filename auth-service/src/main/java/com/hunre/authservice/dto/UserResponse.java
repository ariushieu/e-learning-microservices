package com.hunre.authservice.dto;

import com.hunre.authservice.domain.Role;
import com.hunre.authservice.domain.User;
import com.hunre.authservice.domain.UserStatus;
import lombok.*;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private UserStatus status;
    private List<String> roles;
    private Instant createdAt;

    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }
        List<String> roles = user.getRoles() != null
                ? user.getRoles().stream().map(Role::getCode).map(Enum::name).toList()
                : Collections.emptyList();

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
