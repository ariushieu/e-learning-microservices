package com.hunre.courseservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Hàng khóa dùng chung giữa các instance để tránh gửi snapshot ngược thứ tự. */
@Entity
@Table(name = "outbox_dispatch_lock")
@Getter
@NoArgsConstructor
public class OutboxDispatchLock {
    @Id
    private Long id;
}
