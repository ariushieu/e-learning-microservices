package com.hunre.notificationservice.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Nghe kênh Redis của {@link RedisRealtimeBroadcaster}, đăng ký ở luồng nền.
 *
 * <p>Không khai {@code RedisMessageListenerContainer} thành bean cho Spring tự bật: lần đăng ký
 * đầu tiên mà Redis chưa lên thì container ném lỗi và kéo cả service không khởi động được —
 * mất phần "tức thời" thành mất luôn hộp thư. Ở đây đăng ký hỏng thì thử lại sau
 * {@link #RETRY_DELAY}. Đã đăng ký được rồi thì mất kết nối giữa chừng do container tự nối lại.
 */
class RedisSubscription implements SmartLifecycle {

    static final Duration RETRY_DELAY = Duration.ofSeconds(5);

    private static final Logger log = LoggerFactory.getLogger(RedisSubscription.class);

    private final RedisConnectionFactory connectionFactory;
    private final RedisRealtimeBroadcaster listener;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "realtime-redis-subscribe");
        thread.setDaemon(true);
        return thread;
    });

    private volatile RedisMessageListenerContainer container;
    private volatile boolean running;

    RedisSubscription(RedisConnectionFactory connectionFactory, RedisRealtimeBroadcaster listener) {
        this.connectionFactory = connectionFactory;
        this.listener = listener;
    }

    @Override
    public void start() {
        running = true;
        scheduler.execute(this::subscribe);
    }

    @Override
    public void stop() {
        running = false;
        scheduler.shutdownNow();
        RedisMessageListenerContainer current = container;
        if (current != null) {
            destroyQuietly(current);
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /** Đã nghe được kênh chưa, cho test. */
    boolean isSubscribed() {
        RedisMessageListenerContainer current = container;
        return current != null && current.isListening();
    }

    private void subscribe() {
        if (!running) {
            return;
        }
        // Mỗi lần thử một container mới: container vừa hỏng lúc start không chắc còn dùng lại được.
        RedisMessageListenerContainer candidate = new RedisMessageListenerContainer();
        candidate.setConnectionFactory(connectionFactory);
        candidate.addMessageListener(listener, new ChannelTopic(RedisRealtimeBroadcaster.CHANNEL));
        try {
            candidate.afterPropertiesSet();
            candidate.start();
            container = candidate;
            log.info("Đã nghe kênh Redis {}", RedisRealtimeBroadcaster.CHANNEL);
        } catch (RuntimeException e) {
            destroyQuietly(candidate);
            log.warn("Chưa nghe được kênh Redis ({}), thử lại sau {} giây",
                    e.getMessage(), RETRY_DELAY.toSeconds());
            if (running) {
                scheduler.schedule(this::subscribe, RETRY_DELAY.toMillis(), TimeUnit.MILLISECONDS);
            }
        }
    }

    private static void destroyQuietly(RedisMessageListenerContainer target) {
        try {
            target.destroy();
        } catch (Exception e) {
            log.debug("Đóng container Redis lỗi: {}", e.getMessage());
        }
    }
}
