package com.hunre.quizservice.security;

import com.hunre.quizservice.entity.Quiz;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;

public final class QuizOwnership {
    private QuizOwnership() {}

    public static void requireOwner(Quiz quiz, Long currentUserId, boolean isAdmin) {
        if (currentUserId == null || (!isAdmin && !currentUserId.equals(quiz.getCreatedBy()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Bạn không có quyền quản lý bài kiểm tra của giảng viên khác");
        }
    }
}
