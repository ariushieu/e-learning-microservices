package com.hunre.authservice.exception;

import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;

/** Only this rejection commits its audit event; unrelated login failures still roll back. */
public class IncorrectLoginPasswordException extends BusinessException {
    public IncorrectLoginPasswordException() {
        super(ErrorCode.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác");
    }
}
