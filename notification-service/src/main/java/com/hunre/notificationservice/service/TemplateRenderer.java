package com.hunre.notificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Điền giá trị vào các chỗ trống dạng <code>{tenBien}</code> trong mẫu thông báo.
 *
 * <pre>{@code
 * render("Bài kiểm tra {quizTitle} đạt {score} điểm",
 *        Map.of("quizTitle", "Chương 1", "score", "85.50"))
 * → "Bài kiểm tra Chương 1 đạt 85.50 điểm"
 * }</pre>
 *
 * <p>Cố ý không dùng thư viện template nào. Mẫu ở đây chỉ có thay thế chuỗi, không vòng
 * lặp không điều kiện, nên một biểu thức chính quy là đủ; kéo cả Thymeleaf hay Freemarker
 * vào chỉ để làm việc này thì service nặng thêm mà không được gì.
 */
@Component
public class TemplateRenderer {

    private static final Logger log = LoggerFactory.getLogger(TemplateRenderer.class);

    /** Tên biến chỉ gồm chữ và số, để dấu ngoặc nhọn trong nội dung thường không bị hiểu nhầm. */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-zA-Z][a-zA-Z0-9_]*)}");

    public String render(String template, Map<String, String> variables) {
        if (template == null || template.isEmpty()) {
            return "";
        }

        Map<String, String> values = variables == null ? Map.of() : variables;
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String name = matcher.group(1);
            String value = values.get(name);

            if (value == null) {
                // Hiện nguyên chữ {fullName} cho người dùng thì khó coi, nên thay bằng rỗng.
                // Nhưng phải ghi log: thiếu biến gần như luôn là mẫu và sự kiện không khớp
                // nhau, và đó là lỗi cần sửa chứ không phải chuyện bình thường.
                log.warn("Mẫu thông báo có biến {{}} nhưng sự kiện không cung cấp giá trị", name);
                value = "";
            }

            // Giá trị có thể chứa $ hoặc \, hai ký tự mang nghĩa riêng khi thay thế.
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
