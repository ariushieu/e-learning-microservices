package com.hunre.enrollmentservice.security;

import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.Roles;

public final class EnrollmentPermissions {
    private EnrollmentPermissions() {}

    public static void requireStudent(AuthenticatedUser user) {
        if (!user.hasRole(Roles.STUDENT)) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Cần vai trò học viên để ghi danh và cập nhật tiến độ học của mình");
        }
    }
}
