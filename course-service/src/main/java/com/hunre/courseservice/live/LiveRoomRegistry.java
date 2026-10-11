package com.hunre.courseservice.live;

import com.hunre.courseservice.repository.LiveAttendanceRepository;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Các phòng học trực tuyến đang mở trên <b>bản service này</b>.
 *
 * <p>Mỗi người vào phòng giữ một luồng SSE. Máy chủ không đụng tới hình và tiếng: trình duyệt
 * gửi thẳng cho nhau bằng WebRTC, ở đây chỉ chuyển các tin báo hiệu (offer, answer, ICE
 * candidate) để chúng tìm thấy nhau, cùng với chat, giơ tay và trạng thái micro/camera.
 *
 * <p>Sự kiện gửi xuống, {@code data} đều là JSON:
 * <ul>
 *   <li>{@code welcome}: mã kết nối của chính mình, những người đang trong phòng, chat gần đây.
 *       Người mới vào là người gửi offer cho từng người có sẵn, nên hai bên không gửi offer cùng lúc.</li>
 *   <li>{@code peer-joined}, {@code peer-updated}, {@code peer-left}: thay đổi danh sách người.</li>
 *   <li>{@code signal}: tin báo hiệu WebRTC từ một người khác.</li>
 *   <li>{@code chat}, {@code mute-request} (giảng viên nhờ tắt micro), {@code ended} (buổi học kết thúc).</li>
 * </ul>
 *
 * <p>Phòng chỉ nằm trong bộ nhớ, nên mọi người phải nối vào cùng một bản course-service. Chạy
 * nhiều bản thì phải chuyển tin qua Redis như luồng thông báo của notification-service.
 */
@Component
public class LiveRoomRegistry {
    public static final int MAX_PARTICIPANTS = 12;
    static final int CHAT_HISTORY = 100;

    private static final Logger log = LoggerFactory.getLogger(LiveRoomRegistry.class);

    private final Map<Long, Room> rooms = new ConcurrentHashMap<>();
    private final ObjectMapper json;
    private final LiveAttendanceRepository attendance;
    private final long streamTimeoutMillis;
    private final ScheduledExecutorService heartbeat;

    public LiveRoomRegistry(ObjectMapper json, LiveAttendanceRepository attendance,
                            @Value("${elearning.live.stream-timeout:4h}") Duration streamTimeout,
                            @Value("${elearning.live.heartbeat-interval:15s}") Duration heartbeatInterval) {
        this.json = json;
        this.attendance = attendance;
        this.streamTimeoutMillis = streamTimeout.toMillis();
        this.heartbeat = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "live-room-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        long every = heartbeatInterval.toMillis();
        heartbeat.scheduleWithFixedDelay(this::ping, every, every, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    void shutdown() {
        heartbeat.shutdownNow();
    }

    /** Vào phòng: trả luồng SSE, sự kiện đầu tiên là {@code welcome}. */
    public SseEmitter join(Long sessionId, AuthenticatedUser user, boolean host, boolean mic, boolean cam) {
        Room room = rooms.computeIfAbsent(sessionId, Room::new);
        SseEmitter emitter = new SseEmitter(streamTimeoutMillis);
        Peer peer = new Peer(UUID.randomUUID().toString(), user.userId(), displayName(user), host, emitter);
        peer.mic = mic;
        peer.cam = cam;
        synchronized (room) {
            if (room.closed) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED, "Buổi học đã kết thúc");
            }
            if (room.peers.size() >= MAX_PARTICIPANTS) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                        "Phòng học đã đủ " + MAX_PARTICIPANTS + " người, thử lại sau");
            }
            boolean firstTab = room.peers.values().stream().noneMatch(p -> p.userId.equals(peer.userId));
            room.peers.put(peer.id, peer);
            if (firstTab) room.presentSince.put(peer.userId, Instant.now());

            Map<String, Object> welcome = new LinkedHashMap<>();
            welcome.put("peerId", peer.id);
            welcome.put("participants", room.peers.values().stream().filter(p -> p != peer).map(Peer::view).toList());
            welcome.put("chat", List.copyOf(room.chat));
            send(room, peer, "welcome", welcome);
            broadcast(room, "peer-joined", peer.view(), peer);
        }
        record(() -> attendance.recordJoin(sessionId, peer.userId, peer.name, host, Instant.now()));

        // Tab đóng hoặc mạng rớt: Tomcat báo lỗi/kết thúc thì gỡ người đó khỏi phòng.
        emitter.onCompletion(() -> leave(room, peer.id, false));
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> leave(room, peer.id, false));
        return emitter;
    }

    /** Chuyển từng tin báo hiệu tới đúng người nhận; người đã rời phòng thì bỏ qua. */
    public void relay(Long sessionId, AuthenticatedUser user, String from, List<Signal> signals) {
        Room room = requireRoom(sessionId);
        synchronized (room) {
            Peer sender = requirePeer(room, user, from);
            for (Signal signal : signals) {
                Peer target = room.peers.get(signal.to());
                if (target == null || target == sender) continue;
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("from", sender.id);
                body.put("type", signal.type());
                body.put("data", signal.data());
                send(room, target, "signal", body);
            }
        }
    }

    public void updateState(Long sessionId, AuthenticatedUser user, String from,
                            boolean mic, boolean cam, boolean hand, boolean screen) {
        Room room = requireRoom(sessionId);
        synchronized (room) {
            Peer peer = requirePeer(room, user, from);
            peer.mic = mic;
            peer.cam = cam;
            peer.hand = hand;
            peer.screen = screen;
            broadcast(room, "peer-updated", peer.view(), null);
        }
    }

    public void chat(Long sessionId, AuthenticatedUser user, String from, String text) {
        Room room = requireRoom(sessionId);
        synchronized (room) {
            Peer peer = requirePeer(room, user, from);
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("id", ++room.chatSequence);
            message.put("peerId", peer.id);
            message.put("userId", peer.userId);
            message.put("name", peer.name);
            message.put("host", peer.host);
            message.put("text", text.strip());
            message.put("sentAt", Instant.now());
            room.chat.addLast(message);
            while (room.chat.size() > CHAT_HISTORY) room.chat.removeFirst();
            // Gửi cả người viết: thấy tin hiện ra nghĩa là máy chủ đã nhận.
            broadcast(room, "chat", message, null);
        }
    }

    /** Giảng viên nhờ một người tắt micro; trình duyệt người đó tự tắt rồi báo lại trạng thái. */
    public void requestMute(Long sessionId, AuthenticatedUser user, String from, String targetId) {
        Room room = requireRoom(sessionId);
        synchronized (room) {
            Peer sender = requirePeer(room, user, from);
            if (!sender.host) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ giảng viên được tắt micro của người khác");
            }
            Peer target = room.peers.get(targetId);
            if (target == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Người này đã rời phòng");
            }
            send(room, target, "mute-request", Map.of("by", sender.name));
        }
    }

    /** Rời phòng chủ động (bấm "Rời lớp"); đóng tab thì {@link #join} tự gỡ qua callback của luồng. */
    public void leave(Long sessionId, AuthenticatedUser user, String from) {
        Room room = rooms.get(sessionId);
        if (room == null) return;
        Peer peer;
        synchronized (room) {
            peer = room.peers.get(from);
            if (peer == null) return;
            if (!peer.userId.equals(user.userId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "Đây không phải kết nối của bạn");
            }
        }
        leave(room, from, true);
    }

    /** Kết thúc buổi học: báo mọi người, đóng mọi luồng, chốt thời gian có mặt. */
    public void close(Long sessionId) {
        Room room = rooms.remove(sessionId);
        if (room == null) return;
        List<Peer> peers;
        synchronized (room) {
            room.closed = true;
            broadcast(room, "ended", Map.of("sessionId", sessionId), null);
            peers = new ArrayList<>(room.peers.values());
            for (Peer peer : peers) removeLocked(room, peer);
        }
        peers.forEach(peer -> complete(peer.emitter));
    }

    /** Số kết nối đang ở trong phòng. */
    public int participantCount(Long sessionId) {
        Room room = rooms.get(sessionId);
        if (room == null) return 0;
        synchronized (room) {
            return room.peers.size();
        }
    }

    /** Dòng chú thích giữ luồng sống qua proxy, và ghi thất bại cho biết tab đã đóng. */
    void ping() {
        for (Room room : rooms.values()) {
            synchronized (room) {
                List<Peer> dead = new ArrayList<>();
                for (Peer peer : room.peers.values()) {
                    if (!trySend(peer, SseEmitter.event().comment("ping"))) dead.add(peer);
                }
                dead.forEach(peer -> removeLocked(room, peer));
            }
        }
    }

    private void leave(Room room, String peerId, boolean completeStream) {
        Peer peer;
        synchronized (room) {
            peer = room.peers.get(peerId);
            if (peer == null) return;
            removeLocked(room, peer);
        }
        if (completeStream) complete(peer.emitter);
    }

    /** Gỡ một người khi đang giữ khóa phòng. Tab cuối cùng của người đó đóng thì ghi thời gian có mặt. */
    private void removeLocked(Room room, Peer peer) {
        if (room.peers.remove(peer.id) == null) return;
        if (!room.closed) broadcast(room, "peer-left", Map.of("peerId", peer.id), null);
        boolean lastTab = room.peers.values().stream().noneMatch(p -> p.userId.equals(peer.userId));
        if (lastTab) {
            Instant since = room.presentSince.remove(peer.userId);
            Instant now = Instant.now();
            long seconds = since == null ? 0 : Duration.between(since, now).toSeconds();
            record(() -> attendance.recordLeave(room.sessionId, peer.userId, seconds, now));
        }
    }

    private void broadcast(Room room, String event, Object body, Peer except) {
        String data = json.writeValueAsString(body);
        List<Peer> dead = new ArrayList<>();
        for (Peer peer : room.peers.values()) {
            if (peer == except) continue;
            if (!trySend(peer, SseEmitter.event().name(event).data(data))) dead.add(peer);
        }
        dead.forEach(peer -> removeLocked(room, peer));
    }

    private void send(Room room, Peer peer, String event, Object body) {
        if (!trySend(peer, SseEmitter.event().name(event).data(json.writeValueAsString(body)))) {
            removeLocked(room, peer);
        }
    }

    private static boolean trySend(Peer peer, SseEmitter.SseEventBuilder event) {
        try {
            peer.emitter.send(event);
            return true;
        } catch (IOException | IllegalStateException e) {
            log.debug("Gỡ kết nối {} khỏi phòng: {}", peer.id, e.getMessage());
            return false;
        }
    }

    private static void complete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (RuntimeException e) {
            log.debug("Luồng đã đóng: {}", e.getMessage());
        }
    }

    private static void record(Runnable write) {
        try {
            write.run();
        } catch (RuntimeException e) {
            // Mất một dòng điểm danh không đáng để làm hỏng buổi học đang diễn ra.
            log.warn("Không ghi được điểm danh buổi học: {}", e.getMessage());
        }
    }

    private Room requireRoom(Long sessionId) {
        Room room = rooms.get(sessionId);
        if (room == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Phòng học không mở hoặc đã kết thúc");
        }
        return room;
    }

    private static Peer requirePeer(Room room, AuthenticatedUser user, String peerId) {
        Peer peer = room.peers.get(peerId);
        if (peer == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Bạn không còn ở trong phòng, hãy vào lại");
        }
        if (!peer.userId.equals(user.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Đây không phải kết nối của bạn");
        }
        return peer;
    }

    private static String displayName(AuthenticatedUser user) {
        String name = user.fullName();
        return name == null || name.isBlank() ? "Người dùng " + user.userId() : name.strip();
    }

    /** Một tin báo hiệu, {@code data} giữ nguyên JSON trình duyệt gửi lên. */
    public record Signal(String to, String type, JsonNode data) {}

    /** Thông tin một người trong phòng mà người khác được thấy. */
    public record ParticipantView(String peerId, Long userId, String name, boolean host,
                                  boolean mic, boolean cam, boolean hand, boolean screen) {}

    private static final class Room {
        final Long sessionId;
        final Map<String, Peer> peers = new LinkedHashMap<>();
        final Map<Long, Instant> presentSince = new HashMap<>();
        final Deque<Map<String, Object>> chat = new ArrayDeque<>();
        long chatSequence;
        boolean closed;

        Room(Long sessionId) {
            this.sessionId = sessionId;
        }
    }

    private static final class Peer {
        final String id;
        final Long userId;
        final String name;
        final boolean host;
        final SseEmitter emitter;
        boolean mic = true;
        boolean cam = true;
        boolean hand;
        boolean screen;

        Peer(String id, Long userId, String name, boolean host, SseEmitter emitter) {
            this.id = id;
            this.userId = userId;
            this.name = name;
            this.host = host;
            this.emitter = emitter;
        }

        ParticipantView view() {
            return new ParticipantView(id, userId, name, host, mic, cam, hand, screen);
        }
    }
}
