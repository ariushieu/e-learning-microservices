package com.hunre.courseservice.repository;

import com.hunre.courseservice.entity.Course;
import com.hunre.courseservice.entity.CourseReview;

/** Đọc đánh giá và khóa trong cùng truy vấn, tránh tải khóa riêng cho từng dòng. */
public record ReviewCourseRow(CourseReview review, Course course) {}
