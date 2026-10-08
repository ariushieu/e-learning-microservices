package com.hunre.authservice.dto;

/** Shared phone contract for registration and profile updates. */
public final class PhoneConstraints {
    public static final String REGEXP = " *(?:\\+?[0-9](?: ?[0-9]){8,14})? *";
    public static final String MESSAGE = "Số điện thoại phải có 9–15 chữ số, có thể bắt đầu bằng + và cách nhau bằng một khoảng trắng";

    private PhoneConstraints() {
    }
}
