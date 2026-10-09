package com.hunre.enrollmentservice.leaderboard;

/**
 * Một dòng trên bảng xếp hạng.
 *
 * <p>Điểm = {@value #LESSON_POINTS} cho mỗi bài học hoàn thành + {@value #COURSE_POINTS} cho mỗi
 * khóa học hoàn thành. Lượt ghi danh đã hủy không tính.
 */
public record LeaderboardEntry(int rank, Long userId, String name, int points,
                               int completedLessons, int completedCourses) {

    public static final int LESSON_POINTS = 10;
    public static final int COURSE_POINTS = 100;

    public static int pointsFor(int completedLessons, int completedCourses) {
        return completedLessons * LESSON_POINTS + completedCourses * COURSE_POINTS;
    }

    LeaderboardEntry withRank(int newRank) {
        return new LeaderboardEntry(newRank, userId, name, points, completedLessons, completedCourses);
    }
}
