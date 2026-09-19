package com.hunre.enrollmentservice.dto.response;

import com.hunre.enrollmentservice.entity.Certificate;
import lombok.Builder;

import java.time.Instant;

@Builder
public record CertificateResponse(
        Long id,
        Long enrollmentId,
        Long userId,
        Long courseId,
        String courseTitle,
        String certificateCode,
        String fileUrl,
        Instant issuedAt
) {
    public static CertificateResponse from(Certificate certificate, Long userId, Long courseId, String courseTitle) {
        return CertificateResponse.builder()
                .id(certificate.getId())
                .enrollmentId(certificate.getEnrollmentId())
                .userId(userId)
                .courseId(courseId)
                .courseTitle(courseTitle)
                .certificateCode(certificate.getCertificateCode())
                .fileUrl(certificate.getFileUrl())
                .issuedAt(certificate.getIssuedAt())
                .build();
    }
}
