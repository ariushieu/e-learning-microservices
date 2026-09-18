#!/usr/bin/env bash
# Áp dụng file migration của từng service vào MySQL đang chạy trong Docker.
#
# Đây là tiện ích tạm thời để xem và thử database trước khi Flyway được bật.
# Khi cấu hình Spring Data JPA + Flyway xong, schema sẽ tự chạy lúc service khởi động
# và script này không còn cần nữa.
#
# Dùng: docker compose up -d mysql && bash infra/mysql/apply-schema.sh

set -euo pipefail

CONTAINER="${MYSQL_CONTAINER:-elearning-mysql}"
DB_USER="${MYSQL_USER:-elearning}"
DB_PASS="${MYSQL_PASSWORD:-elearning}"

# Git Bash trên Windows sẽ tự đổi đường dẫn kiểu Unix khi truyền vào docker, tắt đi
export MSYS_NO_PATHCONV=1

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

SERVICES=(
    "auth-service:auth_db"
    "course-service:course_db"
    "enrollment-service:enrollment_db"
    "quiz-service:quiz_db"
    "notification-service:notification_db"
)

mysql_run() {
    docker exec -i "$CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" \
        --default-character-set=utf8mb4 "$@" 2>&1 |
        grep -v 'password on the command line' || true
}

if ! docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
    echo "Không thấy container '$CONTAINER' đang chạy. Chạy trước: docker compose up -d mysql" >&2
    exit 1
fi

for pair in "${SERVICES[@]}"; do
    service="${pair%%:*}"
    database="${pair##*:}"

    existing=$(mysql_run -N -e \
        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$database';")
    if [ "${existing//[!0-9]/}" != "0" ]; then
        echo "[bỏ qua] $database đã có $existing bảng. Xóa sạch bằng: docker compose down -v"
        continue
    fi

    for file in "$ROOT_DIR/$service"/src/main/resources/db/migration/V*.sql; do
        echo "[áp dụng] $database <- $(basename "$file")"
        mysql_run "$database" <"$file"
    done
done

echo
echo "Số bảng theo từng database:"
mysql_run -e "
SELECT table_schema AS 'database', COUNT(*) AS 'so_bang'
  FROM information_schema.tables
 WHERE table_schema IN ('auth_db','course_db','enrollment_db','quiz_db','notification_db')
 GROUP BY table_schema;"
