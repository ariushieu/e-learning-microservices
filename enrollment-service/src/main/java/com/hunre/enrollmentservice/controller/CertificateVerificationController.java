package com.hunre.enrollmentservice.controller;

import com.hunre.enrollmentservice.dto.response.CertificateVerificationResponse;
import com.hunre.enrollmentservice.repository.CertificateRepository;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateVerificationController {
    private final CertificateRepository certificates;

    @GetMapping("/verify/{code}")
    public ResponseEntity<ApiResponse<CertificateVerificationResponse>> verify(@PathVariable String code) {
        if (code.length() > 40 || !code.matches("CERT-[A-Za-z0-9-]+")) {
            throw new ResourceNotFoundException("Không tìm thấy chứng chỉ");
        }
        var certificate = certificates.findByCertificateCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chứng chỉ"));
        if (certificate.getLearnerName() == null || certificate.getCourseTitle() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Chứng chỉ chưa có đủ thông tin xác minh. Chủ sở hữu vui lòng mở lại trang chứng chỉ sau khi đăng nhập.");
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(
                new CertificateVerificationResponse(certificate.getLearnerName(), certificate.getCourseTitle(),
                        certificate.getIssuedAt(), certificate.getCertificateCode())));
    }
}
