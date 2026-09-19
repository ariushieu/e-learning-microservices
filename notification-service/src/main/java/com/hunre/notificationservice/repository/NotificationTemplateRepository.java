package com.hunre.notificationservice.repository;

import com.hunre.notificationservice.entity.NotificationChannel;
import com.hunre.notificationservice.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByCodeAndChannelAndActiveTrue(
            String code, NotificationChannel channel);
}
