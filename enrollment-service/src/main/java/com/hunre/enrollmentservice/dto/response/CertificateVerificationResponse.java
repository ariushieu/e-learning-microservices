package com.hunre.enrollmentservice.dto.response;

import java.time.Instant;

/** Danh sách trắng các trường công khai: không serialize entity hay response của chủ sở hữu. */
public record CertificateVerificationResponse(String learnerName, String courseTitle,
                                              Instant issuedAt, String certificateCode) { }
