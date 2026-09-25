package com.hunre.authservice.controller;

import com.hunre.authservice.domain.RoleCode;
import com.hunre.authservice.domain.UserStatus;
import com.hunre.authservice.dto.UserResponse;
import com.hunre.authservice.service.AuthService;
import com.hunre.sharedcommon.exception.GlobalExceptionHandler;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.AuthenticatedUserArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static com.hunre.sharedcommon.security.JwtAuthenticationFilter.USER_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private UserController userController;

    private static final AuthenticatedUser ADMIN_USER = new AuthenticatedUser(
            1L, "admin@elearning.hunre.edu.vn", "Administrator",
            Set.of("ROLE_ADMIN"));

    private static final AuthenticatedUser STUDENT_USER = new AuthenticatedUser(
            2L, "student@hunre.edu.vn", "Nguyen Van A",
            Set.of("ROLE_STUDENT"));

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticatedUserArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("PATCH /api/users/{id}/roles - Admin cấp ROLE_INSTRUCTOR thành công (200 OK)")
    void updateRoles_adminGrantsInstructor_success() throws Exception {
        UserResponse updated = UserResponse.builder()
                .id(2L)
                .email("student@hunre.edu.vn")
                .fullName("Nguyen Van A")
                .status(UserStatus.ACTIVE)
                .roles(List.of("ROLE_STUDENT", "ROLE_INSTRUCTOR"))
                .build();

        when(authService.updateUserRoles(anyLong(), any())).thenReturn(updated);

        String body = """
                { "roles": ["ROLE_STUDENT", "ROLE_INSTRUCTOR"] }
                """;

        mockMvc.perform(patch("/api/users/2/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .requestAttr(USER_ATTRIBUTE, ADMIN_USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cập nhật vai trò thành công"))
                .andExpect(jsonPath("$.data.roles").isArray())
                .andExpect(jsonPath("$.data.roles.length()").value(2));
    }

    @Test
    @DisplayName("PATCH /api/users/{id}/roles - Student gọi bị chặn 403 FORBIDDEN")
    void updateRoles_studentCaller_returns403() throws Exception {
        String body = """
                { "roles": ["ROLE_ADMIN"] }
                """;

        mockMvc.perform(patch("/api/users/99/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .requestAttr(USER_ATTRIBUTE, STUDENT_USER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("PATCH /api/users/{id}/roles - Admin tự gỡ ROLE_ADMIN của chính mình bị chặn 422 BUSINESS_RULE_VIOLATED")
    void updateRoles_adminSelfDemote_throwsBusinessRuleViolated() throws Exception {
        String body = """
                { "roles": ["ROLE_STUDENT"] }
                """;

        mockMvc.perform(patch("/api/users/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .requestAttr(USER_ATTRIBUTE, ADMIN_USER))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATED"))
                .andExpect(jsonPath("$.message").value("Không thể tự gỡ quyền quản trị của chính mình"));
    }

    @Test
    @DisplayName("PATCH /api/users/{id}/roles - User không tồn tại trả về 404 NOT_FOUND")
    void updateRoles_userNotFound_returns404() throws Exception {
        when(authService.updateUserRoles(anyLong(), any()))
                .thenThrow(new ResourceNotFoundException("người dùng", "id", 999L));

        String body = """
                { "roles": ["ROLE_STUDENT"] }
                """;

        mockMvc.perform(patch("/api/users/999/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .requestAttr(USER_ATTRIBUTE, ADMIN_USER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("PATCH /api/users/{id}/roles - roles để trống trả về 400 VALIDATION_FAILED")
    void updateRoles_emptyRoles_returns400() throws Exception {
        String body = """
                { "roles": [] }
                """;

        mockMvc.perform(patch("/api/users/2/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .requestAttr(USER_ATTRIBUTE, ADMIN_USER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
