package com.hunre.enrollmentservice.service;

import com.hunre.enrollmentservice.entity.Certificate;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/** Chỉ ghi tên từ JWT đã xác thực của chủ chứng chỉ, không lấy từ body hay query. */
@Service
@RequiredArgsConstructor
public class CertificateDetailsService {
    private final ObjectProvider<HttpServletRequest> requests;

    public void captureIfMissing(Certificate certificate, Long ownerId, String courseTitle) {
        if (certificate.getLearnerName() == null) {
            var request = requests.getIfAvailable();
            Object identity = request == null ? null : request.getAttribute(JwtAuthenticationFilter.USER_ATTRIBUTE);
            if (!(identity instanceof AuthenticatedUser user) || !ownerId.equals(user.userId())
                    || user.fullName() == null || user.fullName().isBlank() || user.fullName().length() > 150) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED,
                        "Vui lòng đăng nhập lại để xác nhận tên trên chứng chỉ");
            }
            certificate.setLearnerName(user.fullName().strip());
        }
        if (certificate.getCourseTitle() == null) {
            certificate.setCourseTitle(courseTitle);
        }
    }
}
