package com.hunre.courseservice.repository;

import java.math.BigDecimal;

public interface InstructorStatistics {
    long getPublishedCourses();
    long getTotalStudents();
    BigDecimal getWeightedRating();
    long getRatingCount();
}
