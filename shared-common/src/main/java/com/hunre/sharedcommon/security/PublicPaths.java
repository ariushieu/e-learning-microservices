package com.hunre.sharedcommon.security;

import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;
import java.util.Locale;

/**
 * Danh sách đường dẫn không cần đăng nhập, có thể giới hạn theo phương thức HTTP.
 *
 * <p>Hai dạng khai báo:
 * <pre>
 * /api/auth/login          → mọi phương thức đều công khai
 * GET:/api/courses/**      → chỉ GET công khai, POST và DELETE vẫn phải đăng nhập
 * </pre>
 *
 * <p>Dạng thứ hai là thứ khiến danh mục khóa học dùng được. Khách chưa đăng nhập phải xem
 * được danh sách khóa học, nhưng nếu mở cả {@code /api/courses/**} theo đường dẫn thì
 * {@code POST /api/courses} và {@code DELETE /api/courses/1} cũng công khai luôn — ai cũng
 * xóa được khóa học của người khác.
 *
 * <p>Dùng chung cho cả servlet filter của service lẫn filter reactive của gateway, để hai
 * bên không có cơ hội hiểu khác nhau về việc đường dẫn nào là công khai.
 */
public class PublicPaths {

    private record Entry(String method, PathPattern pattern) {

        boolean matches(String requestMethod, PathContainer path) {
            if (method != null && !method.equalsIgnoreCase(requestMethod)) {
                return false;
            }
            return pattern.matches(path);
        }
    }

    private final List<Entry> entries;

    public PublicPaths(List<String> declarations) {
        PathPatternParser parser = PathPatternParser.defaultInstance;
        this.entries = declarations == null ? List.of() : declarations.stream()
                .map(String::trim)
                .filter(declaration -> !declaration.isEmpty())
                .map(declaration -> toEntry(declaration, parser))
                .toList();
    }

    private Entry toEntry(String declaration, PathPatternParser parser) {
        int separator = declaration.indexOf(':');
        // Chỉ coi là có phương thức khi phần trước dấu hai chấm không chứa '/',
        // để không cắt nhầm một đường dẫn có dấu hai chấm bên trong.
        if (separator > 0 && declaration.lastIndexOf('/', separator) < 0) {
            String method = declaration.substring(0, separator).trim().toUpperCase(Locale.ROOT);
            String path = declaration.substring(separator + 1).trim();
            return new Entry(method, parser.parse(path));
        }
        return new Entry(null, parser.parse(declaration));
    }

    /** Request này có được đi qua mà không cần token không. */
    public boolean matches(String method, PathContainer path) {
        return entries.stream().anyMatch(entry -> entry.matches(method, path));
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
