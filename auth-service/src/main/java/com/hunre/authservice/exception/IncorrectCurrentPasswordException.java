package com.hunre.authservice.exception;

import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;

public class IncorrectCurrentPasswordException extends BusinessException {
    public IncorrectCurrentPasswordException() {
        super(ErrorCode.VALIDATION_FAILED, "Mật khẩu hiện tại không chính xác");
    }
}
