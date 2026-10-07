package com.hunre.notificationservice.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {

    @Test
    @DisplayName("readAt chỉ tới micro giây: response lần đọc đầu khớp giá trị cột DATETIME(6) lưu")
    void readAtKhopDoChinhXacCuaCot() {
        Notification notification = Notification.builder().build();

        notification.markRead();
        Instant first = notification.getReadAt();
        notification.markRead();

        assertThat(first.getNano() % 1_000).isZero();
        assertThat(notification.getReadAt()).isEqualTo(first);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.READ);
    }
}
