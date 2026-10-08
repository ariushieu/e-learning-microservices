package com.hunre.courseservice.dto.response;

import java.math.BigDecimal;

public record InstructorProfileResponse(String name, long publishedCourses, long totalStudents,
                                        BigDecimal ratingAvg, long ratingCount) {
}
