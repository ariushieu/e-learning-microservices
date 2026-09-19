package com.hunre.enrollmentservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enrollment_id", nullable = false, unique = true)
    private Long enrollmentId;

    @Column(name = "certificate_code", nullable = false, unique = true, length = 50)
    private String certificateCode;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @PrePersist
    public void prePersist() {
        if (issuedAt == null) {
            issuedAt = Instant.now();
        }
    }
}
