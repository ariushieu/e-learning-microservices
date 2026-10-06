package com.hunre.apigateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Đối chiếu bảng route của gateway với các controller có thật trong repo.
 *
 * <p><b>Vì sao cần test này.</b> Gateway khai đường dẫn bằng tay trong
 * {@code application.properties}, còn controller thì nằm ở module khác. Hai bên không có
 * gì ràng buộc nhau, nên thêm controller mà quên khai route là chuyện sẽ xảy ra — và đã
 * xảy ra: {@code ProgressController} phục vụ {@code /api/progress} nhưng route
 * enrollment-service chỉ khai {@code /api/enrollments/**}, nên mọi lời gọi qua gateway đều
 * nhận 404 dù token hợp lệ và service vẫn chạy bình thường.
 *
 * <p>Lỗi kiểu này không có triệu chứng ở phía backend: service khởi động tốt, gọi thẳng
 * cổng nội bộ thì đúng, chỉ frontend là gọi không được. Nên nó phải bị bắt ở CI.
 *
 * <p><b>Cách làm.</b> Đọc mã nguồn của các module anh em trên đĩa, lấy mọi chuỗi bắt đầu
 * bằng {@code /api/} trong file {@code *Controller.java}, rút về tiền tố cấp một
 * ({@code /api/progress}), rồi kiểm từng tiền tố có khớp predicate {@code Path=} nào không.
 *
 * <p>Đọc file nguồn thay vì quét annotation vì api-gateway cố ý không phụ thuộc vào module
 * nào của service — thêm dependency chỉ để test là kéo cả Spring MVC vào một ứng dụng
 * WebFlux, đúng thứ mà {@code shared-common} đã phải đánh dấu {@code optional} để tránh.
 */
@SpringBootTest
class GatewayRouteCoverageTest {

    /** Chuỗi bắt đầu bằng /api/ nằm trong dấu nháy kép. */
    private static final Pattern DUONG_DAN_API = Pattern.compile("\"(/api/[^\"]*)\"");

    /** Chú thích khối và chú thích dòng — bỏ đi để ví dụ trong Javadoc không bị tính là route. */
    private static final Pattern CHU_THICH = Pattern.compile("/\\*.*?\\*/|//[^\\n]*", Pattern.DOTALL);

    private static final PathPatternParser PARSER = PathPatternParser.defaultInstance;

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Test
    @DisplayName("mọi tiền tố /api/ có controller phục vụ đều có route ở gateway")
    void moiControllerDeuCoRouteTuongUng() throws IOException {
        Map<String, String> tienToTheoFile = quetTienToTuControllerTrongRepo();

        assertThat(tienToTheoFile)
                .as("không tìm thấy controller nào — kiểm lại cách xác định thư mục gốc repo")
                .isNotEmpty();

        List<PathPattern> mauDuongDan = docMauDuongDanCuaGateway();

        List<String> thieuRoute = tienToTheoFile.entrySet().stream()
                .filter(e -> mauDuongDan.stream().noneMatch(mau -> khop(mau, e.getKey())))
                .map(e -> "%s (khai trong %s)".formatted(e.getKey(), e.getValue()))
                .sorted()
                .toList();

        assertThat(thieuRoute)
                .as("""
                        Có controller phục vụ đường dẫn mà gateway không định tuyến tới.
                        Gọi qua gateway sẽ nhận 404 dù service vẫn chạy.
                        Thêm tiền tố vào predicate Path= của route tương ứng trong
                        api-gateway/src/main/resources/application.properties.
                        Route hiện có: %s""", mauDuongDan)
                .isEmpty();
    }

    @Test
    @DisplayName("gateway không khai route trỏ tới đường dẫn không controller nào phục vụ")
    void khongCoRouteChet() throws IOException {
        List<String> tienToCoThat = List.copyOf(quetTienToTuControllerTrongRepo().keySet());

        List<String> routeChet = docMauDuongDanCuaGateway().stream()
                .map(PathPattern::getPatternString)
                .filter(mau -> tienToCoThat.stream().noneMatch(tienTo -> khop(PARSER.parse(mau), tienTo)))
                .sorted()
                .toList();

        assertThat(routeChet)
                .as("""
                        Gateway khai route cho đường dẫn không controller nào phục vụ.
                        Người gọi nhận 404 từ service sau một lần chuyển tiếp vô ích,
                        và bảng route mô tả sai hệ thống. Bỏ những mẫu này đi.""")
                .isEmpty();
    }

    // ---------------------------------------------------------------------

    /** @return tiền tố cấp một ({@code /api/progress}) → tên file controller khai ra nó */
    private static Map<String, String> quetTienToTuControllerTrongRepo() throws IOException {
        Path goc = timThuMucGocRepo();
        Map<String, String> ketQua = new LinkedHashMap<>();

        try (Stream<Path> files = Files.walk(goc)) {
            List<Path> controllers = files
                    .filter(p -> p.getFileName().toString().endsWith("Controller.java"))
                    .filter(p -> p.toString().contains("src" + p.getFileSystem().getSeparator() + "main"))
                    .sorted()
                    .toList();

            for (Path controller : controllers) {
                String ma = CHU_THICH.matcher(Files.readString(controller)).replaceAll("");
                Matcher m = DUONG_DAN_API.matcher(ma);
                while (m.find()) {
                    ketQua.putIfAbsent(tienToCapMot(m.group(1)), controller.getFileName().toString());
                }
            }
        }
        return ketQua;
    }

    /** {@code /api/quizzes/{id}/attempts} → {@code /api/quizzes} */
    private static String tienToCapMot(String duongDan) {
        String[] phan = duongDan.split("/");
        // phan[0] rỗng vì đường dẫn mở đầu bằng "/", phan[1] là "api"
        return phan.length >= 3 ? "/api/" + phan[2] : duongDan;
    }

    private List<PathPattern> docMauDuongDanCuaGateway() {
        List<RouteDefinition> routes = Objects.requireNonNull(
                routeDefinitionLocator.getRouteDefinitions().collectList().block(),
                "không đọc được bảng route của gateway");

        List<PathPattern> mau = new ArrayList<>();
        for (RouteDefinition route : routes) {
            for (PredicateDefinition predicate : route.getPredicates()) {
                if (!"Path".equals(predicate.getName())) {
                    continue;
                }
                predicate.getArgs().values().stream()
                        // Path= nhận nhiều mẫu ngăn cách bằng dấu phẩy trên cùng một dòng
                        .flatMap(giaTri -> Stream.of(giaTri.split(",")))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(PARSER::parse)
                        .forEach(mau::add);
            }
        }
        return mau;
    }

    /** Mẫu {@code /api/x/**} phải khớp cả {@code /api/x} lẫn đường dẫn con của nó. */
    private static boolean khop(PathPattern mau, String tienTo) {
        return mau.matches(PathContainer.parsePath(tienTo))
                || mau.matches(PathContainer.parsePath(tienTo + "/1"));
    }

    /**
     * Surefire chạy với thư mục làm việc là module hiện tại, nên đi ngược lên tới thư mục
     * có {@code mvnw} — chỉ thư mục gốc của repo mới có file này.
     */
    private static Path timThuMucGocRepo() {
        Path thuMuc = Path.of("").toAbsolutePath();
        while (thuMuc != null && !Files.exists(thuMuc.resolve("mvnw"))) {
            thuMuc = thuMuc.getParent();
        }
        return Objects.requireNonNull(thuMuc, "không tìm thấy thư mục gốc repo (không có mvnw ở cấp nào)");
    }
}
