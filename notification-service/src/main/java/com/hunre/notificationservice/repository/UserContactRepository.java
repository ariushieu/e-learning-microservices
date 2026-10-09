package com.hunre.notificationservice.repository;

import com.hunre.notificationservice.entity.UserContact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserContactRepository extends JpaRepository<UserContact, Long> {
}
