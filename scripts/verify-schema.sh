#!/usr/bin/env bash
#
# Khởi động từng service với MySQL thật để kiểm tra entity có khớp migration không.
#
# Vì sao cần script này. Test của các service chạy trên H2 với
# `spring.jpa.hibernate.ddl-auto=create-drop`, tức là schema được dựng TỪ entity. Entity
# và file migration có lệch nhau thế nào thì test vẫn xanh, vì chúng không bao giờ gặp
# nhau. Lỗi chỉ lộ ra khi ai đó chạy service với MySQL, thường là lúc sắp demo.
#
# Ở đây làm ngược lại: database rỗng, để Flyway dựng schema từ migration, rồi bật
# `ddl-auto=validate` cho Hibernate đối chiếu entity với schema đó. Lệch một cột là
# service không khởi động và script trả về mã lỗi.
#
# Dùng ở máy mình:
#   docker compose up -d mysql
#   ./mvnw -B -ntp -DskipTests package
#   bash scripts/verify-schema.sh
#
# Biến môi trường (đều có mặc định): DB_HOST, DB_PORT, DB_USERNAME, DB_PASSWORD, TEST_PORT

set -euo pipefail

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USERNAME="${DB_USERNAME:-elearning}"
DB_PASSWORD="${DB_PASSWORD:-elearning}"
# Mọi service đều chạy lần lượt trên cùng một cổng tạm, không đụng cổng thật của ai.
TEST_PORT="${TEST_PORT:-18099}"
# Thời gian tối đa chờ một service báo khỏe.
STARTUP_TIMEOUT="${STARTUP_TIMEOUT:-90}"

LOG_DIR="${LOG_DIR:-target/verify-schema}"
mkdir -p "$LOG_DIR"

red()   { printf '\033[31m%s\033[0m\n' "$1"; }
green() { printf '\033[32m%s\033[0m\n' "$1"; }

# Service nào khai spring-boot-flyway thì mới kiểm được: không có module đó thì Flyway
# không chạy, database rỗng sẽ không có bảng nào để validate. Service nào nối JPA sau
# này chỉ cần khai dependency là tự động được kiểm, không phải sửa script hay file CI.
discover_services() {
    for pom in */pom.xml; do
        local module
        module="$(dirname "$pom")"
        grep -q 'spring-boot-flyway' "$pom" || continue
        printf '%s\n' "$module"
    done
}

# auth-service -> auth_db, course-service -> course_db
database_of() {
    printf '%s_db' "${1%-service}"
}

jar_of() {
    local module="$1"
    # Bỏ qua file .jar.original mà spring-boot-maven-plugin để lại sau khi đóng gói.
    find "$module/target" -maxdepth 1 -name '*.jar' ! -name '*.original' 2>/dev/null | head -1
}

wait_for_health() {
    local pid="$1" waited=0
    while [ "$waited" -lt "$STARTUP_TIMEOUT" ]; do
        # Tiến trình chết rồi thì khỏi chờ hết giờ.
        if ! kill -0 "$pid" 2>/dev/null; then
            return 1
        fi
        if curl -fsS "http://127.0.0.1:${TEST_PORT}/actuator/health" > /dev/null 2>&1; then
            return 0
        fi
        sleep 2
        waited=$((waited + 2))
    done
    return 1
}

report_failure() {
    local module="$1" log="$2"
    local problems

    red "✘ $module không khởi động được với schema do Flyway dựng"
    echo

    # Cùng một lỗi được Hibernate và Spring lặp lại qua nhiều lớp exception, nên lọc lấy
    # đúng câu mô tả rồi khử trùng lặp. Không làm vậy thì một cột lệch in ra hơn chục dòng
    # gần như giống hệt nhau và người đọc phải tự tìm.
    problems="$(grep -oE 'Schema validation: [^;]+' "$log" | sed 's/[[:space:]]*$//' | sort -u || true)"

    if [ -n "$problems" ]; then
        echo "   Entity không khớp schema:"
        printf '%s\n' "$problems" | sed 's/^/     /'
        echo
        echo "   Thêm file migration mới (V<n>__*.sql) cho khớp entity."
        echo "   ĐỪNG sửa migration đã vào main: máy nào chạy nó rồi sẽ hỏng checksum."
    else
        # Hỏng vì lý do khác: Flyway checksum, thiếu cấu hình, cổng bận...
        echo "   Không phải lỗi schema. 40 dòng cuối của log:"
        tail -40 "$log" | sed 's/^/     /'
    fi

    echo
    echo "   Log đầy đủ: $log"
    echo
}

verify_service() {
    local module="$1"
    local database log jar pid
    database="$(database_of "$module")"
    log="$LOG_DIR/$module.log"
    jar="$(jar_of "$module")"

    if [ -z "$jar" ]; then
        red "✘ $module chưa có file jar, chạy ./mvnw -DskipTests package trước"
        return 1
    fi

    echo "→ $module  (database $database)"

    # Ghi đè thẳng bằng tham số dòng lệnh thay vì biến môi trường, vì mỗi service đang
    # đặt tên biến một kiểu: auth và course dùng DB_HOST/DB_NAME, quiz dùng
    # SPRING_DATASOURCE_URL. Ghi đè trực tiếp thì không phụ thuộc cách nào cả.
    java -jar "$jar" \
        --server.port="$TEST_PORT" \
        --spring.datasource.url="jdbc:mysql://${DB_HOST}:${DB_PORT}/${database}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8" \
        --spring.datasource.username="$DB_USERNAME" \
        --spring.datasource.password="$DB_PASSWORD" \
        --spring.flyway.enabled=true \
        --spring.jpa.hibernate.ddl-auto=validate \
        --spring.kafka.enabled=false \
        --spring.main.banner-mode=off \
        > "$log" 2>&1 &
    pid=$!

    local result=0
    if wait_for_health "$pid"; then
        green "✔ $module khởi động được, entity khớp schema"
    else
        report_failure "$module" "$log"
        result=1
    fi

    kill "$pid" 2>/dev/null || true
    wait "$pid" 2>/dev/null || true
    return "$result"
}

main() {
    local services status=0
    services="$(discover_services)"

    if [ -z "$services" ]; then
        echo "Không service nào khai spring-boot-flyway, không có gì để kiểm."
        return 0
    fi

    echo "Kiểm tra entity khớp migration bằng MySQL thật tại ${DB_HOST}:${DB_PORT}"
    echo

    while IFS= read -r module; do
        [ -n "$module" ] || continue
        verify_service "$module" || status=1
    done <<< "$services"

    echo
    if [ "$status" -eq 0 ]; then
        green "Tất cả service đều khớp giữa entity và migration."
    else
        red "Có service không khớp, xem chi tiết bên trên."
    fi
    return "$status"
}

main "$@"
