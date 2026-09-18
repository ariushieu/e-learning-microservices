package com.hunre.sharedcommon.security;

/**
 * Mã vai trò, khớp với bảng {@code roles} trong auth_db (xem {@code V2__seed_roles.sql}).
 *
 * <p>Để ở đây vì cả 5 service đều phải hiểu giống nhau: quiz-service cần biết
 * {@code ROLE_INSTRUCTOR} là ai thì mới chặn được học viên xem đáp án. Viết chuỗi
 * thẳng trong từng service thì chỉ cần một lần gõ nhầm là mất tác dụng chặn mà
 * không có lỗi biên dịch nào báo.
 */
public final class Roles {

    public static final String STUDENT = "ROLE_STUDENT";
    public static final String INSTRUCTOR = "ROLE_INSTRUCTOR";
    public static final String ADMIN = "ROLE_ADMIN";

    private Roles() {
    }
}
