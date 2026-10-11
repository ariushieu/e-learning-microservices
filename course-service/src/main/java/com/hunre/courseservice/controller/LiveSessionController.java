package com.hunre.courseservice.controller;

import com.hunre.courseservice.dto.request.RoomChatRequest;
import com.hunre.courseservice.dto.request.RoomPeerRequest;
import com.hunre.courseservice.dto.request.RoomSignalRequest;
import com.hunre.courseservice.dto.request.RoomStateRequest;
import com.hunre.courseservice.dto.request.ScheduleLiveSessionRequest;
import com.hunre.courseservice.dto.response.LiveAttendanceResponse;
import com.hunre.courseservice.dto.response.LiveSessionResponse;
import com.hunre.courseservice.live.LiveRoomRegistry;
import com.hunre.courseservice.security.CurrentUserProvider;
import com.hunre.courseservice.service.LiveSessionService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/** Lớp học trực tuyến: lịch học, vòng đời buổi học và phòng học (SSE + các lệnh POST). */
@RestController
@RequiredArgsConstructor
public class LiveSessionController {
    private final LiveSessionService service;
    private final LiveRoomRegistry rooms;
    private final CurrentUserProvider currentUser;

    // GET khóa học công khai ở gateway, nên tự đọc danh tính; service trả 401 nếu không có.
    @GetMapping("/api/courses/{courseId}/live-sessions")
    public ApiResponse<List<LiveSessionResponse>> list(@PathVariable Long courseId) {
        return ApiResponse.ok(service.list(courseId, currentUser.getCurrentUser().orElse(null)));
    }

    @PostMapping("/api/courses/{courseId}/live-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LiveSessionResponse> schedule(@PathVariable Long courseId,
            @Valid @RequestBody ScheduleLiveSessionRequest request, AuthenticatedUser user) {
        return ApiResponse.ok(service.schedule(courseId, request, user), "Đã lên lịch buổi học trực tuyến");
    }

    @GetMapping("/api/live-sessions/{id}")
    public ApiResponse<LiveSessionResponse> get(@PathVariable Long id, AuthenticatedUser user) {
        return ApiResponse.ok(service.get(id, user));
    }

    @PostMapping("/api/live-sessions/{id}/start")
    public ApiResponse<LiveSessionResponse> start(@PathVariable Long id, AuthenticatedUser user) {
        return ApiResponse.ok(service.start(id, user), "Buổi học đã bắt đầu");
    }

    @PostMapping("/api/live-sessions/{id}/end")
    public ApiResponse<LiveSessionResponse> end(@PathVariable Long id, AuthenticatedUser user) {
        return ApiResponse.ok(service.end(id, user), "Đã kết thúc buổi học");
    }

    @PostMapping("/api/live-sessions/{id}/cancel")
    public ApiResponse<LiveSessionResponse> cancel(@PathVariable Long id, AuthenticatedUser user) {
        return ApiResponse.ok(service.cancel(id, user), "Đã hủy buổi học");
    }

    @GetMapping("/api/live-sessions/{id}/attendance")
    public ApiResponse<List<LiveAttendanceResponse>> attendance(@PathVariable Long id, AuthenticatedUser user) {
        return ApiResponse.ok(service.attendance(id, user));
    }

    /**
     * Vào phòng: luồng Server-Sent Events, xem {@link LiveRoomRegistry} về các loại sự kiện.
     * {@code mic}, {@code cam}: trạng thái lúc vào, để người khác thấy đúng ngay từ đầu.
     */
    @GetMapping("/api/live-sessions/{id}/room")
    public SseEmitter room(@PathVariable Long id, AuthenticatedUser user,
                           @RequestParam(defaultValue = "true") boolean mic,
                           @RequestParam(defaultValue = "true") boolean cam) {
        return service.openRoom(id, user, mic, cam);
    }

    @PostMapping("/api/live-sessions/{id}/room/signal")
    public ApiResponse<Void> signal(@PathVariable Long id, @Valid @RequestBody RoomSignalRequest request,
                                    AuthenticatedUser user) {
        rooms.relay(id, user, request.from(), request.messages().stream()
                .map(m -> new LiveRoomRegistry.Signal(m.to(), m.type(), m.data())).toList());
        return ApiResponse.message("OK");
    }

    @PostMapping("/api/live-sessions/{id}/room/state")
    public ApiResponse<Void> state(@PathVariable Long id, @Valid @RequestBody RoomStateRequest request,
                                   AuthenticatedUser user) {
        rooms.updateState(id, user, request.from(), request.mic(), request.cam(), request.hand(), request.screen());
        return ApiResponse.message("OK");
    }

    @PostMapping("/api/live-sessions/{id}/room/chat")
    public ApiResponse<Void> chat(@PathVariable Long id, @Valid @RequestBody RoomChatRequest request,
                                  AuthenticatedUser user) {
        rooms.chat(id, user, request.from(), request.text());
        return ApiResponse.message("OK");
    }

    @PostMapping("/api/live-sessions/{id}/room/mute")
    public ApiResponse<Void> mute(@PathVariable Long id, @Valid @RequestBody RoomPeerRequest request,
                                  AuthenticatedUser user) {
        rooms.requestMute(id, user, request.from(), request.target());
        return ApiResponse.message("Đã nhờ học viên tắt micro");
    }

    @PostMapping("/api/live-sessions/{id}/room/leave")
    public ApiResponse<Void> leave(@PathVariable Long id, @Valid @RequestBody RoomPeerRequest request,
                                   AuthenticatedUser user) {
        rooms.leave(id, user, request.from());
        return ApiResponse.message("Đã rời phòng");
    }
}
