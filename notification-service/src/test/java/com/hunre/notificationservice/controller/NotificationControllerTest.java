package com.hunre.notificationservice.controller;

import com.hunre.notificationservice.dto.NotificationResponse;
import com.hunre.notificationservice.service.NotificationService;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.exception.GlobalExceptionHandler;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.AuthenticatedUserArgumentResolver;
import com.hunre.sharedcommon.security.JwtAuthenticationFilter;
import com.hunre.sharedcommon.security.Roles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerTest {

    private MockMvc mockMvc;
    private NotificationService notificationService;

    private final AuthenticatedUser user = new AuthenticatedUser(
            42L, "sv@hunre.edu.vn", "Nguyễn Văn A", Set.of(Roles.STUDENT));

    @BeforeEach
    void setUp() {
        notificationService = Mockito.mock(NotificationService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new NotificationController(notificationService))
                .setCustomArgumentResolvers(
                        new AuthenticatedUserArgumentResolver(),
                        new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("hộp thư lấy theo người trong token, không theo tham số client gửi")
    void layTheoNguoiTrongToken() throws Exception {
        NotificationResponse item = new NotificationResponse(
                1L, "QUIZ_GRADED", "Đã có kết quả", "Bạn đạt 85.50 điểm.",
                null, false, Instant.now(), null);

        when(notificationService.getMyNotifications(eq(42L), any()))
                .thenReturn(PageResponse.of(List.of(item), 0, 20, 1));

        // Gửi kèm userId=999 để chứng minh nó bị bỏ qua hoàn toàn.
        mockMvc.perform(get("/api/notifications?userId=999")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].type").value("QUIZ_GRADED"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        // Nếu controller đọc userId từ query thì lần gọi này đã là 999.
        Mockito.verify(notificationService).getMyNotifications(eq(42L), any());
    }

    @Test
    @DisplayName("không có token thì trả 401 chứ không trả hộp thư rỗng")
    void khongCoToken() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("đếm số thông báo chưa đọc")
    void demChuaDoc() throws Exception {
        when(notificationService.countUnread(42L)).thenReturn(3L);

        mockMvc.perform(get("/api/notifications/unread-count")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    @DisplayName("đánh dấu đã đọc trả về thông báo đã cập nhật")
    void danhDauDaDoc() throws Exception {
        NotificationResponse read = new NotificationResponse(
                1L, "QUIZ_GRADED", "Đã có kết quả", "Bạn đạt 85.50 điểm.",
                null, true, Instant.now(), Instant.now());

        when(notificationService.markRead(1L, 42L)).thenReturn(read);

        mockMvc.perform(patch("/api/notifications/1/read")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true))
                .andExpect(jsonPath("$.message").value("Đã đánh dấu đã đọc"));
    }

    @Test
    @DisplayName("đọc thông báo của người khác trả 404, không phải 403")
    void khongDocDuocCuaNguoiKhac() throws Exception {
        // Service tìm theo cả id lẫn userId nên thông báo của người khác là "không tìm thấy".
        // Trả 403 sẽ tiết lộ rằng id đó có tồn tại.
        when(notificationService.markRead(99L, 42L))
                .thenThrow(new ResourceNotFoundException("thông báo", "id", 99L));

        mockMvc.perform(patch("/api/notifications/99/read")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, user))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
