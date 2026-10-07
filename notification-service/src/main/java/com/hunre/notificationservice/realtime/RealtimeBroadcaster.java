package com.hunre.notificationservice.realtime;

/**
 * Đưa một sự kiện tới mọi luồng SSE của người dùng, dù luồng đó nằm ở bản service nào.
 */
public interface RealtimeBroadcaster {

    /**
     * @param userId người nhận
     * @param event  tên sự kiện SSE, xem {@link InboxStreamService}
     * @param json   phần {@code data} đã ở dạng JSON
     */
    void broadcast(Long userId, String event, String json);
}
