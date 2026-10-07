package com.hunre.quizservice.exception;

import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;

/** A rejected submission whose EXPIRED state must still be committed. */
public class AttemptExpiredException extends BusinessException {
    public AttemptExpiredException() {
        super(ErrorCode.BUSINESS_RULE_VIOLATED,
                "Thời gian làm bài đã kết thúc, bài thi không được chấm");
    }
}
