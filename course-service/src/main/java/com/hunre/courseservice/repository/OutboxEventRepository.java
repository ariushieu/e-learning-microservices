package com.hunre.courseservice.repository;

import com.hunre.courseservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop50ByPublishedAtIsNullOrderByIdAsc();
}
