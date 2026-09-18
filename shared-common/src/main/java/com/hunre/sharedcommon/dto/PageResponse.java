package com.hunre.sharedcommon.dto;

import java.util.List;

/**
 * Kết quả phân trang dùng chung cho mọi API danh sách.
 *
 * <p>Lớp này cố ý <b>không</b> phụ thuộc vào {@code org.springframework.data.domain.Page}.
 * shared-common là nơi chứa hợp đồng giữa các service, không phải nơi chứa chi tiết lưu
 * trữ dữ liệu; kéo Spring Data vào đây đồng nghĩa mọi service đều phải có Spring Data,
 * kể cả service không dùng database. Service nào dùng JPA thì tự chuyển từ {@code Page}
 * sang lớp này:
 *
 * <pre>{@code
 * Page<Course> page = courseRepository.findAll(pageable);
 * return PageResponse.of(
 *         page.getContent().stream().map(CourseResponse::from).toList(),
 *         page.getNumber(),
 *         page.getSize(),
 *         page.getTotalElements());
 * }</pre>
 *
 * @param content       dữ liệu của trang hiện tại
 * @param page          số thứ tự trang, bắt đầu từ 0
 * @param size          số phần tử tối đa trên một trang
 * @param totalElements tổng số phần tử của toàn bộ kết quả
 * @param totalPages    tổng số trang, được tính ra chứ không truyền vào
 * @param first         có phải trang đầu tiên không
 * @param last          có phải trang cuối cùng không
 * @param <T>           kiểu phần tử
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    /**
     * Tạo kết quả phân trang, tự tính {@code totalPages}, {@code first} và {@code last}
     * để các service không tính mỗi nơi một kiểu.
     *
     * @throws IllegalArgumentException nếu {@code page} âm, {@code size} không dương,
     *                                  hoặc {@code totalElements} âm
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        if (page < 0) {
            throw new IllegalArgumentException("page không được âm, nhận được: " + page);
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size phải lớn hơn 0, nhận được: " + size);
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements không được âm, nhận được: " + totalElements);
        }

        int totalPages = (int) ((totalElements + size - 1) / size);
        boolean first = page == 0;
        boolean last = page >= totalPages - 1;

        return new PageResponse<>(
                content == null ? List.of() : List.copyOf(content),
                page,
                size,
                totalElements,
                totalPages,
                first,
                last);
    }

    /** Trang rỗng, dùng khi không có kết quả nào. */
    public static <T> PageResponse<T> empty(int page, int size) {
        return of(List.of(), page, size, 0);
    }
}
